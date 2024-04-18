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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.usage.WikiRecycleBins;

/**
 * Retrieve data about wikis recycle bins.
 *
 * @version $Id$
 */
@Component(roles = RecycleBinsManager.class)
@Singleton
public class RecycleBinsManager
{
    private static final String WIKI_NAME_KEY = "wikiName";

    private static final String DOCUMENTS_COUNT_KEY = "documentsCount";

    private static final String ATTACHMENTS_COUNT_KEY = "attachmentsCount";

    private static final String TOTAL_COUNT_KEY = "totalCount";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    /**
     * Get instance recycle bins info, like deleted documents and attachments.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a sorted and filtered {@link List} of {@link WikiRecycleBins} objects containing recycle bins info for
     *     wikis in instance.
     * @throws RuntimeException when there is an issue regarding the queries that retrieve the number of deleted
     *     documents and attachments.
     * @throws WikiManagerException for any exception while retrieving the {@link Collection} of
     *     {@link WikiDescriptor}.
     */
    public List<WikiRecycleBins> getWikisRecycleBinsSize(Map<String, String> filters, String sortColumn, String order)
        throws WikiManagerException
    {
        List<WikiRecycleBins> results = new ArrayList<>();
        Collection<WikiDescriptor> wikisDescriptors = this.wikiDescriptorManager.getAll();
        String filteredName = filters.get(WIKI_NAME_KEY);
        if (filteredName != null && !filteredName.isEmpty()) {
            wikisDescriptors.removeIf(wiki -> !wiki.getPrettyName().toLowerCase().contains(filteredName.toLowerCase()));
            filters.remove(WIKI_NAME_KEY);
        }
        wikisDescriptors.forEach(wikiDescriptor -> {
            try {
                WikiRecycleBins wikiRecycleBinResult = getWikiRecycleBinsSize(wikiDescriptor);
                if (checkFilters(filters, wikiRecycleBinResult)) {
                    results.add(wikiRecycleBinResult);
                }
            } catch (QueryException e) {
                throw new RuntimeException(e);
            }
        });
        applySort(results, sortColumn, order);
        return results;
    }

    private WikiRecycleBins getWikiRecycleBinsSize(WikiDescriptor wikiDescriptor) throws QueryException
    {
        String wikiId = wikiDescriptor.getId();
        WikiRecycleBins result = new WikiRecycleBins();
        result.setWikiName(wikiDescriptor.getPrettyName());
        result.setWikiId(wikiId);
        result.setAttachmentsCount(getNumberOfDeletedDocuments(wikiId, "DeletedAttachment"));
        result.setDocumentsCount(getNumberOfDeletedDocuments(wikiId, "XWikiDeletedDocument"));
        return result;
    }

    private long getNumberOfDeletedDocuments(String wikiID, String table) throws QueryException
    {
        String statement = String.format("select count(ddoc) from %s as ddoc", table);
        return (long) this.queryManager.createQuery(statement, Query.XWQL).setWiki(wikiID).execute().get(0);
    }

    private boolean checkFilters(Map<String, String> filters, WikiRecycleBins wikiData)
    {
        return filters.entrySet().stream().filter(filter -> filter.getValue() != null && !filter.getValue().isEmpty())
            .allMatch(filter -> {
                switch (filter.getKey()) {
                    case DOCUMENTS_COUNT_KEY:
                        return wikiData.getDocumentsCount().equals(Long.parseLong(filter.getValue()));
                    case ATTACHMENTS_COUNT_KEY:
                        return wikiData.getAttachmentsCount().equals(Long.parseLong(filter.getValue()));
                    case TOTAL_COUNT_KEY:
                        return wikiData.getTotal().equals(Long.parseLong(filter.getValue()));
                    default:
                        throw new IllegalArgumentException("Invalid filter field: " + filter.getKey());
                }
            });
    }

    private void applySort(List<WikiRecycleBins> list, String sort, String order)
    {
        Comparator<WikiRecycleBins> comparator = null;
        switch (sort) {
            case WIKI_NAME_KEY:
                comparator = Comparator.comparing(WikiRecycleBins::getWikiName);
                break;
            case DOCUMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiRecycleBins::getDocumentsCount);
                break;
            case ATTACHMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiRecycleBins::getAttachmentsCount);
                break;
            case TOTAL_COUNT_KEY:
                comparator = Comparator.comparing(WikiRecycleBins::getTotal);
                break;
            default:
                break;
        }
        if (comparator != null) {
            if ("desc".equals(order)) {
                comparator = comparator.reversed();
            }
            list.sort(comparator);
        }
    }
}
