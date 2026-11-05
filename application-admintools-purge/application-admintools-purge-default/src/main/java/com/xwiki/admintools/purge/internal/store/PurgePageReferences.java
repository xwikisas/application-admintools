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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Knows where the automatic purge pages and classes are located. All of them are stored on the main wiki.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgePageReferences.class)
@Singleton
public class PurgePageReferences
{
    private static final String ADMIN_TOOLS = "AdminTools";

    private static final String PURGE_SPACE = "Purge";

    private static final String SPACE_SEPARATOR = ".";

    private static final List<String> CODE_SPACE = Arrays.asList(ADMIN_TOOLS, PURGE_SPACE, "Code");

    private static final LocalDocumentReference SETTINGS_CLASS =
        new LocalDocumentReference(CODE_SPACE, "SettingsClass");

    private static final List<String> WIKIS_SPACE = Arrays.asList(ADMIN_TOOLS, PURGE_SPACE, "Wikis");

    private static final String HISTORY_CLASS_NAME = "HistoryClass";

    private static final LocalDocumentReference HISTORY_CLASS =
        new LocalDocumentReference(CODE_SPACE, HISTORY_CLASS_NAME);

    private static final String SETTINGS_PAGE = "Settings";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @return the identifier of the main wiki, where all the automatic purge pages are stored
     */
    public String getMainWikiId()
    {
        return this.wikiDescriptorManager.getMainWikiId();
    }

    /**
     * @return the reference of the page holding all the settings: the global settings and those of each customized wiki
     */
    public DocumentReference getSettingsPageReference()
    {
        return new DocumentReference(getMainWikiId(), CODE_SPACE, SETTINGS_PAGE);
    }

    /**
     * @param wikiId the identifier of a wiki
     * @return the reference of the page holding the purge history of the given wiki
     */
    public DocumentReference getHistoryPageReference(String wikiId)
    {
        return new DocumentReference(getMainWikiId(), WIKIS_SPACE, wikiId);
    }

    /**
     * @return the serialized reference of the space holding the purge history page of each wiki, as used in queries
     */
    public String getWikisSpaceReference()
    {
        return String.join(SPACE_SEPARATOR, WIKIS_SPACE);
    }

    /**
     * @return the reference of the class of the global and per-wiki settings objects
     */
    public LocalDocumentReference getSettingsClassReference()
    {
        return SETTINGS_CLASS;
    }

    /**
     * @return the reference of the class of the purge history objects
     */
    public LocalDocumentReference getHistoryClassReference()
    {
        return HISTORY_CLASS;
    }

    /**
     * @return the serialized reference of the class of the purge history objects, as used in queries
     */
    public String getHistoryClassName()
    {
        return String.join(SPACE_SEPARATOR, CODE_SPACE) + SPACE_SEPARATOR + HISTORY_CLASS_NAME;
    }
}
