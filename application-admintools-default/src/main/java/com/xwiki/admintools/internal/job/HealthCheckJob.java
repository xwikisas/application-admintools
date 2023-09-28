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
package com.xwiki.admintools.internal.job;

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;

import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.internal.health.HealthCheckManager;
import com.xwiki.admintools.jobs.HealthCheckJobRequest;
import com.xwiki.admintools.jobs.HealthCheckJobStatus;

@Component
@Named(HealthCheckJob.JOB_TYPE)
public class HealthCheckJob extends AbstractJob<HealthCheckJobRequest, HealthCheckJobStatus>
{
    /**
     * The PDF export job type.
     */
    public static final String JOB_TYPE = "admintools.healthcheck";

    @Inject
    private HealthCheckManager healthCheckManager;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(1, this);

        try {
            Iterator<HealthCheckResult> t = healthCheckManager.runHealthChecks();
            while (t.hasNext()) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    t.next();
                    Thread.yield();
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }
}
