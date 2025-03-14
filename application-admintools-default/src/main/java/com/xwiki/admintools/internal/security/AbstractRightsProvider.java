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
package com.xwiki.admintools.internal.security;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.security.RightsResult;
import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Implementations for rights provider classes to simplify code.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractRightsProvider
{
    static final String SEPARATOR = "-";

    static final String TYPE_KEY = "type";

    static final String SPACE_KEY = "space";

    static final String DOCUMENT_KEY = "docName";

    static final String LEVEL_KEY = "level";

    static final String ENTITY_KEY = "entity";

    static final String POLICY_KEY = "policy";

    private static final String DESCENDING_ORDER = "desc";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    /**
     * Check if a {@link WikiUsageResult} matches the given filters.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param wikiData entry that has the filters applied on.
     * @return {@code true} if the entry matches all given filters, or {@code false} otherwise.
     */
    public boolean checkFilters(Map<String, String> filters, RightsResult wikiData)
    {
        return filters.entrySet().stream().filter(
            filter -> filter.getValue() != null && !filter.getValue().isEmpty() && !filter.getValue()
                .equals(SEPARATOR)).allMatch(filter -> {
                    switch (filter.getKey()) {
                        case POLICY_KEY:
                            return wikiData.getPolicy().equalsIgnoreCase(filter.getValue());
                        case LEVEL_KEY:
                            String[] levelArray = filter.getValue().split("\\|");
                            return Arrays.stream(levelArray).filter(level -> !level.equals(SEPARATOR))
                                .allMatch(wikiData.getLevel()::contains);
                        default:
                            return true;
                    }
                });
    }

    /**
     * Sort the given {@link List} over a given column and in a given order.
     *
     * @param list the {@link List} to be sorted.
     * @param sortColumn the column after which to be sorted.
     * @param order the sort oder.
     */
    public void applySort(List<RightsResult> list, String sortColumn, String order)
    {
        Comparator<RightsResult> comparator = null;
        switch (sortColumn) {
            case POLICY_KEY:
                comparator = Comparator.comparing(RightsResult::getPolicy);
                break;
            case LEVEL_KEY:
                comparator = Comparator.comparing(RightsResult::getLevel);
                break;
            case TYPE_KEY:
                comparator = Comparator.comparing(RightsResult::getType);
                break;
            case SPACE_KEY:
                comparator = Comparator.comparing(RightsResult::getSpace);
                break;
            case DOCUMENT_KEY:
                comparator = Comparator.comparing(RightsResult::getDocReference);
                break;
            case ENTITY_KEY:
                comparator = Comparator.comparing(RightsResult::getEntity);
                break;
            default:
                break;
        }
        if (comparator != null) {
            if (DESCENDING_ORDER.equals(order)) {
                comparator = comparator.reversed();
            }
            list.sort(comparator);
        }
    }
}
