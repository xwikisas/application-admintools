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

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.AbstractJob;
import com.xpn.xwiki.web.Utils;
import com.xwiki.admintools.purge.internal.PurgeManager;

/**
 * Starts the daily purge of the recycle bins. The scheduler runs it from the {@code AdminTools.Purge.Code.SchedulerJob}
 * page of the main wiki, and only from that page.
 *
 * @version $Id$
 * @since 1.5
 */
public class PurgeSchedulerJob extends AbstractJob
{
    private static final List<String> SCHEDULER_JOB_SPACE = List.of("AdminTools", "Purge", "Code");

    @Override
    protected void executeJob(JobExecutionContext jobContext) throws JobExecutionException
    {
        // Another scheduler page naming this class doesn't start the purge: only the page of Admin Tools does.
        DocumentReference expectedPage =
            new DocumentReference(getXWikiContext().getMainXWiki(), SCHEDULER_JOB_SPACE, "SchedulerJob");
        BaseObject job = (BaseObject) jobContext.getJobDetail().getJobDataMap().get("xjob");
        if (job == null || !expectedPage.equals(job.getDocumentReference())) {
            throw new JobExecutionException(
                String.format("The purge of the recycle bins can only be started from [%s].", expectedPage));
        }
        try {
            // The scheduler creates the job itself, so the components can't be injected.
            Utils.getComponent(PurgeManager.class).dispatchDuePurges();
        } catch (Exception e) {
            throw new JobExecutionException("Failed to start the purge of the recycle bins.", e);
        }
    }
}
