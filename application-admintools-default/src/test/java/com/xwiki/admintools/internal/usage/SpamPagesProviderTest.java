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

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class SpamPagesProviderTest
{
    @InjectMockComponents
    SpamPagesProvider spamPagesProvider;

    @MockComponent
    @Named("secure")
    QueryManager queryManager;

    @Mock
    SecureQuery commentsQuery;

    @Mock
    SecureQuery commentsQuery2;

    @Mock
    SecureQuery commentsQuery3;

    List<String> filterStatements = new ArrayList<>();

    List<String> filterStatements2 = new ArrayList<>();

    private long maxComments = 21L;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private QueryResponse queryResponse;

    @MockComponent
    private QueryResponse queryResponse2;

    private SolrDocumentList solrDocuments = new SolrDocumentList();

    private SolrDocumentList solrDocuments2 = new SolrDocumentList();

    @Mock
    private SolrDocument solrDocument1;

    @Mock
    private SolrDocument solrDocument2;

    @Mock
    private SolrDocument solrDocument3;

    @BeforeEach
    void beforeEach() throws QueryException
    {
        filterStatements.add("type:DOCUMENT");
        filterStatements.add(String.format("AdminTools.NumberOfComments_sortInt:[%d TO *]", maxComments));
        filterStatements2.addAll(filterStatements);

        when(solrUtils.toCompleteFilterQueryString("searchedDocument")).thenReturn("escapedSearchDocument");
        when(solrUtils.toCompleteFilterQueryString("searchedWiki")).thenReturn("escapedSearchedWiki");

        filterStatements2.add("wiki:escapedSearchedWiki");

        when(queryManager.createQuery("*", "solr")).thenReturn(commentsQuery);

        when(commentsQuery.bindValue("fl",
            "title_, reference, wiki, AdminTools.NumberOfComments_sortInt, links, name, spaces")).thenReturn(
            commentsQuery);
        when(commentsQuery.bindValue("fq", filterStatements)).thenReturn(commentsQuery);
        when(commentsQuery.bindValue("sort",
            String.format("AdminTools.NumberOfComments_sortInt %s", "desc"))).thenReturn(commentsQuery);
        when(commentsQuery.setLimit(100)).thenReturn(commentsQuery);

        when(queryManager.createQuery("title:escapedSearchDocument", "solr")).thenReturn(commentsQuery2);
        when(commentsQuery2.bindValue("searchString", "%")).thenReturn(commentsQuery3);
        when(commentsQuery2.bindValue("fl",
            "title_, reference, wiki, AdminTools.NumberOfComments_sortInt, links, name, spaces")).thenReturn(
            commentsQuery2);
        when(commentsQuery2.bindValue("fq", filterStatements)).thenReturn(commentsQuery2);
        when(commentsQuery2.bindValue("sort",
            String.format("AdminTools.NumberOfComments_sortInt %s", "desc"))).thenReturn(commentsQuery2);
        when(commentsQuery2.setLimit(100)).thenReturn(commentsQuery2);

        solrDocuments.add(solrDocument1);
        solrDocuments.add(solrDocument2);
        when(queryResponse.getResults()).thenReturn(solrDocuments);
        when(commentsQuery.execute()).thenReturn(List.of(queryResponse));

        solrDocuments2.add(solrDocument3);
        when(queryResponse2.getResults()).thenReturn(solrDocuments2);
        when(commentsQuery2.execute()).thenReturn(List.of(queryResponse2));
    }

    @Test
    void getDocumentsOverGivenNumberOfComments() throws Exception
    {
        assertEquals(2,
            spamPagesProvider.getDocumentsOverGivenNumberOfComments(maxComments, Map.of("docName", ""), "desc").size());
        assertEquals(1, spamPagesProvider.getDocumentsOverGivenNumberOfComments(maxComments,
            Map.of("docName", "searchedDocument", "wikiName", "searchedWiki"), "desc").size());
    }

    @Test
    void getDocumentsOverGivenNumberOfCommentsError() throws Exception
    {
        when(commentsQuery2.execute()).thenThrow(new RuntimeException("Query error"));
        Exception exception = assertThrows(RuntimeException.class,
            () -> spamPagesProvider.getDocumentsOverGivenNumberOfComments(maxComments,
                Map.of("docName", "searchedDocument"), "desc").size());

        assertEquals("Query error", exception.getMessage());
    }
}
