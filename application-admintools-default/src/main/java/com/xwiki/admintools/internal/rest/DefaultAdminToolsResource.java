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
package com.xwiki.admintools.internal.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.internal.files.ImportantFilesManager;
import com.xwiki.admintools.internal.health.cache.data.CacheDataFlusher;
import com.xwiki.admintools.internal.uploadJob.UploadJob;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;
import com.xwiki.admintools.jobs.PackageUploadJobRequest;
import com.xwiki.admintools.rest.AdminToolsResource;

/**
 * Default implementation of {@link AdminToolsResource}.
 *
 * @version $Id$
 */
@Component
@Named("com.xwiki.admintools.internal.rest.DefaultAdminToolsResource")
@Singleton
public class DefaultAdminToolsResource extends ModifiablePageResource implements AdminToolsResource
{
    @Inject
    private Logger logger;

    /**
     * Handles files requests.
     */
    @Inject
    private ImportantFilesManager importantFilesManager;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private CacheDataFlusher cacheDataFlusher;

    @Override
    public Response getFile(String hint)
    {
        // Check to see if the request was made by a user with admin rights.
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
            byte[] fileContent;
            XWikiContext wikiContext = xcontextProvider.get();
            XWikiRequest xWikiRequest = wikiContext.getRequest();
            Map<String, String[]> formParameters = xWikiRequest.getParameterMap();

            fileContent = importantFilesManager.getFile(hint, formParameters);
            InputStream inputStream = new ByteArrayInputStream(fileContent);
            return Response.ok(inputStream).type(MediaType.TEXT_PLAIN_TYPE).build();
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to get file from DataResource [{}] due to restricted rights.", hint);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (IOException e) {
            logger.warn("Error while handling file from DataResource [{}]. Root cause: [{}]", hint,
                ExceptionUtils.getRootCauseMessage(e));
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (Exception e) {
            logger.warn("Failed to get data from DataResource [{}]. Root cause: [{}]", hint,
                ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getFiles()
    {
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
            XWikiContext wikiContext = xcontextProvider.get();
            XWikiRequest xWikiRequest = wikiContext.getRequest();
            Map<String, String[]> formParameters = xWikiRequest.getParameterMap();
            byte[] filesArchive = importantFilesManager.getFilesArchive(formParameters);
            // Set the appropriate response headers to indicate a zip file files.
            return Response.ok(filesArchive).type("application/zip")
                .header("Content-Disposition", "attachment; filename=AdminToolsFiles.zip").build();
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to get files due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn("Failed to get zip archive. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response flushCache()
    {
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
            this.contextualAuthorizationManager.checkAccess(Right.PROGRAM);
            XWikiContext xwikiContext = xcontextProvider.get();
            XWiki xwiki = xwikiContext.getWiki();
            xwiki.flushCache(xwikiContext);
            return Response.ok().type(MediaType.TEXT_PLAIN_TYPE).build();
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to flush the cache due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn("Failed to flush instance cache. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response flushJMXCache()
    {
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
            this.contextualAuthorizationManager.checkAccess(Right.PROGRAM);
            boolean success = cacheDataFlusher.clearAllCache();
            if (success) {
                return Response.ok().build();
            } else {
                logger.warn("There were some errors while flushing the JMX cache.");
                return Response.ok().type(MediaType.TEXT_PLAIN_TYPE).build();
            }
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to flush JMX caches due to restricted rights.", deniedException);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn("Failed to flush JMX caches. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response flushJMXEntryCache(String entryName)
    {
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);
            this.contextualAuthorizationManager.checkAccess(Right.PROGRAM);
            boolean found = cacheDataFlusher.clearCache(entryName);
            if (found) {
                return Response.ok().build();
            } else {
                logger.warn("[{}] JMX cache not found.", entryName);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to flush JMX cache due to restricted rights.", deniedException);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn("Failed to flush JMX cache. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response uploadPackageArchive(String attachReference, String startTime)
    {
        try {
            this.contextualAuthorizationManager.checkAccess(Right.ADMIN);

            List<String> jobId = List.of("adminTools", "upload", attachReference, startTime);
            Job job = this.jobExecutor.getJob(jobId);
            if (job == null) {
                PackageUploadJobRequest packageUploadJobRequest = new PackageUploadJobRequest(attachReference, jobId);
                UploadJob uploadJob = (UploadJob) this.jobExecutor.execute(UploadJob.JOB_TYPE, packageUploadJobRequest);
                if (this.jobExecutor.getCurrentJob(uploadJob.getGroupPath()) != null) {
                    uploadJob.getStatus()
                        .addLog(new JobResult("adminTools.jobs.upload.start.waiting", JobResultLevel.INFO));
                }
                return Response.status(202).build();
            } else {
                return Response.status(102).build();
            }
        } catch (AccessDeniedException deniedException) {
            logger.warn("Failed to begin the package upload due to insufficient rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        } catch (Exception e) {
            logger.warn("Failed to begin package upload job. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
