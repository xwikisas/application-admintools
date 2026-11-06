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
package com.xwiki.admintools.purge.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Provides the rows of the LiveData listing the automatic purge of each wiki. No rights are checked: the callers must
 * do it.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeWikisDataProvider.class)
@Singleton
public class PurgeWikisDataProvider
{
    private static final String WIKI_NAME = "wikiName";

    private static final String LAST_PURGE_SUFFIX = "LastPurge";

    private static final String MODE_SUFFIX = "Mode";

    private static final String STATUS_SUFFIX = "Status";

    /**
     * The status filter of the item types that were never purged.
     */
    private static final String NEVER_PURGED = "none";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgeHistoryStore historyStore;

    /**
     * Get the rows of the LiveData listing the automatic purge of each wiki, sorted by wiki name. Each row holds the
     * {@code wikiId} and {@code wikiName} of a wiki and, for each item type, the {@link PurgeTypeSettings} of the wiki
     * under the identifier of the item type, and its newest {@link PurgeHistoryEntry} under {@code <typeId>LastPurge},
     * which is missing if the item type was never purged on the wiki.
     *
     * @param filters the filters of the LiveData, empty ones being ignored: {@code wikiName}, a part of the wiki
     *     name (its pretty name, or its identifier if it has none), ignoring the case; {@code <typeId>Mode}, the
     *     identifier of the mode of the item type for the wiki; {@code <typeId>Status}, the identifier of the status of
     *     the newest purge of the item type, or {@code none} if it was never purged
     * @param order {@code desc} to sort the wikis by descending name, anything else to sort them by ascending name
     * @return the rows of the wikis matching the filters
     * @throws WikiManagerException if the wikis can't be listed
     * @throws QueryException if the purge history can't be queried
     * @throws XWikiException if the settings page can't be loaded
     */
    public List<Map<String, Object>> getWikis(Map<String, String> filters, String order)
        throws WikiManagerException, QueryException, XWikiException
    {
        String nameFilter = filters.get(WIKI_NAME);
        List<WikiDescriptor> wikis = this.wikiDescriptorManager.getAll().stream().filter(
                wiki -> StringUtils.isEmpty(nameFilter) || StringUtils.containsIgnoreCase(getName(wiki), nameFilter))
            .collect(Collectors.toList());
        Map<String, Map<PurgeType, PurgeTypeSettings>> allSettings = this.settingsStore.getSettingsOfWikis(
            wikis.stream().map(WikiDescriptor::getId).collect(Collectors.toList()));
        Map<String, Map<PurgeType, PurgeHistoryEntry>> allLatestEntries = this.historyStore.getAllLatestEntries();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (WikiDescriptor wiki : wikis) {
            Map<PurgeType, PurgeTypeSettings> settings = allSettings.get(wiki.getId());
            Map<PurgeType, PurgeHistoryEntry> latestEntries = allLatestEntries.getOrDefault(wiki.getId(), Map.of());
            if (matches(filters, settings, latestEntries)) {
                rows.add(createRow(wiki, settings, latestEntries));
            }
        }
        Comparator<Map<String, Object>> comparator =
            Comparator.comparing(row -> (String) row.get(WIKI_NAME), String.CASE_INSENSITIVE_ORDER);
        rows.sort("desc".equals(order) ? comparator.reversed() : comparator);
        return rows;
    }

    /**
     * @param filters the filters of the LiveData
     * @param settings the settings of each item type for a wiki
     * @param latestEntries the newest history entry of each item type purged on the wiki
     * @return {@code true} if the mode and the newest purge status of every item type match the filters
     */
    private boolean matches(Map<String, String> filters, Map<PurgeType, PurgeTypeSettings> settings,
        Map<PurgeType, PurgeHistoryEntry> latestEntries)
    {
        for (PurgeType type : PurgeType.values()) {
            PurgeHistoryEntry latestEntry = latestEntries.get(type);
            String status = latestEntry == null ? NEVER_PURGED : latestEntry.getStatus().getId();
            if (!matches(filters.get(type.getId() + MODE_SUFFIX), settings.get(type).getMode().getId()) || !matches(
                filters.get(type.getId() + STATUS_SUFFIX), status))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @param filter the value of a filter, or {@code null}
     * @param value the value to check
     * @return {@code true} if the filter is empty or equal to the value
     */
    private boolean matches(String filter, String value)
    {
        return StringUtils.isEmpty(filter) || filter.equals(value);
    }

    /**
     * @param wiki a wiki
     * @param settings the settings of each item type for the wiki
     * @param latestEntries the newest history entry of each item type purged on the wiki
     * @return the row of the wiki
     */
    private Map<String, Object> createRow(WikiDescriptor wiki, Map<PurgeType, PurgeTypeSettings> settings,
        Map<PurgeType, PurgeHistoryEntry> latestEntries)
    {
        Map<String, Object> row = new HashMap<>();
        row.put("wikiId", wiki.getId());
        row.put(WIKI_NAME, getName(wiki));
        for (PurgeType type : PurgeType.values()) {
            row.put(type.getId(), settings.get(type));
            if (latestEntries.containsKey(type)) {
                row.put(type.getId() + LAST_PURGE_SUFFIX, latestEntries.get(type));
            }
        }
        return row;
    }

    /**
     * @param wiki a wiki
     * @return the pretty name of the wiki, or its identifier if it has no pretty name
     */
    private String getName(WikiDescriptor wiki)
    {
        return StringUtils.defaultIfBlank(wiki.getPrettyName(), wiki.getId());
    }
}
