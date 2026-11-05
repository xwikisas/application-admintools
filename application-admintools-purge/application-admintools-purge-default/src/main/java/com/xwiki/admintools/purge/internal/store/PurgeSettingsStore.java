/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.admintools.purge.internal.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.internal.store.mapper.PurgeSettingsMapper;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Loads and saves the global and the per-wiki automatic purge settings.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeSettingsStore.class)
@Singleton
public class PurgeSettingsStore
{
    @Inject
    private PurgePageReferences references;

    @Inject
    private PurgePageManager pageManager;

    @Inject
    private PurgeSettingsMapper mapper;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @return the global settings; every item type is disabled if they were never saved
     * @throws XWikiException if the settings page can't be loaded
     * @throws IllegalArgumentException if the stored history size is lower than 1
     */
    public GlobalPurgeSettings getGlobalSettings() throws XWikiException
    {
        return this.mapper.readSettings(getSettingsObject(getSettingsPage(), null).orElse(null));
    }

    /**
     * Save the global settings, with a new revision of the settings page.
     *
     * @param typeSettings the settings of every item type; the mode can't be {@link PurgeMode#INHERIT}
     * @param maxHistoryEntries the number of purge history entries kept for each item type of each wiki, between 1
     *     and 200
     * @throws XWikiException if the settings page can't be saved
     * @throws IllegalArgumentException if the settings are invalid
     */
    public void saveGlobalSettings(Map<PurgeType, PurgeTypeSettings> typeSettings, int maxHistoryEntries)
        throws XWikiException
    {
        Lock lock = this.pageManager.getGlobalLock();
        lock.lock();
        try {
            XWikiDocument document = this.pageManager.getDocumentCopy(this.references.getSettingsPageReference());
            this.mapper.writeSettings(getOrCreateSettingsObject(document, null), typeSettings, maxHistoryEntries);
            this.pageManager.save(document, "Updated the automatic purge defaults.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param wikiId the identifier of a wiki
     * @return the settings of the given wiki; every item type is {@link PurgeMode#INHERIT} if the wiki has no settings
     * @throws XWikiException if the settings page can't be loaded
     */
    public Map<PurgeType, PurgeTypeSettings> getWikiSettings(String wikiId) throws XWikiException
    {
        return this.mapper.readSettings(getSettingsObject(getSettingsPage(), wikiId).orElse(null), wikiId);
    }

    /**
     * Read the settings of the given wikis, from the settings page, which is loaded once.
     *
     * @param wikiIds the identifiers of the wikis
     * @return the settings of each given wiki, by wiki identifier; every item type is {@link PurgeMode#INHERIT} for a
     *     wiki without settings
     * @throws XWikiException if the settings page can't be loaded
     */
    public Map<String, Map<PurgeType, PurgeTypeSettings>> getSettingsOfWikis(Collection<String> wikiIds)
        throws XWikiException
    {
        Map<String, BaseObject> objects = new HashMap<>();
        for (BaseObject object : getSettingsObjects(getSettingsPage())) {
            this.mapper.readWiki(object).ifPresent(wikiId -> objects.putIfAbsent(wikiId, object));
        }
        Map<String, Map<PurgeType, PurgeTypeSettings>> settings = new HashMap<>();
        for (String wikiId : wikiIds) {
            settings.put(wikiId, this.mapper.readSettings(objects.get(wikiId), wikiId));
        }
        return settings;
    }

    /**
     * Save the settings of a wiki, with a new revision of the settings page. When every item type is
     * {@link PurgeMode#INHERIT}, the settings of the wiki are removed instead.
     *
     * @param wikiId the identifier of the wiki
     * @param typeSettings the settings of every item type
     * @throws XWikiException if the settings page can't be saved
     * @throws WikiManagerException if the existence of the wiki can't be checked
     * @throws IllegalArgumentException if the wiki doesn't exist, or if the settings are invalid
     */
    public void saveWikiSettings(String wikiId, Map<PurgeType, PurgeTypeSettings> typeSettings)
        throws XWikiException, WikiManagerException
    {
        if (StringUtils.isBlank(wikiId)) {
            throw new IllegalArgumentException("The wiki identifier is missing.");
        }
        if (inheritsEverything(typeSettings)) {
            removeWikiSettings(wikiId);
            return;
        }
        Lock lock = this.pageManager.getGlobalLock();
        lock.lock();
        try {
            // Check under the lock that the wiki exists, since the settings of a deleted wiki are removed under it.
            if (!this.wikiDescriptorManager.exists(wikiId)) {
                throw new IllegalArgumentException(String.format("The wiki [%s] doesn't exist.", wikiId));
            }
            XWikiDocument document = this.pageManager.getDocumentCopy(this.references.getSettingsPageReference());
            this.mapper.writeSettings(getOrCreateSettingsObject(document, wikiId), wikiId, typeSettings);
            this.pageManager.save(document,
                String.format("Updated the automatic purge settings of wiki [%s].", wikiId));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the settings of a wiki, so that it inherits the global settings for every item type, or because the wiki
     * was deleted. Nothing happens if the wiki has no settings.
     *
     * @param wikiId the identifier of the wiki
     * @throws XWikiException if the settings page can't be saved
     */
    public void removeWikiSettings(String wikiId) throws XWikiException
    {
        Lock lock = this.pageManager.getGlobalLock();
        lock.lock();
        try {
            XWikiDocument document = this.pageManager.getDocumentCopy(this.references.getSettingsPageReference());
            Optional<BaseObject> object = getSettingsObject(document, wikiId);
            if (object.isPresent()) {
                document.removeXObject(object.get());
                this.pageManager.save(document,
                    String.format("Removed the automatic purge settings of wiki [%s].", wikiId));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check whether the settings inherit the global settings for every item type. A missing item type doesn't count as
     * inheriting, so that the settings are then validated, and rejected, when they are written.
     *
     * @param typeSettings the settings of every item type for a wiki
     * @return {@code true} if every item type is {@link PurgeMode#INHERIT}
     */
    private boolean inheritsEverything(Map<PurgeType, PurgeTypeSettings> typeSettings)
    {
        return Arrays.stream(PurgeType.values()).map(typeSettings::get)
            .allMatch(settings -> settings != null && settings.getMode() == PurgeMode.INHERIT);
    }

    /**
     * @return the cached settings page, for reading only
     * @throws XWikiException if the settings page can't be loaded
     */
    private XWikiDocument getSettingsPage() throws XWikiException
    {
        return this.pageManager.getDocument(this.references.getSettingsPageReference());
    }

    /**
     * @param document the settings page
     * @return the settings objects of the page, without the removed ones
     */
    private List<BaseObject> getSettingsObjects(XWikiDocument document)
    {
        return document.getXObjects(this.references.getSettingsClassReference()).stream().filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * @param document the settings page
     * @param wikiId the identifier of a wiki, or {@code null} for the global settings
     * @return the settings object of the wiki, or the global settings object, if there is one
     */
    private Optional<BaseObject> getSettingsObject(XWikiDocument document, String wikiId)
    {
        return getSettingsObjects(document).stream()
            .filter(object -> Objects.equals(this.mapper.readWiki(object).orElse(null), wikiId)).findFirst();
    }

    /**
     * Get the settings object of a wiki, or the global settings object, creating it if the page has none yet.
     *
     * @param document the settings page
     * @param wikiId the identifier of a wiki, or {@code null} for the global settings
     * @return the settings object
     * @throws XWikiException if the object can't be created
     */
    private BaseObject getOrCreateSettingsObject(XWikiDocument document, String wikiId) throws XWikiException
    {
        Optional<BaseObject> object = getSettingsObject(document, wikiId);
        if (object.isPresent()) {
            return object.get();
        }
        return this.pageManager.createObject(document, this.references.getSettingsClassReference());
    }
}
