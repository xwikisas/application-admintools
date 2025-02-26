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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide data for the documents that are spammed.
 *
 * @version $Id$
 */
@Component(roles = SpamPagesProvider.class)
@Singleton
public class SpamPagesProvider extends AbstractInstanceUsageProvider
{
    @Inject
    Solr solr;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("currentlanguage")
    private QueryFilter currentLanguageFilter;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    @Inject
    private SolrUtils solrUtils;

    /**
     * Retrieves the documents that have more than a given number of comments.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the documents that have more than the given number of comments.
     */
    public List<DocumentReference> getDocumentsOverGivenNumberOfComments(long maxComments, Map<String, String> filters,
        String sortColumn, String order) throws WikiManagerException
    {
        Collection<WikiDescriptor> searchedWikis = getRequestedWikis(filters);
        List<DocumentReference> spammedDocuments = new ArrayList<>();
        searchedWikis.forEach(wikiDescriptor -> {
            try {
                List<DocumentReference> queryResults =
                    getCommentsForWiki(maxComments, filters.get("docName"), wikiDescriptor.getId());
                spammedDocuments.addAll(queryResults);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        applyDocumentsSort(spammedDocuments, sortColumn, order);
        return spammedDocuments;
    }

    /**
     * Get the references of documents in wiki with comments above a given limit.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param searchedDocument document hint to be searched.
     * @param wikiId the wiki for which the data will be retrieved.
     * @return a {@link List} with the {@link DocumentReference} of the documents with comments above a given limit.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public List<DocumentReference> getCommentsForWiki(long maxComments, String searchedDocument, String wikiId)
        throws Exception
    {
        String searchString;
        if (searchedDocument == null || searchedDocument.isEmpty()) {
            searchString = "%";
        } else {
            searchString = String.format("%%%s%%", searchedDocument);
        }

        SolrQuery solrQuery = new SolrQuery();

        // Search for documents where the title contains the search string and have comments
        solrQuery.setQuery("title:* AND object:XWiki.XWikiComments" + searchString); //
        // Apply filters
        solrQuery.addFilterQuery("hidden:false");
        solrQuery.addFilterQuery("docviewable:true");

        //QueryResponse queryResponse = this.getSolrClient().query(solrQuery);
        //SolrDocumentList solrDocuments = queryResponse.getResults();
        // Sort results by the number of comments in descending order
        return this.queryManager.createQuery(solrQuery.getQuery(), "solr").setWiki(wikiId).execute();
        // Execute the query

//        return this.queryManager.createQuery("select obj.name from XWikiDocument as doc, BaseObject as obj "
//                + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiComments' "
//                + "and lower(doc.title) like lower(:searchString) "
//                + "group by obj.name having count(*) > :maxComments order by count(*) desc", Query.HQL).setWiki(wikiId)
//            .bindValue("maxComments", maxComments).bindValue("searchString", searchString)
//            .addFilter(currentLanguageFilter).addFilter(hiddenDocumentFilter).addFilter(documentFilter)
//            .addFilter(viewableFilter).execute();
    }
}
