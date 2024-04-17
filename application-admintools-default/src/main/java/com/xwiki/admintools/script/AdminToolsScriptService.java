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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.api.Document;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.internal.AdminToolsManager;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.health.job.HealthCheckJob;
import com.xwiki.admintools.internal.usage.wikiResult.WikiRecycleBins;
import com.xwiki.admintools.internal.usage.wikiResult.WikiSizeResult;
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
    @Named("default")
    protected AdminToolsConfiguration adminToolsConfig;

    @Inject
    private AdminToolsManager adminToolsManager;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ModelContext modelContext;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private CurrentServer currentServer;

    /**
     * Retrieve all the configuration information in a format given by the associated templates generated by the data
     * providers.
     *
     * @return a {@link String} representing all templates.
     */
    public String getConfigurationData() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.generateData();
    }

    /**
     * Get a {@link List} of {@link WikiSizeResult} with the options to sort it and apply filters on it.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a filtered and sorted {@link List} of {@link WikiSizeResult}.
     * @throws AccessDeniedException if the requesting user lacks admin rights.
     */
    public List<WikiSizeResult> getWikisSize(Map<String, String> filters, String sortColumn, String order)
        throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.getWikiSizeResults(filters, sortColumn, order);
    }

    /**
     * Get a specific data provider information in a format given by the associated template.
     *
     * @param hint {@link String} representing the data provider
     * @return a {@link String} representing a specific template.
     */
    public String getConfigurationData(String hint) throws AccessDeniedException, ComponentLookupException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.generateData(hint);
    }

    /**
     * Retrieve the supported databases.
     *
     * @return inline list with supported databases separated by ",".
     */
    public List<String> getSupportedDatabases() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.getSupportedDBs();
    }

    /**
     * Retrieve the supported servers.
     *
     * @return inline list with supported servers separated by ",".
     */
    public List<String> getSupportedServers() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.getSupportedServers();
    }

    /**
     * Get the rendered template for accessing the downloads UI.
     *
     * @return a {@link String} representation of the template.
     */
    public String getFilesSection() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsManager.getFilesSection();
    }

    /**
     * Get the rendered template for viewing info about the size of the XWiki instance.
     *
     * @return a {@link String} representation of the template.
     */
    public String getInstanceSizeSection() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return adminToolsManager.getInstanceSizeTemplate();
    }

    /**
     * Retrieve the pages that have more than a given number of comments.
     *
     * @param maxComments maximum number of comments below which the page is ignored.
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the documents that have more than the given number of comments, or null if there are
     *     any errors.
     */
    public List<Document> getPagesOverGivenNumberOfComments(long maxComments, Map<String, String> filters,
        String sortColumn, String order) throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return adminToolsManager.getPagesOverGivenNumberOfComments(maxComments, filters, sortColumn, order);
    }

    /**
     * Retrieve the configuration settings for minimum spam size.
     *
     * @return an {@link Integer} representing the configured minimum spam size.
     */
    public int getMinimumSpamSize() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return this.adminToolsConfig.getSpamSize();
    }

    /**
     * Check if an Admin Tools Health Check job for the wiki from where the request was made exists. If it does, return
     * the job instance, else create a new Admin Tools health check request for the given wiki and start the execution.
     *
     * @return the asynchronous background job that will execute the request.
     */
    public Job runHealthChecks() throws Exception
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        List<String> requestId = this.getHealthCheckJobId();
        Job job = this.jobExecutor.getJob(requestId);
        if (job == null) {
            HealthCheckJobRequest healthCheckJobRequest = new HealthCheckJobRequest(requestId);
            return this.jobExecutor.execute(HealthCheckJob.JOB_TYPE, healthCheckJobRequest);
        } else {
            return job;
        }
    }

    /**
     * Get the Health Check job id for the current wiki.
     *
     * @return Health check job id.
     */
    public List<String> getHealthCheckJobId() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        String currentWiki = modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        return List.of("adminTools", "healthCheck", currentWiki);
    }

    /**
     * Check if the used server is compatible with Admin tools installation.
     *
     * @return a {@code true} if the used server is compatible with the application installation, or {@code false}
     *     otherwise.
     */
    public boolean isUsedServerCompatible()
    {
        return currentServer.getCurrentServer() != null;
    }

    /**
     * Get recycle bin info for all wikis in your instance with the options to sort and apply filters on it.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return @return a sorted and filtered {@link List} of {@link WikiRecycleBins} objects containing recycle bins
     *     info for wikis of the instance.
     */
    public List<WikiRecycleBins> getWikisRecycleBinSize(Map<String, String> filters, String sortColumn, String order)
        throws AccessDeniedException, WikiManagerException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
        return adminToolsManager.getWikisRecycleBinsSize(filters, sortColumn, order);
    }
}
