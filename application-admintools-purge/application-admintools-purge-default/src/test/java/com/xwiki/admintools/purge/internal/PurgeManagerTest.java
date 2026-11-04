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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
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
import com.xwiki.admintools.purge.internal.policy.RetentionCalculator;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.licensing.Licensor;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private RetentionCalculator retentionCalculator;

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
        // A scheduled purge applies the settings stored when the job runs.
        assertEquals(Optional.empty(), documentsRequest.getTriggeredBy());
        assertEquals(Optional.empty(), documentsRequest.getExpirationDate());
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
    void startManualPurge() throws Exception
    {
        when(this.retentionCalculator.getExpirationDate(any(), eq(10), eq(IntervalUnit.YEARS))).thenReturn(
            EXPIRATION_DATE);
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(3L);

        assertTrue(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 10, IntervalUnit.YEARS, USER));

        // The limit chosen by the user applies, whatever the settings, and the job removes the items counted now.
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor).execute(eq("admintools.purge"), requestCaptor.capture());
        PurgeJobRequest request = (PurgeJobRequest) requestCaptor.getValue();
        assertEquals(Optional.of(USER), request.getTriggeredBy());
        assertEquals(Optional.of(EXPIRATION_DATE), request.getExpirationDate());
        verify(this.policyResolver, never()).getExpirationDate(anyString(), any(), any());
    }

    @Test
    void startManualPurgeWithZeroLimit() throws Exception
    {
        // A limit of 0 empties the recycle bin: the job removes every item deleted until the purge started.
        when(this.retentionCalculator.getExpirationDate(any(), eq(0), eq(IntervalUnit.DAYS))).thenReturn(
            EXPIRATION_DATE);
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(7L);

        assertTrue(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 0, IntervalUnit.DAYS, USER));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(this.jobExecutor).execute(eq("admintools.purge"), requestCaptor.capture());
        assertEquals(Optional.of(EXPIRATION_DATE), ((PurgeJobRequest) requestCaptor.getValue()).getExpirationDate());
    }

    @Test
    void startManualPurgeWithoutExpiredItems() throws Exception
    {
        when(this.retentionCalculator.getExpirationDate(any(), eq(1), eq(IntervalUnit.MONTHS))).thenReturn(
            EXPIRATION_DATE);
        when(this.documentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(0L);

        assertFalse(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1, IntervalUnit.MONTHS, USER));

        verify(this.jobExecutor, never()).getJob(any());
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }

    @Test
    void startManualPurgeWithInvalidLimit() throws Exception
    {
        assertThrows(IllegalArgumentException.class,
            () -> this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, -1, IntervalUnit.DAYS, USER));
        assertThrows(IllegalArgumentException.class,
            () -> this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1, null, USER));

        // The interval can't be larger than the one of the settings.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1001, IntervalUnit.YEARS, USER));
        assertEquals("Invalid purge limit [1001 YEARS]: the interval must be between 0 and 1000, with a unit.",
            exception.getMessage());

        verify(this.retentionCalculator, never()).getExpirationDate(any(), anyInt(), any());
        verify(this.documentsHandler, never()).countExpired(anyString(), any());
        verify(this.jobExecutor, never()).execute(anyString(), any());
    }

    @Test
    void checkLimit()
    {
        // The smallest and the largest intervals are valid: unlike the settings, a manual purge accepts 0, which
        // purges every item deleted until now.
        this.purgeManager.checkLimit(0, IntervalUnit.DAYS);
        this.purgeManager.checkLimit(1000, IntervalUnit.YEARS);

        assertThrows(IllegalArgumentException.class, () -> this.purgeManager.checkLimit(-1, IntervalUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> this.purgeManager.checkLimit(1001, IntervalUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> this.purgeManager.checkLimit(1, null));
    }

    @Test
    void countExpired() throws Exception
    {
        when(this.retentionCalculator.getExpirationDate(any(), eq(7), eq(IntervalUnit.DAYS))).thenReturn(
            EXPIRATION_DATE);
        when(this.attachmentsHandler.countExpired(SUBWIKI, EXPIRATION_DATE)).thenReturn(7L);

        assertEquals(7L, this.purgeManager.countExpired(SUBWIKI, DELETED_ATTACHMENTS, 7, IntervalUnit.DAYS));
        assertThrows(IllegalArgumentException.class,
            () -> this.purgeManager.countExpired(SUBWIKI, DELETED_ATTACHMENTS, -1, IntervalUnit.DAYS));
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
