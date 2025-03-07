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

import javax.inject.Named;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class EmptyDocumentsProviderTest
{
    @InjectMockComponents
    EmptyDocumentsProvider emptyDocumentsProvider;

    @MockComponent
    @Named("secure")
    QueryManager queryManager;

    @Mock
    Query emptyDocsQuery;

    List<String> filterStatements =
        List.of("type:DOCUMENT", "AdminTools.DocumentContentEmpty_boolean:true", "hidden:false");

    private SolrDocumentList solrDocuments = new SolrDocumentList();

    @Mock
    private SolrDocument solrDocument1;

    @Mock
    private SolrDocument solrDocument2;

    @MockComponent
    private QueryResponse queryResponse;

    @BeforeEach
    void beforeEach() throws QueryException
    {
        when(queryManager.createQuery("*", "solr")).thenReturn(emptyDocsQuery);
        when(emptyDocsQuery.bindValue("fl",
            "title_, reference, wiki, name, spaces, AdminTools.DocumentContentEmpty_boolean, hidden")).thenReturn(
            emptyDocsQuery);
        when(emptyDocsQuery.bindValue("fq", filterStatements)).thenReturn(emptyDocsQuery);
        when(emptyDocsQuery.bindValue("sort", "wiki asc")).thenReturn(emptyDocsQuery);
        when(emptyDocsQuery.setLimit(100)).thenReturn(emptyDocsQuery);

        solrDocuments.add(solrDocument1);
        solrDocuments.add(solrDocument2);
        when(queryResponse.getResults()).thenReturn(solrDocuments);
    }

    @Test
    void getEmptyDocuments() throws QueryException
    {
        when(emptyDocsQuery.execute()).thenReturn(List.of(queryResponse));
        SolrDocumentList results = emptyDocumentsProvider.getEmptyDocuments("asc");
        assertEquals(2, results.size());
    }

    @Test
    void getEmptyDocumentsError() throws QueryException
    {
        when(emptyDocsQuery.execute()).thenThrow(new RuntimeException("Query error"));
        Exception exception = assertThrows(RuntimeException.class, () -> emptyDocumentsProvider.getEmptyDocuments(""));

        assertEquals("Query error", exception.getMessage());
    }
}
