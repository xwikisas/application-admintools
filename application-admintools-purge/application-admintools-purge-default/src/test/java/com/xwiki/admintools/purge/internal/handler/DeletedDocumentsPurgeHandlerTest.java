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
package com.xwiki.admintools.purge.internal.handler;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeletedDocumentsPurgeHandler} and the purge loop of {@link AbstractPurgeHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class DeletedDocumentsPurgeHandlerTest
{
    private static final String WIKI_ID = "subwiki";

    private static final String CURRENT_WIKI_ID = "xwiki";

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-09-30T01:00:00Z");

    private static final Instant DATE = Instant.parse("2026-10-30T01:00:00Z");

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final String BATCH_QUERY = "select item.id from XWikiDeletedDocument as item "
        + "where item.date < :expirationDate and item.id > :lastId order by item.id";

    private static final String NAME_QUERY =
        "select item.fullName, item.language from XWikiDeletedDocument as item where item.id = :id";

    private static final String LAST_ID = "lastId";

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DeletedDocumentsPurgeHandler handler;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiRecycleBinStoreInterface recycleBinStore;

    private Query batchQuery;

    private Query nameQuery;

    @BeforeEach
    void setUp() throws Exception
    {
        this.batchQuery = mock(Query.class, RETURNS_SELF);
        this.nameQuery = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery(BATCH_QUERY, Query.HQL)).thenReturn(this.batchQuery);
        when(this.queryManager.createQuery(NAME_QUERY, Query.HQL)).thenReturn(this.nameQuery);
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWikiId()).thenReturn(CURRENT_WIKI_ID);
        when(this.wikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getRecycleBinStore()).thenReturn(this.recycleBinStore);
    }

    @Test
    void hint()
    {
        assertEquals(PurgeType.DELETED_DOCUMENTS.getId(), DeletedDocumentsPurgeHandler.HINT);
    }

    @Test
    void countExpired() throws Exception
    {
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery(
            "select count(item.id) from XWikiDeletedDocument as item where item.date < :expirationDate",
            Query.HQL)).thenReturn(query);
        when(query.execute()).thenReturn(List.of(42L));

        assertEquals(42, this.handler.countExpired(WIKI_ID, EXPIRATION_DATE));

        verify(query).setWiki(WIKI_ID);
        verify(query).bindValue("expirationDate", Date.from(EXPIRATION_DATE));
    }

    @Test
    void purgeInBatches() throws Exception
    {
        when(this.batchQuery.<Long>execute()).thenReturn(getIds(1, 100), getIds(101, 1));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, USER);

        assertEquals(
            new PurgeHistoryEntry(PurgeType.DELETED_DOCUMENTS, DATE, USER, PurgeStatus.SUCCESS, 101, 0, List.of()),
            entry);
        verify(this.batchQuery).bindValue(LAST_ID, Long.MIN_VALUE);
        verify(this.batchQuery).bindValue(LAST_ID, 100L);
        InOrder inOrder = inOrder(this.wikiContext, this.recycleBinStore);
        inOrder.verify(this.wikiContext).setWikiId(WIKI_ID);
        inOrder.verify(this.recycleBinStore).deleteFromRecycleBin(1, this.wikiContext, true);
        inOrder.verify(this.recycleBinStore).deleteFromRecycleBin(101, this.wikiContext, true);
        inOrder.verify(this.wikiContext).setWikiId(CURRENT_WIKI_ID);
        // The names are loaded only for the items that can't be deleted.
        verify(this.queryManager, never()).createQuery(NAME_QUERY, Query.HQL);
    }

    @Test
    void purgeWithFailures() throws Exception
    {
        when(this.batchQuery.<Long>execute()).thenReturn(getIds(1, 100), getIds(101, 2));
        when(this.nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { "Space.Page", "" }));
        doThrow(new XWikiException(0, 0, "Failed to delete")).when(this.recycleBinStore)
            .deleteFromRecycleBin(longThat(id -> id <= 100), same(this.wikiContext), eq(true));
        doThrow(new IllegalStateException("Broken")).when(this.recycleBinStore)
            .deleteFromRecycleBin(101, this.wikiContext, true);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, null);

        assertEquals(PurgeStatus.PARTIAL, entry.getStatus());
        assertEquals(1, entry.getRemovedCount());
        assertEquals(101, entry.getFailedCount());
        // The list of failures is capped, the number of failed items is not.
        assertEquals(100, entry.getFailures().size());
        assertEquals("Space.Page\tXWikiException: Error number 0 in 0: Failed to delete", entry.getFailures().get(0));
        verify(this.nameQuery).bindValue("id", 1L);
        // The listed failures are logged too, with the same limit.
        assertEquals(100, this.logCapture.size());
        for (int i = 0; i < 100; i++) {
            assertEquals("Failed to purge the [deletedDocuments] item [Space.Page] on wiki [subwiki].",
                this.logCapture.getMessage(i));
        }
        verify(this.recycleBinStore).deleteFromRecycleBin(102, this.wikiContext, true);
    }

    @Test
    void purgeWithOnlyFailures() throws Exception
    {
        when(this.batchQuery.<Long>execute()).thenReturn(List.of(1L));
        when(this.nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { "Space.Page", "" }));
        doThrow(new IllegalStateException("Broken")).when(this.recycleBinStore)
            .deleteFromRecycleBin(1, this.wikiContext, true);

        assertEquals(PurgeStatus.FAILED, this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, USER).getStatus());
        assertEquals("Failed to purge the [deletedDocuments] item [Space.Page] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeFailureNames() throws Exception
    {
        when(this.batchQuery.<Long>execute()).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));
        when(this.nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { "Space.Page\tA", "fr" }),
                List.<Object[]>of(new Object[] { "Space.PageB", "" }), List.of())
            .thenThrow(new QueryException("Broken", this.nameQuery, null))
            .thenThrow(new IllegalStateException("Unexpected"));
        doThrow(new IllegalStateException("Broken")).when(this.recycleBinStore)
            .deleteFromRecycleBin(anyLong(), same(this.wikiContext), eq(true));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, USER);

        // An item whose name can't be found, or loaded for any reason, is listed with its identifier.
        assertEquals(
            List.of("Space.Page A (fr)\tIllegalStateException: Broken", "Space.PageB\tIllegalStateException: Broken",
                "3\tIllegalStateException: Broken", "4\tIllegalStateException: Broken",
                "5\tIllegalStateException: Broken"), entry.getFailures());
        assertEquals(PurgeStatus.FAILED, entry.getStatus());
        // The log keeps the name as it is.
        assertEquals(List.of("Failed to purge the [deletedDocuments] item [Space.Page\tA (fr)] on wiki [subwiki].",
                "Failed to purge the [deletedDocuments] item [Space.PageB] on wiki [subwiki].",
                "Failed to purge the [deletedDocuments] item [3] on wiki [subwiki].",
                "Failed to purge the [deletedDocuments] item [4] on wiki [subwiki].",
                "Failed to purge the [deletedDocuments] item [5] on wiki [subwiki]."),
            List.of(this.logCapture.getMessage(0), this.logCapture.getMessage(1), this.logCapture.getMessage(2),
                this.logCapture.getMessage(3), this.logCapture.getMessage(4)));
    }

    @Test
    void purgeInterruptedWhenTheNextItemsCantBeLoaded() throws Exception
    {
        when(this.batchQuery.<Long>execute()).thenReturn(getIds(1, 100))
            .thenThrow(new QueryException("Timeout", this.batchQuery, null));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, USER);

        // The items removed until then are still reported.
        assertEquals(
            new PurgeHistoryEntry(PurgeType.DELETED_DOCUMENTS, DATE, USER, PurgeStatus.INTERRUPTED, 100, 0, List.of()),
            entry);
        verify(this.wikiContext).setWikiId(CURRENT_WIKI_ID);
        assertEquals(
            "Interrupted the purge of [deletedDocuments] on wiki [subwiki], since the next items can't be " + "loaded.",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeInterruptedBeforeAnyItem() throws Exception
    {
        when(this.batchQuery.execute()).thenThrow(new QueryException("Timeout", this.batchQuery, null));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, EXPIRATION_DATE, DATE, null);

        assertEquals(PurgeStatus.INTERRUPTED, entry.getStatus());
        assertEquals(0, entry.getRemovedCount());
        verify(this.recycleBinStore, never()).deleteFromRecycleBin(anyLong(), any(), eq(true));
        verify(this.wikiContext).setWikiId(CURRENT_WIKI_ID);
        assertEquals(
            "Interrupted the purge of [deletedDocuments] on wiki [subwiki], since the next items can't be " + "loaded.",
            this.logCapture.getMessage(0));
    }

    /**
     * @param firstId the identifier of the first item
     * @param count the number of items
     * @return the identifiers of the items
     */
    private List<Long> getIds(long firstId, int count)
    {
        return LongStream.range(firstId, firstId + count).boxed().collect(Collectors.toList());
    }
}
