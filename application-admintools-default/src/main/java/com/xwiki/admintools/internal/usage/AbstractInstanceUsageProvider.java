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
package com.xwiki.admintools.internal.usage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.api.Document;
import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Implementations for usage provider classes to simplify code.
 *
 * @version $Id$
 */
public abstract class AbstractInstanceUsageProvider
{
    private static final String WIKI_NAME_KEY = "wikiName";

    private static final String DOCUMENTS_COUNT_KEY = "documentsCount";

    private static final String ATTACHMENTS_COUNT_KEY = "attachmentsCount";

    private static final String TOTAL_COUNT_KEY = "totalCount";

    private static final String USER_COUNT_KEY = "userCount";

    private static final String ATTACHMENTS_SIZE_KEY = "attachmentsSize";

    private static final String INTERVAL_SEPARATOR = "-";

    private static final String DESCENDING_ORDER = "desc";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Check if a {@link WikiUsageResult} matches the given filters.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param wikiData entry that has the filters applied on.
     * @return {@code true} if the entry matches all given filters, or {@code false} otherwise.
     */
    public boolean checkFilters(Map<String, String> filters, WikiUsageResult wikiData)
    {
        return filters.entrySet().stream().filter(
            filter -> filter.getValue() != null && !filter.getValue().isEmpty() && !filter.getValue()
                .equals(INTERVAL_SEPARATOR)).allMatch(filter -> {
                    switch (filter.getKey()) {
                        case USER_COUNT_KEY:
                            return wikiData.getUserCount().equals(Long.parseLong(filter.getValue()));
                        case ATTACHMENTS_SIZE_KEY:
                            String[] interval = filter.getValue().split(INTERVAL_SEPARATOR);
                            long attachmentsSize = wikiData.getAttachmentsSize();
                            long lowerBound = Long.parseLong(interval[0]);
                            long upperBound = "x".equals(interval[1]) ? Long.MAX_VALUE : Long.parseLong(interval[1]);
                            return attachmentsSize > lowerBound && attachmentsSize < upperBound;
                        case ATTACHMENTS_COUNT_KEY:
                            return wikiData.getAttachmentsCount().equals(Long.parseLong(filter.getValue()));
                        case DOCUMENTS_COUNT_KEY:
                            return wikiData.getDocumentsCount().equals(Long.parseLong(filter.getValue()));
                        case TOTAL_COUNT_KEY:
                            return wikiData.getTotal().equals(Long.parseLong(filter.getValue()));
                        default:
                            throw new IllegalArgumentException("Invalid filter field: " + filter.getKey());
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
    public void applySort(List<WikiUsageResult> list, String sortColumn, String order)
    {
        Comparator<WikiUsageResult> comparator = null;
        switch (sortColumn) {
            case WIKI_NAME_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getWikiName);
                break;
            case USER_COUNT_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getUserCount);
                break;
            case ATTACHMENTS_SIZE_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getAttachmentsSize);
                break;
            case ATTACHMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getAttachmentsCount);
                break;
            case DOCUMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getDocumentsCount);
                break;
            case TOTAL_COUNT_KEY:
                comparator = Comparator.comparing(WikiUsageResult::getTotal);
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

    /**
     * Sort the given {@link List} over a given column and in a given order.
     *
     * @param list the {@link List} to be sorted.
     * @param sortColumn the column after which to be sorted.
     * @param order the sort oder.
     */
    public void applySpamSort(List<Document> list, String sortColumn, String order)
    {
        Comparator<Document> comparator = null;
        switch (sortColumn) {
            case WIKI_NAME_KEY:
                comparator = Comparator.comparing(doc -> {
                    try {
                        return wikiDescriptorManager.getById(doc.getWiki()).getPrettyName();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                break;
            case "docName":
                comparator = Comparator.comparing(Document::getTitle);
                break;
            case "numberOfComments":
                comparator = Comparator.comparing(doc -> doc.getComments().size());
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
