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

import java.util.List;

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

/**
 * Retrieve data about wikis empty pages.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = EmptyDocumentsProvider.class)
@Singleton
public class EmptyDocumentsProvider extends AbstractInstanceUsageProvider
{
    private static final List<String> VALID_SORT_ORDERS = List.of("asc", "desc");

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    /**
     * Get the {@link SolrDocumentList} of empty documents in wiki.
     *
     * @param order the order of the sort.
     * @return a {@link SolrDocumentList} with the empty documents.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public SolrDocumentList getEmptyDocuments(String order) throws QueryException
    {
        List<String> filterStatements =
            List.of("type:DOCUMENT", "AdminTools.DocumentContentEmpty_boolean:true", "hidden:false");

        Query query = this.secureQueryManager.createQuery("*", "solr");
        if (query instanceof SecureQuery) {
            ((SecureQuery) query).checkCurrentAuthor(true);
            ((SecureQuery) query).checkCurrentUser(true);
        }

        query.bindValue("fl", "title_, reference, wiki, name, spaces, AdminTools.DocumentContentEmpty_boolean, hidden");
        query.bindValue("fq", filterStatements);
        query.bindValue("sort",
            String.format("wiki %s", VALID_SORT_ORDERS.contains(order) ? order : VALID_SORT_ORDERS.get(0)));
        query.setLimit(100);
        return ((QueryResponse) query.execute().get(0)).getResults();
    }
}
