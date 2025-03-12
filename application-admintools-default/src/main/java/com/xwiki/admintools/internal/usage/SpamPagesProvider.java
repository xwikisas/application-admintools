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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;

/**
 * Provide data for the documents that are spammed.
 *
 * @version $Id$
 */
@Component(roles = SpamPagesProvider.class)
@Singleton
public class SpamPagesProvider
{
    private static final List<String> VALID_SORT_ORDERS = List.of("desc", "asc");

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    @Inject
    private SolrUtils solrUtils;

    /**
     * Get a list of solr documents in wiki with comments above a given limit.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param filters {@link Map} of filters to be applied on the results list.
     * @param order the order of the sort.
     * @return a {@link SolrDocumentList} with the needed fields set.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public SolrDocumentList getDocumentsOverGivenNumberOfComments(long maxComments, Map<String, String> filters,
        String order) throws Exception
    {
        String searchedDocument = filters.get("docName");
        String queryStatement = "*";
        if (searchedDocument != null && !searchedDocument.isEmpty()) {
            queryStatement = String.format("title:%s", solrUtils.toCompleteFilterQueryString(searchedDocument));
        }
        List<String> filterStatements = new ArrayList<>();
        filterStatements.add("type:DOCUMENT");
        filterStatements.add(String.format("AdminTools.NumberOfComments_sortInt:[%d TO *]", maxComments));
        String searchedWiki = filters.get("wikiName");
        if (searchedWiki != null && !searchedWiki.isEmpty() && !searchedWiki.equals("-")) {
            // The XWikiServer document has a name format of "XWikiServer<wiki ID>". To select the wiki ID, we
            // have to remove the first part of the name and set it to lowercase, as wiki IDs are always in lowercase.
            String searchedWikiID = searchedWiki.replace("XWikiServer", "").toLowerCase();
            filterStatements.add(String.format("wiki:%s", solrUtils.toCompleteFilterQueryString(searchedWikiID)));
        }
        Query query = this.secureQueryManager.createQuery(queryStatement, "solr");
        if (query instanceof SecureQuery) {
            ((SecureQuery) query).checkCurrentAuthor(true);
            ((SecureQuery) query).checkCurrentUser(true);
        }

        query.bindValue("fl", "title_, reference, wiki, AdminTools.NumberOfComments_sortInt, name, spaces");
        query.bindValue("fq", filterStatements);
        query.bindValue("sort", String.format("AdminTools.NumberOfComments_sortInt %s",
            VALID_SORT_ORDERS.contains(order) ? order : VALID_SORT_ORDERS.get(0)));
        query.setLimit(100);
        return ((QueryResponse) query.execute().get(0)).getResults();
    }
}
