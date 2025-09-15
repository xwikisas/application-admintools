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
package com.xwiki.admintools.internal.health.job;

import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.HealthCheckJobRequest;
import com.xwiki.admintools.jobs.HealthCheckJobStatus;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;

/**
 * The Admin Tools health check job.
 *
 * @version $Id$
 */
@Component
@Named(HealthCheckJob.JOB_TYPE)
public class HealthCheckJob extends AbstractJob<HealthCheckJobRequest, HealthCheckJobStatus> implements GroupedJob
{
    /**
     * Admin Tools health check job type.
     */
    public static final String JOB_TYPE = "admintools.healthcheck";

    @Inject
    private Provider<List<HealthCheck>> healthChecks;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        String wikiId = wikiContext.getWikiId();
        return new JobGroupPath(List.of("adminTools", "healthCheck", wikiId));
    }

    @Override
    protected HealthCheckJobStatus createNewStatus(HealthCheckJobRequest request)
    {
        return new HealthCheckJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    /**
     * Run the health check job.
     */
    @Override
    protected void runInternal()
    {
        List<HealthCheck> healthCheckList = healthChecks.get();
        this.progressManager.pushLevelProgress(healthCheckList.size(), this);
        Iterator<HealthCheck> healthCheckIterator = healthCheckList.iterator();
        try {
            while (healthCheckIterator.hasNext()) {
                if (status.isCanceled()) {
                    break;
                } else {
                    progressManager.startStep(this);
                    // We start the check for the current HealthCheck in the iterator.
                    JobResult checkResult = healthCheckIterator.next().check();
                    status.getJobResults().add(checkResult);
                    progressManager.endStep(this);
                    Thread.yield();
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred while running the health check.", e);
            status.getJobResults()
                .add(new JobResult("adminTools.dashboard.healthcheck.execution.error", JobResultLevel.FAIL));
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
