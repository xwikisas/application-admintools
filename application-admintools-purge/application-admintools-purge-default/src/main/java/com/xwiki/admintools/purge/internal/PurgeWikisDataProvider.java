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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.QueryException;
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
     * Get a page of the rows of the LiveData listing the automatic purge of each wiki, sorted by wiki name. Each row
     * holds the {@code wikiId} and {@code wikiName} of a wiki and, for each item type, the {@link PurgeTypeSettings} of
     * the wiki under the identifier of the item type, and its newest {@link PurgeHistoryEntry} under
     * {@code <typeId>LastPurge}, which is missing if the item type was never purged on the wiki. Unless the wikis are
     * filtered by the status of their newest purge, the newest purges are read only for the wikis of the page, so that
     * the cost doesn't grow with the history of all the wikis.
     *
     * @param filters the filters of the LiveData, empty ones being ignored: {@code wikiName}, a part of the wiki
     *     name (its pretty name, or its identifier if it has none), ignoring the case; {@code <typeId>Mode}, the
     *     identifier of the mode of the item type for the wiki; {@code <typeId>Status}, the identifier of the status of
     *     the newest purge of the item type, or {@code none} if it was never purged
     * @param order {@code desc} to sort the wikis by descending name, anything else to sort them by ascending name
     * @param offset the index of the first row of the page, starting at 0
     * @param limit the maximum number of rows of the page
     * @return {@code total}, the number of wikis matching the filters, and {@code rows}, the rows of the page
     * @throws WikiManagerException if the wikis can't be listed
     * @throws QueryException if the purge history can't be queried
     * @throws XWikiException if the settings page can't be loaded
     */
    public Map<String, Object> getWikis(Map<String, String> filters, String order, int offset, int limit)
        throws WikiManagerException, QueryException, XWikiException
    {
        Map<String, String> wikiNames = getWikiNames(filters.get(WIKI_NAME), order);
        // The settings of all the wikis are on one page, so filtering by mode is cheap.
        Map<String, Map<PurgeType, PurgeTypeSettings>> settings =
            this.settingsStore.getSettingsOfWikis(wikiNames.keySet());
        // The newest purge of every wiki is needed only to filter them by its status.
        Map<String, Map<PurgeType, PurgeHistoryEntry>> latestEntries =
            hasStatusFilter(filters) ? this.historyStore.getLatestEntries(null) : null;
        List<String> wikiIds = applyFilters(wikiNames.keySet(), filters, settings, latestEntries);

        List<String> page = getPage(wikiIds, offset, limit);
        if (latestEntries == null) {
            // Otherwise, the newest purges are read only for the wikis of the page.
            latestEntries = this.historyStore.getLatestEntries(page);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", wikiIds.size());
        result.put("rows", createRows(page, wikiNames, settings, latestEntries));
        return result;
    }

    /**
     * @param nameFilter a part of the wiki names to look for, ignoring the case, or an empty value for every wiki
     * @param order {@code desc} to sort the wikis by descending name, anything else to sort them by ascending name
     * @return the names of the wikis matching the filter, by wiki identifier, sorted by name, ignoring the case; the
     *     name of a wiki is its pretty name, or its identifier if it has none
     * @throws WikiManagerException if the wikis can't be listed
     */
    private Map<String, String> getWikiNames(String nameFilter, String order) throws WikiManagerException
    {
        Comparator<Map.Entry<String, String>> comparator = Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER);
        return this.wikiDescriptorManager.getAll().stream()
            .map(wiki -> Map.entry(wiki.getId(), StringUtils.defaultIfBlank(wiki.getPrettyName(), wiki.getId())))
            .filter(
                wiki -> StringUtils.isEmpty(nameFilter) || StringUtils.containsIgnoreCase(wiki.getValue(), nameFilter))
            .sorted("desc".equals(order) ? comparator.reversed() : comparator).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    }

    /**
     * @param filters the filters of the LiveData
     * @return {@code true} if the wikis are filtered by the status of the newest purge of an item type
     */
    private boolean hasStatusFilter(Map<String, String> filters)
    {
        return Arrays.stream(PurgeType.values())
            .anyMatch(type -> StringUtils.isNotEmpty(filters.get(type.getId() + STATUS_SUFFIX)));
    }

    /**
     * Keep the wikis whose mode, and newest purge status when filtered by it, match the filters for every item type.
     *
     * @param wikiIds the identifiers of the wikis, in display order
     * @param filters the filters of the LiveData
     * @param settings the settings of each item type, by wiki identifier
     * @param latestEntries the newest history entry of each item type, by wiki identifier, or {@code null} if the
     *     wikis are not filtered by status; a wiki that was never purged matches the {@code none} status
     * @return the identifiers of the matching wikis, in the same order
     */
    private List<String> applyFilters(Collection<String> wikiIds, Map<String, String> filters,
        Map<String, Map<PurgeType, PurgeTypeSettings>> settings,
        Map<String, Map<PurgeType, PurgeHistoryEntry>> latestEntries)
    {
        return wikiIds.stream().filter(wikiId -> Arrays.stream(PurgeType.values()).allMatch(type -> {
            String typeId = type.getId();
            if (!matches(filters.get(typeId + MODE_SUFFIX), settings.get(wikiId).get(type).getMode().getId())) {
                return false;
            }
            if (latestEntries == null) {
                return true;
            }
            PurgeHistoryEntry latestEntry = latestEntries.getOrDefault(wikiId, Map.of()).get(type);
            return matches(filters.get(typeId + STATUS_SUFFIX),
                latestEntry == null ? NEVER_PURGED : latestEntry.getStatus().getId());
        })).collect(Collectors.toList());
    }

    /**
     * @param wikiIds the identifiers of the wikis matching the filters
     * @param offset the index of the first wiki of the page, starting at 0
     * @param limit the maximum number of wikis of the page
     * @return the identifiers of the wikis of the page, empty if the page is after the last wiki
     */
    private List<String> getPage(List<String> wikiIds, int offset, int limit)
    {
        int from = Math.min(Math.max(offset, 0), wikiIds.size());
        return wikiIds.subList(from, Math.min(from + Math.max(limit, 0), wikiIds.size()));
    }

    /**
     * @param wikiIds the identifiers of the wikis of the page
     * @param wikiNames the names of the wikis, by wiki identifier
     * @param settings the settings of each item type, by wiki identifier
     * @param latestEntries the newest history entry of each item type, by wiki identifier
     * @return the rows of the wikis, in the same order
     */
    private List<Map<String, Object>> createRows(List<String> wikiIds, Map<String, String> wikiNames,
        Map<String, Map<PurgeType, PurgeTypeSettings>> settings,
        Map<String, Map<PurgeType, PurgeHistoryEntry>> latestEntries)
    {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String wikiId : wikiIds) {
            rows.add(createRow(wikiId, wikiNames.get(wikiId), settings.get(wikiId),
                latestEntries.getOrDefault(wikiId, Map.of())));
        }
        return rows;
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
     * @param wikiId the identifier of a wiki
     * @param wikiName the name of the wiki
     * @param settings the settings of each item type for the wiki
     * @param latestEntries the newest history entry of each item type purged on the wiki
     * @return the row of the wiki
     */
    private Map<String, Object> createRow(String wikiId, String wikiName, Map<PurgeType, PurgeTypeSettings> settings,
        Map<PurgeType, PurgeHistoryEntry> latestEntries)
    {
        Map<String, Object> row = new HashMap<>();
        row.put("wikiId", wikiId);
        row.put(WIKI_NAME, wikiName);
        for (PurgeType type : PurgeType.values()) {
            row.put(type.getId(), settings.get(type));
            if (latestEntries.containsKey(type)) {
                row.put(type.getId() + LAST_PURGE_SUFFIX, latestEntries.get(type));
            }
        }
        return row;
    }
}
