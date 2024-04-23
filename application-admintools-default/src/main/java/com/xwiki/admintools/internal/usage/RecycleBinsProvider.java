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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.internal.usage.wikiResult.WikiRecycleBins;
import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Retrieve data about wikis recycle bins.
 *
 * @version $Id$
 */
@Component(roles = RecycleBinsProvider.class)
@Singleton
public class RecycleBinsProvider extends AbstractInstanceUsageProvider
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    /**
     * Get instance recycle bins info, like deleted documents and attachments.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn the column after which to be sorted.
     * @param order the sort oder.
     * @return a sorted and filtered {@link List} of {@link WikiRecycleBins} objects containing recycle bins info for
     *     wikis in instance.
     */
    public List<WikiRecycleBins> getWikisRecycleBinsSize(Map<String, String> filters, String sortColumn, String order)
        throws WikiManagerException
    {
        Collection<WikiDescriptor> searchedWikis = getRequestedWikis(filters);

        List<WikiUsageResult> results = new ArrayList<>();

        searchedWikis.forEach(wikiDescriptor -> {
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
        return results.stream().filter(WikiRecycleBins.class::isInstance).map(WikiRecycleBins.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * Get wiki recycle bins info, like deleted documents and attachments.
     *
     * @param wikiDescriptor the wiki for which the data will be retrieved.
     * @return a {@link WikiRecycleBins} containing info about the recycle bins of the wiki.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public WikiRecycleBins getWikiRecycleBinsSize(WikiDescriptor wikiDescriptor) throws QueryException
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
}
