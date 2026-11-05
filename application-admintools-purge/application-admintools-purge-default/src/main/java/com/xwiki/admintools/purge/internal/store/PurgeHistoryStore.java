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

import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.internal.store.mapper.PurgeHistoryMapper;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Stores the purge history of each wiki, on the page of the wiki. Each item type keeps a limited number of entries, and
 * only its newest entry keeps the list of failures.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeHistoryStore.class)
@Singleton
public class PurgeHistoryStore
{
    /**
     * Finds the newest history entry of each item type of each wiki, flagged when it was added. Only the values that
     * describe the entry are read; the failures are not. The {@code %s} is replaced by the history class.
     */
    private static final String LATEST_ENTRIES_QUERY = "select doc.name, history.type, history.date, "
        + "history.triggeredBy, history.status, history.removedCount, history.failedCount "
        + "from Document doc, doc.object(%s) as history where doc.space = :space and history.latest = 1";

    @Inject
    private PurgePageReferences references;

    @Inject
    private QueryManager queryManager;

    @Inject
    private PurgePageManager pageManager;

    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgeHistoryMapper mapper;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    /**
     * Add an entry to the purge history of a wiki, as the newest entry of its item type. The older entries of the same
     * item type lose their failures and their newest-entry flag, and the oldest ones are removed so that the type keeps
     * at most the number of entries set in the global settings. Invalid history objects are removed too. All of this is
     * saved at once, without a new revision of the history page of the wiki. Nothing is added for a wiki that no longer
     * exists, so that its history doesn't come back after it was deleted with the wiki.
     *
     * @param wikiId the identifier of the wiki
     * @param entry the new history entry
     * @throws XWikiException if the global settings can't be loaded, or the page of the wiki can't be saved
     * @throws WikiManagerException if the existence of the wiki can't be checked
     * @throws IllegalArgumentException if the wiki identifier is missing
     */
    public void addEntry(String wikiId, PurgeHistoryEntry entry) throws XWikiException, WikiManagerException
    {
        if (StringUtils.isBlank(wikiId)) {
            throw new IllegalArgumentException("The wiki identifier is missing.");
        }
        int maxHistoryEntries = this.settingsStore.getGlobalSettings().getMaxHistoryEntries();
        Lock lock = this.pageManager.getWikiLock(wikiId);
        lock.lock();
        try {
            // Check under the lock that the wiki still exists.
            if (!this.wikiDescriptorManager.exists(wikiId)) {
                this.logger.debug("Skipped the purge history entry of wiki [{}], since the wiki no longer exists.",
                    wikiId);
                return;
            }
            XWikiDocument document = this.pageManager.getDocumentCopy(this.references.getHistoryPageReference(wikiId));
            removeInvalidObjects(document);
            List<BaseObject> olderObjects = getObjectsNewestFirst(document, entry.getType());
            olderObjects.forEach(this.mapper::markAsOlder);
            // The new entry takes one of the places.
            olderObjects.stream().skip(maxHistoryEntries - 1L).forEach(document::removeXObject);
            this.mapper.writeEntry(this.pageManager.createObject(document, this.references.getHistoryClassReference()),
                entry);
            this.pageManager.saveWithoutRevision(document, "Added an automatic purge history entry.");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Read the newest history entry of each item type of every wiki, with one query on the newest-entry flag, without
     * loading the history pages. The failures of the entries are not read.
     *
     * @return the newest history entry of each item type, by wiki identifier; wikis that were never purged are left out
     * @throws QueryException if the history can't be queried
     */
    public Map<String, Map<PurgeType, PurgeHistoryEntry>> getAllLatestEntries() throws QueryException
    {
        List<Object[]> rows =
            this.queryManager.createQuery(String.format(LATEST_ENTRIES_QUERY, this.references.getHistoryClassName()),
                    Query.XWQL).setWiki(this.references.getMainWikiId())
                .bindValue("space", this.references.getWikisSpaceReference()).execute();
        Map<String, Map<PurgeType, PurgeHistoryEntry>> allLatestEntries = new HashMap<>();
        for (Object[] row : rows) {
            // The columns selected by the query, in the same order.
            String wikiId = (String) row[0];
            String typeId = (String) row[1];
            Date date = (Date) row[2];
            String triggeredBy = (String) row[3];
            String statusId = (String) row[4];
            long removedCount = (Long) row[5];
            long failedCount = (Long) row[6];
            Optional<PurgeHistoryEntry> entry =
                this.mapper.readEntry(typeId, date, triggeredBy, statusId, removedCount, failedCount);
            if (entry.isPresent()) {
                Map<PurgeType, PurgeHistoryEntry> wikiEntries =
                    allLatestEntries.computeIfAbsent(wikiId, id -> new EnumMap<>(PurgeType.class));
                wikiEntries.put(entry.get().getType(), entry.get());
            }
        }
        return allLatestEntries;
    }

    /**
     * Delete the purge history of a wiki, once the wiki no longer exists. Nothing happens if the wiki has no history.
     *
     * @param wikiId the identifier of the deleted wiki
     * @throws XWikiException if the history page can't be loaded or deleted
     */
    public void deleteHistory(String wikiId) throws XWikiException
    {
        Lock lock = this.pageManager.getWikiLock(wikiId);
        lock.lock();
        try {
            XWikiDocument document = this.pageManager.getDocumentCopy(this.references.getHistoryPageReference(wikiId));
            if (!document.isNew()) {
                this.pageManager.delete(document);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove the history objects without a known type or a date, which can only come from manual edits. They are
     * skipped everywhere else, so they would otherwise never be trimmed.
     *
     * @param document the page of a wiki, to be saved afterwards
     */
    private void removeInvalidObjects(XWikiDocument document)
    {
        List<BaseObject> invalidObjects = document.getXObjects(this.references.getHistoryClassReference()).stream()
            .filter(object -> object != null && !isValid(object)).collect(Collectors.toList());
        for (BaseObject object : invalidObjects) {
            this.logger.warn("Removed the invalid automatic purge history entry [{}] from [{}]: its type or date is "
                + "missing or invalid.", object.getNumber(), document.getDocumentReference());
            document.removeXObject(object);
        }
    }

    /**
     * Get the history objects of an item type, newest first. Only their type and date are read. Invalid objects are
     * skipped.
     *
     * @param document the page of a wiki
     * @param type the item type
     * @return the history objects of the item type, sorted by date, newest first
     */
    private List<BaseObject> getObjectsNewestFirst(XWikiDocument document, PurgeType type)
    {
        return document.getXObjects(this.references.getHistoryClassReference()).stream()
            .filter(object -> object != null && isValid(object) && this.mapper.readType(object).orElseThrow() == type)
            .sorted(Comparator.comparing((BaseObject object) -> this.mapper.readDate(object).orElseThrow()).reversed())
            .collect(Collectors.toList());
    }

    /**
     * @param object a history object
     * @return {@code true} if the object has a known item type and a date
     */
    private boolean isValid(BaseObject object)
    {
        return this.mapper.readType(object).isPresent() && this.mapper.readDate(object).isPresent();
    }
}
