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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class EmptyDocumentsProviderTest
{
    @InjectMockComponents
    EmptyDocumentsProvider emptyDocumentsProvider;

    @MockComponent
    QueryManager queryManager;

    @Mock
    Query emptyDocsQueryWiki1;

    @Mock
    Query emptyDocsQueryWiki2;

    @Mock
    WikiDescriptor wikiDescriptor;

    @Mock
    WikiDescriptor wikiDescriptor2;

    String wikiId1 = "wikiId1";

    String wikiId2 = "wikiId2";

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWiki xWiki;

    @Mock
    private XWikiContext xContext;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocument document2;

    @Mock
    private XWikiDocument document3;

    @Mock
    private XWikiDocument document4;

    @Mock
    private DocumentReference documentRef;

    @Mock
    private DocumentReference documentRef2;

    @Mock
    private DocumentReference documentRef3;

    @Mock
    private DocumentReference documentRef4;

    @MockComponent
    @Named("hidden/document")
    private QueryFilter hiddenFilter;

    @MockComponent
    @Named("document")
    private QueryFilter documentFilter;

    @MockComponent
    @Named("viewable")
    private QueryFilter viewableFilter;

    @MockComponent
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void beforeEach() throws QueryException, WikiManagerException, XWikiException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);
        wikiDescriptors.add(wikiDescriptor2);
        when(wikiDescriptorManagerProvider.get()).thenReturn(wikiDescriptorManager);
        when(wikiDescriptorManager.getAll()).thenReturn(wikiDescriptors);

        when(wikiDescriptor.getId()).thenReturn(wikiId1);
        when(wikiDescriptor2.getId()).thenReturn(wikiId2);

        when(queryManager.createQuery(
            "select doc.fullName from XWikiDocument doc " + "where (doc.content = '' or trim(doc.content) = '') "
                + "and not exists (select obj from BaseObject obj where obj.name = doc.fullName) "
                + "and not exists (select att from XWikiAttachment att where att.docId = doc.id)",
            Query.HQL)).thenReturn(emptyDocsQueryWiki1);

        when(emptyDocsQueryWiki1.setWiki(wikiId1)).thenReturn(emptyDocsQueryWiki1);
        when(emptyDocsQueryWiki1.addFilter(hiddenFilter)).thenReturn(emptyDocsQueryWiki1);
        when(emptyDocsQueryWiki1.addFilter(documentFilter)).thenReturn(emptyDocsQueryWiki1);
        when(emptyDocsQueryWiki1.addFilter(viewableFilter)).thenReturn(emptyDocsQueryWiki1);

        when(emptyDocsQueryWiki1.setWiki(wikiId2)).thenReturn(emptyDocsQueryWiki2);
        when(emptyDocsQueryWiki2.addFilter(hiddenFilter)).thenReturn(emptyDocsQueryWiki2);
        when(emptyDocsQueryWiki2.addFilter(documentFilter)).thenReturn(emptyDocsQueryWiki2);
        when(emptyDocsQueryWiki2.addFilter(viewableFilter)).thenReturn(emptyDocsQueryWiki2);

        when(xcontextProvider.get()).thenReturn(xContext);
        when(xContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(documentRef, xContext)).thenReturn(document);
        when(xWiki.getDocument(documentRef2, xContext)).thenReturn(document2);
        when(xWiki.getDocument(documentRef3, xContext)).thenReturn(document3);
        when(xWiki.getDocument(documentRef4, xContext)).thenReturn(document4);

        when(document.isHidden()).thenReturn(false);
        when(document2.isHidden()).thenReturn(false);
        when(document3.isHidden()).thenReturn(true);
        when(document4.isHidden()).thenReturn(false);

        when(document.getXClassXML()).thenReturn("");
        when(document2.getXClassXML()).thenReturn("");
        when(document3.getXClassXML()).thenReturn("");
        when(document4.getXClassXML()).thenReturn("a class");
    }

    @Test
    void getEmptyDocumentsForWiki() throws QueryException
    {
        when(emptyDocsQueryWiki1.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(emptyDocsQueryWiki2.execute()).thenReturn(List.of(documentRef3));

        assertEquals(2, emptyDocumentsProvider.getEmptyDocumentsForWiki(wikiId1).size());
        assertEquals(1, emptyDocumentsProvider.getEmptyDocumentsForWiki(wikiId2).size());
    }

    @Test
    void getEmptyDocuments() throws QueryException, WikiManagerException
    {
        when(emptyDocsQueryWiki1.execute()).thenReturn(List.of(documentRef, documentRef2, documentRef3));
        when(emptyDocsQueryWiki2.execute()).thenReturn(List.of(documentRef4));

        List<DocumentReference> testResults = emptyDocumentsProvider.getEmptyDocuments(Map.of("docName", ""), "", "");
        assertEquals(2, testResults.size());
        assertEquals(documentRef, testResults.get(0));
        assertEquals(documentRef2, testResults.get(1));
    }

    @Test
    void getEmptyDocumentsError() throws QueryException
    {
        when(emptyDocsQueryWiki1.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(emptyDocsQueryWiki2.execute()).thenThrow(
            new QueryException("Query error", emptyDocsQueryWiki2, new Exception()));
        Exception exception = assertThrows(RuntimeException.class,
            () -> emptyDocumentsProvider.getEmptyDocuments(Map.of("docName", ""), "", ""));

        assertEquals("org.xwiki.query.QueryException: Query error. Query statement = [null]", exception.getMessage());
    }

    @Test
    void checkSort() throws QueryException, WikiManagerException, XWikiException
    {
        DocumentReference docRefA = new DocumentReference("bbb", "bbb", "ddd");
        DocumentReference docRefB = new DocumentReference("bbb", "ccc", "ccc");
        DocumentReference docRefC = new DocumentReference("aaa", "aaa", "bbb");
        DocumentReference docRefD = new DocumentReference("aaa", "aaa", "aaa");

        when(xWiki.getDocument(docRefA, xContext)).thenReturn(document);
        when(xWiki.getDocument(docRefB, xContext)).thenReturn(document2);
        when(xWiki.getDocument(docRefC, xContext)).thenReturn(document3);
        when(xWiki.getDocument(docRefD, xContext)).thenReturn(document4);

        when(document.getTitle()).thenReturn("ddd");
        when(document2.getTitle()).thenReturn("ccc");
        when(document3.getTitle()).thenReturn("bbb");
        when(document4.getTitle()).thenReturn("aaa");
        when(document4.getXClassXML()).thenReturn("");

        when(emptyDocsQueryWiki1.execute()).thenReturn(List.of(docRefA, docRefB));
        when(emptyDocsQueryWiki2.execute()).thenReturn(List.of(docRefC, docRefD));
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 pretty name");
        List<DocumentReference> testResults =
            emptyDocumentsProvider.getEmptyDocuments(Map.of("docName", ""), "docName", "asc");

        assertEquals(3, testResults.size());
        assertEquals(docRefD, testResults.get(0));
        assertEquals(docRefB, testResults.get(1));
        assertEquals(docRefA, testResults.get(2));
    }
}
