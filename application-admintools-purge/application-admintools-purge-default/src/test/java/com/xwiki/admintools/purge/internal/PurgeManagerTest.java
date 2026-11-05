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
package com.xwiki.admintools.purge.internal;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.licensing.Licensor;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeManager}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeManagerTest
{
    private static final String MAIN_WIKI = "xwiki";

    private static final String SUBWIKI = "subwiki";

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-10-23T01:00:00Z");

    private static final DocumentReference USER = new DocumentReference(MAIN_WIKI, "XWiki", "Admin");

    private static final DocumentReference LICENSED_PAGE =
        new DocumentReference(MAIN_WIKI, List.of("AdminTools", "Code"), "ConfigurationClass");

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PurgeManager purgeManager;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @Mock
    private Licensor licensor;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private PurgePolicyResolver policyResolver;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgeHistoryStore historyStore;

    @MockComponent
    @Named("deletedDocuments")
    private PurgeHandler documentsHandler;

    @MockComponent
    @Named("deletedAttachments")
    private PurgeHandler attachmentsHandler;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private ContextStoreManager contextStoreManager;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.licensorProvider.get()).thenReturn(this.licensor);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(MAIN_WIKI);
        when(this.licensor.hasLicensure(LICENSED_PAGE)).thenReturn(true);
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of(MAIN_WIKI, SUBWIKI));
        when(this.policyResolver.getExpirationDate(anyString(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void dispatchDuePurgesWithoutLicense() throws Exception
    {
        when(this.licensor.hasLicensure(LICENSED_PAGE)).thenReturn(false);
        assertNoLicense();

        when(this.licensorProvider.get()).thenReturn(null);
        assertNoLicense();

        verify(this.wikiDescriptorManager, never()).getAllIds();
        verify(this.jobExecutor, never()).execute(anyString(), any());
        verifyNoInteractions(this.settingsStore, this.historyStore);
    }

    @Test
    void dispatchDuePurgesRemovesDataOfDeletedWikis() throws Exception
    {
        when(this.settingsStore.getWikiIdsWithSettings()).thenReturn(Set.of(SUBWIKI, "deletedWithSettings"));
        when(this.historyStore.getWikiIdsWithHistory()).thenReturn(List.of(MAIN_WIKI, "deletedWithHistory"));

        this.purgeManager.dispatchDuePurges();

        // Only the data of the wikis that no longer exist is removed, before the purges are dispatched.
        InOrder inOrder = inOrder(this.settingsStore, this.historyStore, this.policyResolver);
        inOrder.verify(this.settingsStore).removeWikiSettings("deletedWithSettings");
        inOrder.verify(this.historyStore).deleteHistory("deletedWithHistory");
        inOrder.verify(this.policyResolver).getExpirationDate(eq(MAIN_WIKI), eq(DELETED_DOCUMENTS), any());
        verify(this.settingsStore, never()).removeWikiSettings(SUBWIKI);
        verify(this.historyStore, never()).deleteHistory(MAIN_WIKI);
    }

    @Test
    void dispatchDuePurgesContinuesAfterACleanupError() throws Exception
    {
        when(this.settingsStore.getWikiIdsWithSettings()).thenThrow(new XWikiException());
        when(this.policyResolver.getExpirationDate(eq(MAIN_WIKI), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(MAIN_WIKI, EXPIRATION_DATE)).thenReturn(5L);

        this.purgeManager.dispatchDuePurges();

        verify(this.jobExecutor).execute(eq("admintools.purge"), any());
        assertEquals("Failed to remove the automatic purge data of the deleted wikis.", this.logCapture.getMessage(0));
    }

    @Test
    void dispatchDuePurgesStartsOnlyTheDueJobs() throws Exception
    {
        Map<String, Serializable> context = Map.of("user", "XWiki.superadmin");
        when(this.contextStoreManager.save(List.of("user", "wiki"))).thenReturn(context);
        // Pages: due on the main wiki, nothing expired on the subwiki. Attachments: disabled on the main wiki, due on
        // the subwiki.
        when(this.policyResolver.getExpirationDate(anyString(), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(MAIN_WIKI, EXPIRATION_DATE)).thenReturn(5L);
        when(this.policyResolver.getExpirationDate(eq(SUBWIKI), eq(DELETED_ATTACHMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.attachmentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(2L);

        this.purgeManager.dispatchDuePurges();

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor, times(2)).execute(eq("admintools.purge"), requestCaptor.capture());
        PurgeJobRequest documentsRequest = (PurgeJobRequest) requestCaptor.getAllValues().get(0);
        assertEquals(List.of("adminTools", "purge", "deletedDocuments", MAIN_WIKI), documentsRequest.getId());
        assertEquals(context, documentsRequest.getContext());
        assertEquals(List.of("adminTools", "purge", "deletedAttachments", SUBWIKI),
            requestCaptor.getAllValues().get(1).getId());
        verify(this.attachmentsHandler, never()).countExpired(eq(MAIN_WIKI), any());
    }

    @Test
    void dispatchDuePurgesSkipsUnfinishedJobs() throws Exception
    {
        Job runningJob = mockJob(JobStatus.State.RUNNING);
        when(this.jobExecutor.getJob(PurgeJobRequest.getJobId(MAIN_WIKI, DELETED_DOCUMENTS))).thenReturn(runningJob);
        Job finishedJob = mockJob(JobStatus.State.FINISHED);
        when(this.jobExecutor.getJob(PurgeJobRequest.getJobId(MAIN_WIKI, DELETED_ATTACHMENTS))).thenReturn(finishedJob);
        when(this.policyResolver.getExpirationDate(eq(MAIN_WIKI), any(), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(MAIN_WIKI, EXPIRATION_DATE)).thenReturn(5L);
        when(this.attachmentsHandler.countExpired(MAIN_WIKI, EXPIRATION_DATE)).thenReturn(2L);

        this.purgeManager.dispatchDuePurges();

        // Only the job whose previous run is finished is started again.
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor).execute(eq("admintools.purge"), requestCaptor.capture());
        assertEquals(List.of("adminTools", "purge", "deletedAttachments", MAIN_WIKI), requestCaptor.getValue().getId());
    }

    @Test
    void dispatchDuePurgesContinuesAfterAnError() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(MAIN_WIKI), eq(DELETED_DOCUMENTS), any())).thenThrow(
            new XWikiException());
        when(this.policyResolver.getExpirationDate(eq(MAIN_WIKI), eq(DELETED_ATTACHMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.attachmentsHandler.countExpired(MAIN_WIKI, EXPIRATION_DATE)).thenReturn(2L);

        this.purgeManager.dispatchDuePurges();

        verify(this.jobExecutor).execute(eq("admintools.purge"), any());
        assertEquals("Failed to start the automatic purge of [deletedDocuments] on wiki [xwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void startPurgeManually() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(SUBWIKI), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(3L);

        assertTrue(this.purgeManager.startPurge(SUBWIKI, DELETED_DOCUMENTS, USER));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor).execute(eq("admintools.purge"), requestCaptor.capture());
        assertEquals(Optional.of(USER), ((PurgeJobRequest) requestCaptor.getValue()).getTriggeredBy());
    }

    @Test
    void startPurgeWithoutExpiredItems() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(SUBWIKI), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(0L);

        assertFalse(this.purgeManager.startPurge(SUBWIKI, DELETED_DOCUMENTS, USER));
        // A disabled item type has nothing to purge either, and is not even counted.
        assertFalse(this.purgeManager.startPurge(SUBWIKI, DELETED_ATTACHMENTS, USER));

        verify(this.attachmentsHandler, never()).countExpired(anyString(), any());
        verify(this.jobExecutor, never()).getJob(any());
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }

    @Test
    void getExpiredCounts() throws Exception
    {
        when(this.policyResolver.getExpirationDate(eq(SUBWIKI), eq(DELETED_DOCUMENTS), any())).thenReturn(
            Optional.of(EXPIRATION_DATE));
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(7L);

        assertEquals(Map.of(DELETED_DOCUMENTS, 7L), this.purgeManager.getExpiredCounts(SUBWIKI));
    }

    /**
     * Check that the purge fails because the license is not active.
     */
    private void assertNoLicense()
    {
        JobException exception = assertThrows(JobException.class, () -> this.purgeManager.dispatchDuePurges());
        assertEquals("The automatic purge of the recycle bins was not started, since the Admin Tools license is not "
            + "active.", exception.getMessage());
    }

    /**
     * @param state the state of the job
     * @return a job in the given state
     */
    private Job mockJob(JobStatus.State state)
    {
        Job job = mock(Job.class);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);
        when(status.getState()).thenReturn(state);
        return job;
    }
}
