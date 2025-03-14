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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.StringClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class EntityRightsProviderTest
{
    private static final LocalDocumentReference GLOBAL_RIGHTS_CLASS =
        new LocalDocumentReference("XWiki", "XWikiGlobalRights");

    private static final LocalDocumentReference DOCUMENT_RIGHTS_CLASS =
        new LocalDocumentReference("XWiki", "XWikiRights");

    @MockComponent
    @Named("secure")
    QueryManager queryManager;

    @Mock
    SecureQuery query;

    @Mock
    SecureQuery query1;

    List<String> filterStatements = new ArrayList<>();

    List<String> filterStatements2 = new ArrayList<>();

    List<String> filterStatements3 = new ArrayList<>();

    DocumentReference docRef1 =
        new DocumentReference(new LocalDocumentReference("docSpace1", "docTitle"), new WikiReference("xwiki"));

    DocumentReference docRef2 =
        new DocumentReference(new LocalDocumentReference("docSpace2", "docTitle2"), new WikiReference("xwiki"));

    DocumentReference docRef3 =
        new DocumentReference(new LocalDocumentReference("docSpace3", "docTitle3"), new WikiReference("xwiki"));

    DocumentReference docRef4 = new DocumentReference(new LocalDocumentReference("docSpace4", "docTitle4"),
        new WikiReference("xwiki"));

    DocumentReference docRefGlobal =
        new DocumentReference(new LocalDocumentReference("XWiki", "XWikiPreferences"), new WikiReference("xwiki"));

    @Mock
    StringClass stringClass1;

    @Mock
    StringClass stringClass2;

    @Mock
    StringClass stringClass3;

    @Mock
    StringClass stringClass4;

    @Mock
    StringClass stringClass5;

    @Mock
    StringClass stringClass6;

    @Mock
    SpaceReference spaceReference1;

    @Mock
    SpaceReference spaceReference2;

    @Mock
    SpaceReference spaceReference3;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @InjectMockComponents
    private EntityRightsProvider entityRightsProvider;

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

    private String queryStatement1 = "type:DOCUMENT AND object:XWiki.XWikiRights";

    private String queryStatement2 = "type:DOCUMENT AND object:XWiki.XWikiGlobalRights";

    @MockComponent
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiDocument doc1;

    @Mock
    private XWikiDocument doc2;

    @Mock
    private XWikiDocument doc3;

    @Mock
    private XWikiDocument docGlobal;

    @Mock
    private BaseObject obj1;

    @Mock
    private BaseObject obj2;

    @Mock
    private BaseObject obj3;

    @Mock
    private BaseObject obj4;

    @Mock
    private BaseObject obj5;

    @Mock
    private BaseObject obj6;

    @BeforeEach
    void setUp() throws QueryException, XWikiException
    {
        when(solrUtils.toCompleteFilterQueryString("searchedDocument")).thenReturn("escapedSearchDocument");
        when(solrUtils.toCompleteFilterQueryString("searchedSpace")).thenReturn("escapedSearchedSpace");
        when(solrUtils.toCompleteFilterQueryString("searchedWiki")).thenReturn("escapedSearchedWiki");

        filterStatements.add("wiki:escapedSearchDocument");
        filterStatements.add("wiki:escapedSearchedSpace");
        filterStatements.add("wiki:escapedSearchedWiki");
        filterStatements3.addAll(filterStatements);
        filterStatements3.add("-name:XWikiPreferences");
        filterStatements.add("hidden:false");
        filterStatements2.add("-name:XWikiPreferences");
        when(queryManager.createQuery(queryStatement1, "solr")).thenReturn(query);

        when(query.bindValue("fl", "title_, reference, wiki, name, spaces")).thenReturn(query);
        when(query.bindValue("fq", filterStatements)).thenReturn(query);
        when(query.setLimit(200)).thenReturn(query);

        when(queryManager.createQuery(queryStatement2, "solr")).thenReturn(query1);
        when(query1.bindValue("fl", "title_, reference, wiki, name, spaces")).thenReturn(query1);
        when(query1.bindValue("fq", filterStatements3)).thenReturn(query1);
        when(query1.setLimit(200)).thenReturn(query1);

        solrDocuments.add(solrDocument1);
        solrDocuments.add(solrDocument2);
        when(queryResponse.getResults()).thenReturn(solrDocuments);
        when(query.execute()).thenReturn(List.of(queryResponse));

        solrDocuments2.add(solrDocument3);
        when(queryResponse2.getResults()).thenReturn(solrDocuments2);
        when(query1.execute()).thenReturn(List.of(queryResponse2));

        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(wiki);

        when(solrDocumentReferenceResolver.resolve(solrDocument1)).thenReturn(docRef1);
        when(solrDocumentReferenceResolver.resolve(solrDocument2)).thenReturn(docRef2);
        when(solrDocumentReferenceResolver.resolve(solrDocument3)).thenReturn(docRef3);
        when(wiki.getDocument(docRef1, context)).thenReturn(doc1);
        when(wiki.getDocument(docRef2, context)).thenReturn(doc2);
        when(wiki.getDocument(docRef3, context)).thenReturn(doc3);
        when(wiki.getDocument(docRefGlobal, context)).thenReturn(docGlobal);
        when(doc1.getXObjects(DOCUMENT_RIGHTS_CLASS)).thenReturn(List.of(obj1, obj2));
        when(doc2.getXObjects(DOCUMENT_RIGHTS_CLASS)).thenReturn(List.of(obj3, obj4));
        when(doc3.getXObjects(GLOBAL_RIGHTS_CLASS)).thenReturn(List.of(obj5, obj6));
        when(docGlobal.getXObjects(GLOBAL_RIGHTS_CLASS)).thenReturn(List.of(obj5, obj6, obj1));

        when(doc1.getDocumentReference()).thenReturn(docRef1);
        when(doc2.getDocumentReference()).thenReturn(docRef2);
        when(doc3.getDocumentReference()).thenReturn(docRef3);
        when(docGlobal.getDocumentReference()).thenReturn(docRefGlobal);

        when(obj1.get("groups")).thenReturn(stringClass1);
        when(stringClass1.toFormString()).thenReturn("stringClass1");
        when(obj2.get("groups")).thenReturn(stringClass2);
        when(stringClass2.toFormString()).thenReturn("stringClass2");
        when(obj3.get("groups")).thenReturn(stringClass3);
        when(stringClass3.toFormString()).thenReturn("stringClass3");
        when(obj4.get("groups")).thenReturn(stringClass4);
        when(stringClass4.toFormString()).thenReturn("");
        when(obj5.get("groups")).thenReturn(stringClass5);
        when(stringClass5.toFormString()).thenReturn("stringClass5");
        when(obj6.get("groups")).thenReturn(stringClass6);
        when(stringClass6.toFormString()).thenReturn("stringClass6");

        when(obj1.get("levels")).thenReturn(stringClass1);
        when(stringClass1.toFormString()).thenReturn("stringClass1");
        when(obj2.get("levels")).thenReturn(stringClass2);
        when(stringClass2.toFormString()).thenReturn("stringClass2");
        when(obj3.get("levels")).thenReturn(stringClass3);
        when(stringClass3.toFormString()).thenReturn("stringClass3");
        when(obj4.get("levels")).thenReturn(stringClass4);
        when(stringClass4.toFormString()).thenReturn("");
        when(obj5.get("levels")).thenReturn(stringClass5);
        when(stringClass5.toFormString()).thenReturn("stringClass5");
        when(obj6.get("levels")).thenReturn(stringClass6);
        when(stringClass6.toFormString()).thenReturn("stringClass6");

        when(obj1.get("allow")).thenReturn(stringClass1);
        when(stringClass1.toFormString()).thenReturn("stringClass1");
        when(obj2.get("allow")).thenReturn(stringClass2);
        when(stringClass2.toFormString()).thenReturn("stringClass2");
        when(obj3.get("allow")).thenReturn(stringClass3);
        when(stringClass3.toFormString()).thenReturn("stringClass3");
        when(obj4.get("allow")).thenReturn(stringClass4);
        when(stringClass4.toFormString()).thenReturn("");
        when(obj5.get("allow")).thenReturn(stringClass5);
        when(stringClass5.toFormString()).thenReturn("stringClass5");
        when(obj6.get("allow")).thenReturn(stringClass6);
        when(stringClass6.toFormString()).thenReturn("stringClass6");

        when(documentReferenceResolver.resolve("XWiki.XWikiPreferences")).thenReturn(docRefGlobal);
        when(documentReferenceResolver.resolve("searchedWiki:XWiki.XWikiPreferences")).thenReturn(docRef4);
        when(documentReferenceResolver.resolve("stringClass1")).thenReturn(docRef1);
        when(documentReferenceResolver.resolve("stringClass2")).thenReturn(docRef2);
        when(documentReferenceResolver.resolve("stringClass3")).thenReturn(docRef3);
        when(documentReferenceResolver.resolve("stringClass5")).thenReturn(docRefGlobal);
        when(documentReferenceResolver.resolve("stringClass6")).thenReturn(docRef4);
    }

    @Test
    void testGetEntityRights_All()
    {
        Map<String, String> filters =
            Map.of("wikiName", "searchedWiki", "space", "searchedSpace", "docName", "searchedDocument");

        assertEquals(5, entityRightsProvider.getEntityRights(filters, "type", "desc", "groups").size());
    }

    @Test
    void testGetEntityRights_Global()
    {
        Map<String, String> filters = Map.of("type", "Global");

        assertEquals(3, entityRightsProvider.getEntityRights(filters, "type", "desc", "groups").size());
    }

    @Test
    void testGetEntityRights_Error() throws QueryException
    {
        Map<String, String> filters = Map.of("type", "Space");
        when(query1.execute()).thenThrow(new RuntimeException("Query error"));
        Exception exception = assertThrows(RuntimeException.class,
            () -> entityRightsProvider.getEntityRights(filters, "type", "desc", "groups"));

        assertEquals("java.lang.RuntimeException: Query error", exception.getMessage());
    }
}
