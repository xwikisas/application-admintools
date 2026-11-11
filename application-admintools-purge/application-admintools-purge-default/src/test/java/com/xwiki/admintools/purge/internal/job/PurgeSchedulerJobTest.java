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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;
import com.xwiki.admintools.purge.internal.PurgeManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeSchedulerJob}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeSchedulerJobTest
{
    private static final DocumentReference SCHEDULER_JOB_PAGE =
        new DocumentReference("xwiki", List.of("AdminTools", "Purge", "Code"), "SchedulerJob");

    private final PurgeSchedulerJob job = new PurgeSchedulerJob();

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private PurgeManager purgeManager;

    // The scheduler job initializes the execution context through these components.
    @MockComponent
    private ExecutionContextManager executionContextManager;

    @MockComponent
    private Execution execution;

    @Mock
    private JobExecutionContext jobContext;

    @Mock
    private JobDetail jobDetail;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private BaseObject jobObject;

    @BeforeEach
    void setUp()
    {
        Utils.setComponentManager(this.componentManager);
        JobDataMap data = new JobDataMap();
        data.put("context", this.xcontext);
        data.put("xjob", this.jobObject);
        when(this.jobContext.getJobDetail()).thenReturn(this.jobDetail);
        when(this.jobDetail.getJobDataMap()).thenReturn(data);
        when(this.xcontext.clone()).thenReturn(this.xcontext);
        when(this.xcontext.getMainXWiki()).thenReturn("xwiki");
        when(this.jobObject.getDocumentReference()).thenReturn(SCHEDULER_JOB_PAGE);
    }

    @AfterEach
    void tearDown()
    {
        Utils.setComponentManager(null);
    }

    @Test
    void execute() throws Exception
    {
        this.job.execute(this.jobContext);

        verify(this.purgeManager).dispatchDuePurges();
        verify(this.execution).removeContext();
    }

    @Test
    void executeFromAnotherPage() throws Exception
    {
        when(this.jobObject.getDocumentReference()).thenReturn(
            new DocumentReference("subwiki", List.of("AdminTools", "Purge", "Code"), "SchedulerJob"));

        JobExecutionException exception =
            assertThrows(JobExecutionException.class, () -> this.job.execute(this.jobContext));

        assertEquals(
            "The purge of the recycle bins can only be started from " + "[xwiki:AdminTools.Purge.Code.SchedulerJob].",
            exception.getMessage());
        verify(this.purgeManager, never()).dispatchDuePurges();
    }

    @Test
    void executeFailsWithTheReason() throws Exception
    {
        JobException error = new JobException("The license is not active.");
        doThrow(error).when(this.purgeManager).dispatchDuePurges();

        JobExecutionException exception =
            assertThrows(JobExecutionException.class, () -> this.job.execute(this.jobContext));

        assertEquals("Failed to start the purge of the recycle bins.", exception.getMessage());
        assertSame(error, exception.getCause());
    }
}
