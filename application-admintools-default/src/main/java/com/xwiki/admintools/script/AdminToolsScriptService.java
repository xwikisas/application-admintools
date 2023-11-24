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
package com.xwiki.admintools.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.internal.AdminToolsManager;
import com.xwiki.admintools.internal.health.job.HealthCheckJob;
import com.xwiki.admintools.jobs.HealthCheckJobRequest;

/**
 * Admin Tools script services.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("admintools")
@Singleton
@Unstable
public class AdminToolsScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    private AdminToolsManager adminToolsManager;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    /**
     * Retrieve all the configuration information in a format given by the associated templates generated by the data
     * providers.
     *
     * @return a {@link String} representing all templates.
     */
    public String getConfigurationData()
    {
        return this.adminToolsManager.generateData();
    }

    /**
     * Get a specific data provider information in a format given by the associated template.
     *
     * @param hint {@link String} representing the data provider
     * @return a {@link String} representing a specific template.
     */
    public String getConfigurationData(String hint) throws ComponentLookupException
    {
        try {
            return this.adminToolsManager.generateData(hint);
        } catch (ComponentLookupException e) {
            logger.error("Could not retrieve data for the given hint [{}]. Root cause is: [{}]", hint,
                ExceptionUtils.getRootCauseMessage(e));
            throw e;
        }
    }

    /**
     * Retrieve the supported databases.
     *
     * @return inline list with supported databases separated by ",".
     */
    public List<String> getSupportedDatabases()
    {
        return this.adminToolsManager.getSupportedDBs();
    }

    /**
     * Retrieve the supported servers.
     *
     * @return inline list with supported servers separated by ",".
     */
    public List<String> getSupportedServers()
    {
        return this.adminToolsManager.getSupportedServers();
    }

    /**
     * Get the rendered template for accessing the downloads UI.
     *
     * @return a {@link String} representation of the template.
     */
    public String getFilesSection()
    {
        return this.adminToolsManager.getFilesSection();
    }

    /**
     * Check if an Admin Tools Health Check job for the wiki from where the request was made exists. If it does, return
     * the job instance, else create a new Admin Tools health check request for the given wiki and start the execution.
     *
     * @return the asynchronous background job that will execute the request.
     */
    public Job runHealthChecks()
    {
        try {
            List<String> requestId = this.getHealthCheckJobId();
            Job job = this.jobExecutor.getJob(requestId);
            if (job == null) {
                HealthCheckJobRequest healthCheckJobRequest = new HealthCheckJobRequest(requestId);
                return this.jobExecutor.execute(HealthCheckJob.JOB_TYPE, healthCheckJobRequest);
            } else {
                return job;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the Health Check job id for the current wiki.
     *
     * @return Health check job id.
     */
    public List<String> getHealthCheckJobId()
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        String wikiID = wikiContext.getWikiId();
        return List.of("adminTools", "healthCheck", wikiID);
    }
}
