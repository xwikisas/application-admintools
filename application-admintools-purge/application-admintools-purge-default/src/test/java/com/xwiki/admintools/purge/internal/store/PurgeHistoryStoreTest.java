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

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.internal.store.mapper.PurgeHistoryMapper;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeHistoryStore}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeHistoryStoreTest
{
    private static final String WIKI_ID = "subwiki";

    private static final String SAVE_COMMENT = "Added an automatic purge history entry.";

    private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

    private static final PurgeHistoryEntry NEW_ENTRY =
        new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, null, PurgeStatus.SUCCESS, 10, 0, null);

    private final DocumentReference wikiReference =
        new DocumentReference("xwiki", Arrays.asList("AdminTools", "Purge", "Wikis"), WIKI_ID);

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PurgeHistoryStore store;

    @MockComponent
    private PurgePageReferences references;

    @MockComponent
    private PurgePageManager pageManager;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgeHistoryMapper mapper;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Mock
    private LocalDocumentReference historyClass;

    @Mock
    private XWikiDocument document;

    @Mock
    private Lock wikiLock;

    @Mock
    private BaseObject newObject;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.references.getHistoryPageReference(WIKI_ID)).thenReturn(this.wikiReference);
        when(this.references.getHistoryClassReference()).thenReturn(this.historyClass);
        when(this.pageManager.getDocument(this.wikiReference)).thenReturn(this.document);
        when(this.pageManager.loadDocument(this.wikiReference)).thenReturn(this.document);
        when(this.pageManager.getWikiLock(WIKI_ID)).thenReturn(this.wikiLock);
        when(this.pageManager.createObject(this.document, this.historyClass)).thenReturn(this.newObject);
        when(this.document.getDocumentReference()).thenReturn(this.wikiReference);
        when(this.wikiDescriptorManager.exists(WIKI_ID)).thenReturn(true);
        mockMaxHistoryEntries(3);
    }

    @Test
    void addEntry() throws Exception
    {
        BaseObject oldest = mockObject(DELETED_DOCUMENTS, NOW.minusSeconds(300));
        BaseObject middle = mockObject(DELETED_DOCUMENTS, NOW.minusSeconds(200));
        BaseObject newest = mockObject(DELETED_DOCUMENTS, NOW.minusSeconds(100));
        BaseObject attachments = mockObject(DELETED_ATTACHMENTS, NOW.minusSeconds(400));
        // A removed object leaves a null slot in the list of objects.
        when(this.document.getXObjects(this.historyClass)).thenReturn(
            Arrays.asList(middle, null, attachments, newest, oldest));

        this.store.addEntry(WIKI_ID, NEW_ENTRY);

        // Only the entries of the same type are no longer the newest.
        verify(this.mapper).markAsOlder(oldest);
        verify(this.mapper).markAsOlder(middle);
        verify(this.mapper).markAsOlder(newest);
        verify(this.mapper, never()).markAsOlder(attachments);
        // With the new entry, the type keeps 3 entries: the oldest one is removed.
        verify(this.document).removeXObject(oldest);
        verify(this.document, never()).removeXObject(middle);
        verify(this.document, never()).removeXObject(newest);
        verify(this.document, never()).removeXObject(attachments);
        InOrder inOrder = inOrder(this.wikiLock, this.mapper, this.pageManager);
        inOrder.verify(this.wikiLock).lock();
        inOrder.verify(this.mapper).writeEntry(this.newObject, NEW_ENTRY);
        inOrder.verify(this.pageManager).saveWithoutRevision(this.document, SAVE_COMMENT);
        inOrder.verify(this.wikiLock).unlock();
        verify(this.pageManager, never()).save(any(), anyString());
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void addEntryWithSingleHistoryEntry() throws Exception
    {
        mockMaxHistoryEntries(1);
        BaseObject older = mockObject(DELETED_DOCUMENTS, NOW.minusSeconds(100));
        when(this.document.getXObjects(this.historyClass)).thenReturn(List.of(older));

        this.store.addEntry(WIKI_ID, NEW_ENTRY);

        verify(this.document).removeXObject(older);
        verify(this.mapper).writeEntry(this.newObject, NEW_ENTRY);
        verify(this.pageManager).saveWithoutRevision(this.document, SAVE_COMMENT);
    }

    @Test
    void addFirstEntry() throws Exception
    {
        when(this.document.getXObjects(this.historyClass)).thenReturn(Collections.emptyList());

        this.store.addEntry(WIKI_ID, NEW_ENTRY);

        verify(this.document, never()).removeXObject(any());
        verify(this.mapper).writeEntry(this.newObject, NEW_ENTRY);
        verify(this.pageManager).saveWithoutRevision(this.document, SAVE_COMMENT);
    }

    @Test
    void addEntryRemovesInvalidObjects() throws Exception
    {
        BaseObject withoutType = mockObject(null, NOW.minusSeconds(100));
        BaseObject withoutDate = mockObject(DELETED_DOCUMENTS, null);
        BaseObject valid = mockObject(DELETED_DOCUMENTS, NOW.minusSeconds(100));
        when(withoutDate.getNumber()).thenReturn(1);
        when(this.document.getXObjects(this.historyClass)).thenReturn(List.of(withoutType, withoutDate, valid));

        this.store.addEntry(WIKI_ID, NEW_ENTRY);

        verify(this.document).removeXObject(withoutType);
        verify(this.document).removeXObject(withoutDate);
        verify(this.document, never()).removeXObject(valid);
        verify(this.mapper, never()).markAsOlder(withoutType);
        verify(this.mapper, never()).markAsOlder(withoutDate);
        verify(this.mapper).markAsOlder(valid);
        assertEquals(2, this.logCapture.size());
        assertEquals("Removed the invalid automatic purge history entry [0] from "
                + "[xwiki:AdminTools.Purge.Wikis.subwiki]: its type or date is missing or invalid.",
            this.logCapture.getMessage(0));
        assertEquals("Removed the invalid automatic purge history entry [1] from "
                + "[xwiki:AdminTools.Purge.Wikis.subwiki]: its type or date is missing or invalid.",
            this.logCapture.getMessage(1));
    }

    @Test
    void addEntryUnlocksWhenSaveFails() throws Exception
    {
        when(this.document.getXObjects(this.historyClass)).thenReturn(Collections.emptyList());
        XWikiException exception = new XWikiException();
        doThrow(exception).when(this.pageManager).saveWithoutRevision(this.document, SAVE_COMMENT);

        assertEquals(exception, assertThrows(XWikiException.class, () -> this.store.addEntry(WIKI_ID, NEW_ENTRY)));

        verify(this.wikiLock).unlock();
    }

    @Test
    void addEntryForDeletedWiki() throws Exception
    {
        when(this.wikiDescriptorManager.exists(WIKI_ID)).thenReturn(false);

        this.store.addEntry(WIKI_ID, NEW_ENTRY);

        // The history of a deleted wiki doesn't come back.
        InOrder inOrder = inOrder(this.wikiLock, this.wikiDescriptorManager);
        inOrder.verify(this.wikiLock).lock();
        inOrder.verify(this.wikiDescriptorManager).exists(WIKI_ID);
        inOrder.verify(this.wikiLock).unlock();
        verify(this.pageManager, never()).loadDocument(any());
        verify(this.pageManager, never()).saveWithoutRevision(any(), anyString());
    }

    @Test
    void addEntryWithoutWikiIdentifier() throws Exception
    {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.store.addEntry(" ", NEW_ENTRY));

        assertEquals("The wiki identifier is missing.", exception.getMessage());
        verify(this.pageManager, never()).getWikiLock(anyString());
        verify(this.pageManager, never()).loadDocument(any());
    }

    @Test
    void getLatestEntriesOfEveryWiki() throws Exception
    {
        when(this.references.getMainWikiId()).thenReturn("xwiki");
        when(this.references.getWikisSpaceReference()).thenReturn("AdminTools.Purge.Wikis");
        when(this.references.getHistoryClassName()).thenReturn("AdminTools.Purge.Code.HistoryClass");
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery("select doc.name, history.type, history.date, history.triggeredBy, "
            + "history.status, history.removedCount, history.failedCount from Document doc, "
            + "doc.object(AdminTools.Purge.Code.HistoryClass) as history "
            + "where doc.space = :space and history.latest = 1", Query.XWQL)).thenReturn(query);
        Date date = Date.from(NOW);
        when(query.<Object[]>execute()).thenReturn(
            List.of(new Object[] { WIKI_ID, "deletedDocuments", date, "scheduled", "success", 3L, 0L },
                new Object[] { WIKI_ID, "deletedAttachments", date, "scheduled", "failed", 0L, 2L },
                new Object[] { "other", "invalid", date, "scheduled", "success", 1L, 0L }));
        PurgeHistoryEntry documentsEntry = mockEntry(DELETED_DOCUMENTS);
        when(this.mapper.readEntry("deletedDocuments", date, "scheduled", "success", 3L, 0L)).thenReturn(
            Optional.of(documentsEntry));
        PurgeHistoryEntry attachmentsEntry = mockEntry(DELETED_ATTACHMENTS);
        when(this.mapper.readEntry("deletedAttachments", date, "scheduled", "failed", 0L, 2L)).thenReturn(
            Optional.of(attachmentsEntry));
        when(this.mapper.readEntry("invalid", date, "scheduled", "success", 1L, 0L)).thenReturn(Optional.empty());

        assertEquals(Map.of(WIKI_ID, Map.of(DELETED_DOCUMENTS, documentsEntry, DELETED_ATTACHMENTS, attachmentsEntry)),
            this.store.getLatestEntries(null));
        verify(query).setWiki("xwiki");
        verify(query).bindValues(Map.of("space", "AdminTools.Purge.Wikis"));
        // No history page is loaded.
        verify(this.pageManager, never()).getDocument(any());
    }

    @Test
    void getEntries() throws Exception
    {
        BaseObject older = mock(BaseObject.class);
        BaseObject newer = mock(BaseObject.class);
        BaseObject invalid = mock(BaseObject.class);
        PurgeHistoryEntry olderEntry =
            new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW.minusSeconds(100), null, PurgeStatus.SUCCESS, 1, 0, null);
        PurgeHistoryEntry newerEntry =
            new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, null, PurgeStatus.PARTIAL, 1, 1, List.of("Sandbox.Page"));
        when(this.mapper.readEntry(older)).thenReturn(Optional.of(olderEntry));
        when(this.mapper.readEntry(newer)).thenReturn(Optional.of(newerEntry));
        when(this.mapper.readEntry(invalid)).thenReturn(Optional.empty());
        // The objects list holds null for the removed objects.
        when(this.document.getXObjects(this.historyClass)).thenReturn(Arrays.asList(older, null, invalid, newer));

        assertEquals(List.of(newerEntry, olderEntry), this.store.getEntries(WIKI_ID));
        // The page is loaded from the database, since the cached one can miss the entries being saved.
        verify(this.pageManager, never()).getDocument(any());
    }

    @Test
    void getEntriesWithoutHistory() throws Exception
    {
        when(this.document.getXObjects(this.historyClass)).thenReturn(List.of());

        assertEquals(List.of(), this.store.getEntries(WIKI_ID));
    }

    @Test
    void getLatestEntriesOfSomeWikis() throws Exception
    {
        when(this.references.getMainWikiId()).thenReturn("xwiki");
        when(this.references.getWikisSpaceReference()).thenReturn("AdminTools.Purge.Wikis");
        when(this.references.getHistoryClassName()).thenReturn("AdminTools.Purge.Code.HistoryClass");
        Query query = mock(Query.class, RETURNS_SELF);
        // Only the history pages of the given wikis are queried.
        when(this.queryManager.createQuery("select doc.name, history.type, history.date, history.triggeredBy, "
            + "history.status, history.removedCount, history.failedCount from Document doc, "
            + "doc.object(AdminTools.Purge.Code.HistoryClass) as history "
            + "where doc.space = :space and history.latest = 1 and doc.name in (:wikis)", Query.XWQL)).thenReturn(query);
        Date date = Date.from(NOW);
        when(query.<Object[]>execute()).thenReturn(
            List.<Object[]>of(new Object[] { WIKI_ID, "deletedDocuments", date, "scheduled", "success", 3L, 0L }));
        PurgeHistoryEntry documentsEntry = mockEntry(DELETED_DOCUMENTS);
        when(this.mapper.readEntry("deletedDocuments", date, "scheduled", "success", 3L, 0L)).thenReturn(
            Optional.of(documentsEntry));

        assertEquals(Map.of(WIKI_ID, Map.of(DELETED_DOCUMENTS, documentsEntry)),
            this.store.getLatestEntries(List.of(WIKI_ID, "other")));
        verify(query).bindValues(Map.of("space", "AdminTools.Purge.Wikis", "wikis", List.of(WIKI_ID, "other")));
    }

    @Test
    void getLatestEntriesOfNoWiki() throws Exception
    {
        assertEquals(Map.of(), this.store.getLatestEntries(List.of()));

        verify(this.queryManager, never()).createQuery(anyString(), anyString());
    }

    @Test
    void getWikiIdsWithHistory() throws Exception
    {
        when(this.references.getMainWikiId()).thenReturn("xwiki");
        when(this.references.getWikisSpaceReference()).thenReturn("AdminTools.Purge.Wikis");
        when(this.references.getHistoryClassName()).thenReturn("AdminTools.Purge.Code.HistoryClass");
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery("select distinct doc.name from Document doc, "
            + "doc.object(AdminTools.Purge.Code.HistoryClass) as history where doc.space = :space", Query.XWQL))
            .thenReturn(query);
        when(query.<String>execute()).thenReturn(List.of(WIKI_ID, "deleted"));

        assertEquals(List.of(WIKI_ID, "deleted"), this.store.getWikiIdsWithHistory());
        verify(query).setWiki("xwiki");
        verify(query).bindValue("space", "AdminTools.Purge.Wikis");
        // No history page is loaded.
        verify(this.pageManager, never()).getDocument(any());
    }

    @Test
    void deleteHistory() throws Exception
    {
        this.store.deleteHistory(WIKI_ID);

        InOrder inOrder = inOrder(this.wikiLock, this.pageManager);
        inOrder.verify(this.wikiLock).lock();
        inOrder.verify(this.pageManager).delete(this.document);
        inOrder.verify(this.wikiLock).unlock();
    }

    @Test
    void deleteHistoryWithoutHistory() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);

        this.store.deleteHistory(WIKI_ID);

        // The page is always checked under the lock, so that an entry being added can't leave the history behind.
        InOrder inOrder = inOrder(this.wikiLock, this.pageManager);
        inOrder.verify(this.wikiLock).lock();
        inOrder.verify(this.pageManager).loadDocument(this.wikiReference);
        inOrder.verify(this.wikiLock).unlock();
        verify(this.pageManager, never()).delete(any());
    }

    /**
     * @param type an item type
     * @return a history entry of the given item type
     */
    private PurgeHistoryEntry mockEntry(PurgeType type)
    {
        PurgeHistoryEntry entry = mock(PurgeHistoryEntry.class);
        when(entry.getType()).thenReturn(type);
        return entry;
    }

    /**
     * Set the number of history entries kept for each item type in the global settings.
     *
     * @param maxHistoryEntries the number of history entries kept for each item type
     * @throws XWikiException never, since the settings store is mocked
     */
    private void mockMaxHistoryEntries(int maxHistoryEntries) throws XWikiException
    {
        Map<PurgeType, PurgeTypeSettings> typeSettings = new EnumMap<>(PurgeType.class);
        typeSettings.put(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS));
        when(this.settingsStore.getGlobalSettings()).thenReturn(
            new GlobalPurgeSettings(typeSettings, maxHistoryEntries));
    }

    /**
     * Create a history object whose type and date the mapper reads.
     *
     * @param type the item type of the object, or {@code null} for a missing or unknown type
     * @param date the date of the object, or {@code null} for a missing date
     * @return the history object
     */
    private BaseObject mockObject(PurgeType type, Instant date)
    {
        BaseObject object = mock(BaseObject.class);
        when(this.mapper.readType(object)).thenReturn(Optional.ofNullable(type));
        when(this.mapper.readDate(object)).thenReturn(Optional.ofNullable(date));
        return object;
    }
}
