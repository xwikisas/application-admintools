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
package com.xwiki.admintools.jobs;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.stability.Unstable;

/**
 * The status of a package upload job.
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class PackageUploadJobStatus extends DefaultJobStatus<PackageUploadJobRequest>
{
    private List<JobResult> uploadLogs = new LinkedList<>();

    /**
     * Create a new import job status.
     *
     * @param jobType the job type.
     * @param request the request provided when the job was started.
     * @param observationManager the observation manager.
     * @param loggerManager the logger manager.
     */
    public PackageUploadJobStatus(String jobType, PackageUploadJobRequest request,
        ObservationManager observationManager, LoggerManager loggerManager)
    {
        super(jobType, request, null, observationManager, loggerManager);
        setCancelable(true);
    }

    /**
     * Get the list result list from the job.
     *
     * @return list with {@link JobResult} containing the results.
     */
    public List<JobResult> getJobResults()
    {
        return uploadLogs;
    }

    /**
     * Add a new log to the job results.
     *
     * @param statusLog the new log result.
     */
    public void addLog(JobResult statusLog)
    {
        uploadLogs.add(statusLog);
    }

    /**
     * Check if any job result has a specific level of severity.
     *
     * @param level represents the searched level of severity.
     * @return {@code true} if there is any match for the given level, or {@code false} otherwise.
     */
    public boolean hasLevel(JobResultLevel level)
    {
        return this.uploadLogs.stream().anyMatch(checkResult -> Objects.equals(level, checkResult.getLevel()));
    }
}
