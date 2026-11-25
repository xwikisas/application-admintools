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
package com.xwiki.admintools.purge.internal.job;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeType;

import static com.xwiki.admintools.purge.model.PurgeStatus.FAILED;
import static com.xwiki.admintools.purge.model.PurgeStatus.INTERRUPTED;
import static com.xwiki.admintools.purge.model.PurgeStatus.SUCCESS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeJob}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeJobTest
{
    private static final String WIKI_ID = "subwiki";

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-10-23T01:00:00Z");

    @InjectMockComponents
    private PurgeJob job;

    @MockComponent
    private PurgePolicyResolver policyResolver;

    @MockComponent
    private PurgeHistoryStore historyStore;

    @MockComponent
    @Named("deletedDocuments")
    private PurgeHandler documentsHandler;

    @MockComponent
    @Named("deletedAttachments")
    private PurgeHandler attachmentsHandler;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiDescriptorManager.exists(WIKI_ID)).thenReturn(true);
    }

    @Test
    void getTypeAndGroupPath()
    {
        this.job.initialize(new PurgeJobRequest(WIKI_ID, DELETED_ATTACHMENTS, null, null));

        assertEquals("admintools.purge", this.job.getType());
        assertEquals(List.of("adminTools", "purge", "deletedAttachments"), this.job.getGroupPath().getPath());
    }

    @Test
    void request()
    {
        PurgeJobRequest request = new PurgeJobRequest(WIKI_ID, DELETED_DOCUMENTS, null, null);

        assertEquals(List.of("adminTools", "purge", "deletedDocuments", WIKI_ID), request.getId());
        assertEquals(WIKI_ID, request.getWikiId());
        assertEquals(DELETED_DOCUMENTS, request.getType());
        assertTrue(request.getTriggeredBy().isEmpty());
        assertTrue(request.getExpirationDate().isEmpty());
        assertFalse(request.isStatusSerialized());
        assertFalse(request.isStatusLogIsolated());

        PurgeJobRequest manualRequest = new PurgeJobRequest(WIKI_ID, DELETED_DOCUMENTS, USER, EXPIRATION_DATE);
        assertEquals(Optional.of(USER), manualRequest.getTriggeredBy());
        assertEquals(Optional.of(EXPIRATION_DATE), manualRequest.getExpirationDate());
    }

    @Test
    void scheduledPurgeWithRemovedItemsIsRecorded() throws Exception
    {
        ArgumentCaptor<ZonedDateTime> nowCaptor = ArgumentCaptor.forClass(ZonedDateTime.class);
        when(this.policyResolver.getExpirationDate(eq(WIKI_ID), eq(DELETED_DOCUMENTS), nowCaptor.capture())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        ArgumentCaptor<Instant> dateCaptor = ArgumentCaptor.forClass(Instant.class);
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.now(), null, SUCCESS, 3, 0, null);
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), dateCaptor.capture(), eq(null))).thenReturn(
            entry);

        run(DELETED_DOCUMENTS, null, null);

        verify(this.historyStore).addEntry(WIKI_ID, entry);
        // The expiration date and the date of the entry are computed from the same moment.
        assertEquals(nowCaptor.getValue().toInstant(), dateCaptor.getValue());
    }

    @Test
    void scheduledPurgeWithNothingRemovedIsNotRecorded() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(WIKI_ID), eq(DELETED_ATTACHMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_ATTACHMENTS, Instant.now(), null, SUCCESS, 0, 0, null);
        when(this.attachmentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(null))).thenReturn(entry);

        run(DELETED_ATTACHMENTS, null, null);

        verify(this.historyStore, never()).addEntry(anyString(), any());
    }

    @Test
    void scheduledPurgeWithFailuresIsRecorded() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(WIKI_ID), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.now(), null, FAILED, 0, 1, null);
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(null))).thenReturn(entry);

        run(DELETED_DOCUMENTS, null, null);

        verify(this.historyStore).addEntry(WIKI_ID, entry);
    }

    @Test
    void scheduledPurgeInterruptedIsRecorded() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(WIKI_ID), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        // Nothing was removed before the purge was interrupted, but the interruption is still recorded.
        PurgeHistoryEntry entry =
            new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.now(), null, INTERRUPTED, 0, 0, null);
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(null))).thenReturn(entry);

        run(DELETED_DOCUMENTS, null, null);

        verify(this.historyStore).addEntry(WIKI_ID, entry);
    }

    @Test
    void manualPurgeIsAlwaysRecorded() throws Exception
    {
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.now(), USER, SUCCESS, 0, 0, null);
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(USER))).thenReturn(entry);

        run(DELETED_DOCUMENTS, USER, EXPIRATION_DATE);

        verify(this.historyStore).addEntry(WIKI_ID, entry);
    }

    @Test
    void manualPurgeAppliesTheLimitOfItsRequest() throws Exception
    {
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_ATTACHMENTS, Instant.now(), USER, SUCCESS, 2, 0, null);
        when(this.attachmentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(USER))).thenReturn(entry);

        run(DELETED_ATTACHMENTS, USER, EXPIRATION_DATE);

        // The stored settings don't matter, even if the item type is disabled.
        verify(this.policyResolver, never()).getExpirationDate(any(), any(), any());
        verify(this.historyStore).addEntry(WIKI_ID, entry);
    }

    @Test
    void deletedWikiIsSkipped() throws Exception
    {
        when(this.wikiDescriptorManager.exists(WIKI_ID)).thenReturn(false);

        run(DELETED_DOCUMENTS, USER, EXPIRATION_DATE);

        verify(this.policyResolver, never()).getExpirationDate(any(), any(), any());
        verify(this.documentsHandler, never()).purge(anyString(), any(), any(), any());
        verify(this.historyStore, never()).addEntry(anyString(), any());
    }

    @Test
    void disabledTypeIsSkipped() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(WIKI_ID), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.empty());

        run(DELETED_DOCUMENTS, null, null);

        verify(this.documentsHandler, never()).purge(anyString(), any(), any(), any());
        verify(this.historyStore, never()).addEntry(anyString(), any());
    }

    @Test
    void errorFailsTheJob() throws Exception
    {
        IllegalStateException error = new IllegalStateException("Broken");
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(USER))).thenThrow(error);

        JobException exception =
            assertThrows(JobException.class, () -> run(DELETED_DOCUMENTS, USER, EXPIRATION_DATE));

        assertEquals("Failed to purge [deletedDocuments] on wiki [subwiki].", exception.getMessage());
        assertSame(error, exception.getCause());
        // The failure is recorded in the history, without counts, with the error as its only failure.
        ArgumentCaptor<PurgeHistoryEntry> entryCaptor = ArgumentCaptor.forClass(PurgeHistoryEntry.class);
        verify(this.historyStore).addEntry(eq(WIKI_ID), entryCaptor.capture());
        PurgeHistoryEntry entry = entryCaptor.getValue();
        assertEquals(DELETED_DOCUMENTS, entry.getType());
        assertEquals(FAILED, entry.getStatus());
        assertEquals(Optional.of(USER), entry.getTriggeredBy());
        assertEquals(0, entry.getRemovedCount());
        assertEquals(0, entry.getFailedCount());
        assertEquals(List.of("Job error\tIllegalStateException: Broken"), entry.getFailures());
    }

    @Test
    void historyErrorFailsTheJob() throws Exception
    {
        PurgeHistoryEntry entry = new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.now(), USER, SUCCESS, 1, 0, null);
        when(this.documentsHandler.purge(eq(WIKI_ID), eq(EXPIRATION_DATE), any(), eq(USER))).thenReturn(entry);
        IllegalStateException saveError = new IllegalStateException("Can't save");
        doThrow(saveError).when(this.historyStore).addEntry(anyString(), any());

        JobException exception =
            assertThrows(JobException.class, () -> run(DELETED_DOCUMENTS, USER, EXPIRATION_DATE));

        // The purge can't be recorded, and neither can the failed entry: both errors are kept by the job.
        assertSame(saveError, exception.getCause());
        assertEquals(List.of(saveError), List.of(exception.getSuppressed()));
        ArgumentCaptor<PurgeHistoryEntry> entryCaptor = ArgumentCaptor.forClass(PurgeHistoryEntry.class);
        verify(this.historyStore, times(2)).addEntry(eq(WIKI_ID), entryCaptor.capture());
        assertSame(entry, entryCaptor.getAllValues().get(0));
        assertEquals(FAILED, entryCaptor.getAllValues().get(1).getStatus());
    }

    /**
     * Run the job on the test wiki.
     *
     * @param type the item type to purge
     * @param triggeredBy the user who started the purge, or {@code null} for a scheduled purge
     * @param expirationDate the expiration date of a manual purge, or {@code null} for a scheduled purge
     */
    private void run(PurgeType type, DocumentReference triggeredBy, Instant expirationDate) throws JobException
    {
        this.job.initialize(new PurgeJobRequest(WIKI_ID, type, triggeredBy, expirationDate));
        this.job.runInternal();
    }
}
