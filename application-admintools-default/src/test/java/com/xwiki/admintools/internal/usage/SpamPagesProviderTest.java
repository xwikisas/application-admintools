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
import java.util.HashMap;
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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.internal.usage.wikiResult.WikiRecycleBins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class SpamPagesProviderTest
{
    @InjectMockComponents
    SpamPagesProvider spamPagesProvider;

    @MockComponent
    QueryManager queryManager;

    @Mock
    Query commentsQuery;

    @Mock
    Query commentsQuery2;

    @Mock
    Query commentsQuery3;

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
    private DocumentReference documentRef;

    @Mock
    private DocumentReference documentRef2;

    @Mock
    private DocumentReference documentRef3;

    @MockComponent
    @Named("currentlanguage")
    private QueryFilter currentLanguageFilter;

    @MockComponent
    @Named("hidden/document")
    private QueryFilter hiddenFilter;

    @MockComponent
    @Named("document")
    private QueryFilter documentFilter;

    @MockComponent
    @Named("viewable")
    private QueryFilter viewableFilter;

    private long maxComments = 21L;

    @BeforeEach
    void beforeEach() throws QueryException, XWikiException
    {
        when(wikiDescriptor.getId()).thenReturn(wikiId1);
        when(wikiDescriptor2.getId()).thenReturn(wikiId2);

        when(queryManager.createQuery("select obj.name from XWikiDocument as doc, BaseObject as obj "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiComments' "
            + "and lower(doc.title) like lower(:searchString) "
            + "group by obj.name having count(*) > :maxComments order by count(*) desc", Query.HQL)).thenReturn(
            commentsQuery);

        when(commentsQuery.setWiki(wikiId1)).thenReturn(commentsQuery);
        when(commentsQuery.bindValue("maxComments", maxComments)).thenReturn(commentsQuery);
        when(commentsQuery.bindValue("searchString", "%")).thenReturn(commentsQuery);
        when(commentsQuery.addFilter(currentLanguageFilter)).thenReturn(commentsQuery);
        when(commentsQuery.addFilter(hiddenFilter)).thenReturn(commentsQuery);
        when(commentsQuery.addFilter(documentFilter)).thenReturn(commentsQuery);
        when(commentsQuery.addFilter(viewableFilter)).thenReturn(commentsQuery);

        when(commentsQuery.setWiki(wikiId2)).thenReturn(commentsQuery2);
        when(commentsQuery2.bindValue("maxComments", maxComments)).thenReturn(commentsQuery2);
        when(commentsQuery2.bindValue("searchString", "%Anna%")).thenReturn(commentsQuery2);
        when(commentsQuery2.addFilter(currentLanguageFilter)).thenReturn(commentsQuery2);
        when(commentsQuery2.addFilter(hiddenFilter)).thenReturn(commentsQuery2);
        when(commentsQuery2.addFilter(documentFilter)).thenReturn(commentsQuery2);
        when(commentsQuery2.addFilter(viewableFilter)).thenReturn(commentsQuery2);

        when(commentsQuery2.bindValue("searchString", "%")).thenReturn(commentsQuery3);
        when(commentsQuery3.addFilter(currentLanguageFilter)).thenReturn(commentsQuery3);
        when(commentsQuery3.addFilter(hiddenFilter)).thenReturn(commentsQuery3);
        when(commentsQuery3.addFilter(documentFilter)).thenReturn(commentsQuery3);
        when(commentsQuery3.addFilter(viewableFilter)).thenReturn(commentsQuery3);

        when(xcontextProvider.get()).thenReturn(xContext);
        when(xContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(documentRef, xContext)).thenReturn(document);
        when(xWiki.getDocument(documentRef2, xContext)).thenReturn(document2);
        when(xWiki.getDocument(documentRef3, xContext)).thenReturn(document3);
    }

    @Test
    void getCommentsForWiki() throws QueryException
    {
        when(commentsQuery.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(commentsQuery2.execute()).thenReturn(List.of(documentRef3));

        assertEquals(2, spamPagesProvider.getCommentsForWiki(maxComments, "", wikiId1).size());
        assertEquals(1, spamPagesProvider.getCommentsForWiki(maxComments, "Anna", wikiId2).size());
    }

    @Test
    void getDocumentsOverGivenNumberOfComments() throws QueryException
    {
        when(commentsQuery.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(commentsQuery3.execute()).thenReturn(List.of());
        Collection<WikiDescriptor> wikiDescriptorCollection = List.of(wikiDescriptor, wikiDescriptor2);

        List<XWikiDocument> testResults =
            spamPagesProvider.getDocumentsOverGivenNumberOfComments(wikiDescriptorCollection, maxComments,
                Map.of("docName", ""), "", "");
        assertEquals(2, testResults.size());
        assertEquals(document, testResults.get(0));
        assertEquals(document2, testResults.get(1));
    }

    @Test
    void getDocumentsOverGivenNumberOfCommentsError() throws QueryException, XWikiException
    {
        when(commentsQuery.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(commentsQuery3.execute()).thenThrow(new QueryException("Query error", commentsQuery3, new Exception()));
        Collection<WikiDescriptor> wikiDescriptorCollection = List.of(wikiDescriptor, wikiDescriptor2);
        Exception exception = assertThrows(RuntimeException.class,
            () -> spamPagesProvider.getDocumentsOverGivenNumberOfComments(wikiDescriptorCollection, maxComments,
                Map.of("docName", ""), "", ""));

        assertEquals("org.xwiki.query.QueryException: Query error. Query statement = [null]", exception.getMessage());
        verify(xWiki).getDocument(documentRef, xContext);
        verify(xWiki).getDocument(documentRef2, xContext);
    }

    @Test
    void checkSort() throws QueryException
    {
        when(commentsQuery.execute()).thenReturn(List.of(documentRef, documentRef2));
        when(commentsQuery3.execute()).thenReturn(List.of(documentRef3));
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 pretty name");
        Collection<WikiDescriptor> wikiDescriptorCollection = List.of(wikiDescriptor, wikiDescriptor2);
        when(document.getTitle()).thenReturn("bbb");
        when(document2.getTitle()).thenReturn("ccc");
        when(document3.getTitle()).thenReturn("aaa");
        List<XWikiDocument> testResults =
            spamPagesProvider.getDocumentsOverGivenNumberOfComments(wikiDescriptorCollection, maxComments,
                Map.of("docName", ""), "docName", "asc");

        assertEquals(3, testResults.size());
        assertEquals(document3, testResults.get(0));
        assertEquals(document, testResults.get(1));
        assertEquals(document2, testResults.get(2));
    }
}
