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
package com.xwiki.admintools.purge.internal.store;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgePageManager}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgePageManagerTest
{
    private static final String COMMENT = "comment";

    @InjectMockComponents
    private PurgePageManager pageManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private DocumentAuthors authors;

    @Mock
    private UserReference currentUser;

    @BeforeEach
    void setUp()
    {
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWiki()).thenReturn(this.wiki);
        when(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.currentUser);
        when(this.document.getAuthors()).thenReturn(this.authors);
    }

    @Test
    void getDocumentReturnsCachedDocument() throws Exception
    {
        DocumentReference reference = mock(DocumentReference.class);
        when(this.wiki.getDocument(reference, this.wikiContext)).thenReturn(this.document);

        assertSame(this.document, this.pageManager.getDocument(reference));
        verify(this.document, never()).clone();
    }

    @Test
    void loadDocument() throws Exception
    {
        DocumentReference reference = new DocumentReference("subwiki", "Space", "Page");
        XWikiStoreInterface notCacheStore = mock(XWikiStoreInterface.class);
        XWikiDocument loadedDocument = mock(XWikiDocument.class);
        when(this.wikiContext.getWikiId()).thenReturn("xwiki");
        when(this.wiki.getNotCacheStore()).thenReturn(notCacheStore);
        when(notCacheStore.loadXWikiDoc(argThat(document -> reference.equals(document.getDocumentReference())),
            same(this.wikiContext))).thenReturn(loadedDocument);

        assertSame(loadedDocument, this.pageManager.loadDocument(reference));
        // The page is loaded from the database of its wiki, then the current wiki is restored.
        InOrder order = inOrder(this.wikiContext, notCacheStore);
        order.verify(this.wikiContext).setWikiId("subwiki");
        order.verify(notCacheStore).loadXWikiDoc(any(), same(this.wikiContext));
        order.verify(this.wikiContext).setWikiId("xwiki");
        verify(this.wiki, never()).getDocument(any(DocumentReference.class), any(XWikiContext.class));
    }

    @Test
    void createObject() throws Exception
    {
        EntityReference classReference = mock(EntityReference.class);
        BaseObject object = mock(BaseObject.class);
        when(this.document.newXObject(classReference, this.wikiContext)).thenReturn(object);

        assertSame(object, this.pageManager.createObject(this.document, classReference));
    }

    @Test
    void saveNewDocument() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);

        this.pageManager.save(this.document, COMMENT);

        verify(this.authors).setEffectiveMetadataAuthor(this.currentUser);
        verify(this.authors).setOriginalMetadataAuthor(this.currentUser);
        verify(this.authors).setContentAuthor(this.currentUser);
        verify(this.authors).setCreator(this.currentUser);
        verify(this.document).setHidden(true);
        verify(this.wiki).saveDocument(this.document, COMMENT, false, this.wikiContext);
    }

    @Test
    void saveExistingDocument() throws Exception
    {
        this.pageManager.save(this.document, COMMENT);

        verify(this.authors).setEffectiveMetadataAuthor(this.currentUser);
        verify(this.authors).setOriginalMetadataAuthor(this.currentUser);
        verify(this.authors, never()).setCreator(this.currentUser);
        verify(this.document, never()).setHidden(anyBoolean());
        verify(this.wiki).saveDocument(this.document, COMMENT, false, this.wikiContext);
    }

    @Test
    void saveWithoutRevisionExistingDocument() throws Exception
    {
        this.pageManager.saveWithoutRevision(this.document, COMMENT);

        InOrder inOrder = inOrder(this.document, this.wiki);
        inOrder.verify(this.document).setContentDirty(false);
        inOrder.verify(this.document).setMetaDataDirty(false);
        inOrder.verify(this.wiki).saveDocument(this.document, COMMENT, true, this.wikiContext);
        verify(this.authors, never()).setEffectiveMetadataAuthor(this.currentUser);
    }

    @Test
    void saveWithoutRevisionNewDocument() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);

        this.pageManager.saveWithoutRevision(this.document, COMMENT);

        verify(this.document, never()).setMetaDataDirty(false);
        verify(this.document).setHidden(true);
        verify(this.wiki).saveDocument(this.document, COMMENT, false, this.wikiContext);
    }

    @Test
    void delete() throws Exception
    {
        this.pageManager.delete(this.document);

        verify(this.wiki).deleteDocument(this.document, false, this.wikiContext);
    }

    @Test
    void getLocks()
    {
        assertSame(this.pageManager.getWikiLock("subwiki"), this.pageManager.getWikiLock("subwiki"));
        assertNotSame(this.pageManager.getWikiLock("subwiki"), this.pageManager.getWikiLock("other"));
        assertNotSame(this.pageManager.getGlobalLock(), this.pageManager.getWikiLock("subwiki"));
    }
}
