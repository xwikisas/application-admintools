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
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.internal.download.DownloadManager;
import com.xwiki.admintools.internal.download.resources.LogsDataResource;
import com.xwiki.admintools.rest.AdminToolsResource;

/**
 * Default implementation of {@link AdminToolsResource}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("com.xwiki.admintools.internal.rest.DefaultAdminToolsResource")
@Singleton
public class DefaultAdminToolsResource extends ModifiablePageResource implements AdminToolsResource
{
    private final String contentDisposition = "Content-Disposition";

    @Inject
    private Logger logger;

    /**
     * Handles download requests.
     */
    @Inject
    private DownloadManager downloadManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Override
    public Response getFileView(String hint)
    {
        // Check to see if the request was made by a user with admin rights.
        if (!isAdmin()) {
            logger.warn("Failed to get file from DataResource [{}] due to restricted rights.", hint);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            byte[] xWikiFileContent = downloadManager.getFileView(null, hint);
            if (xWikiFileContent.length == 0) {
                return Response.status(404).build();
            }
            InputStream inputStream = new ByteArrayInputStream(xWikiFileContent);
            return Response.ok(inputStream).type(MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception e) {
            logger.warn("Failed to get file from DataResource [{}]. Root cause: [{}]", hint,
                ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getFiles()
    {
        if (!isAdmin()) {
            logger.warn("Failed to get files due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            XWikiContext wikiContext = xcontextProvider.get();
            XWikiRequest xWikiRequest = wikiContext.getRequest();
            Map<String, String[]> files = xWikiRequest.getParameterMap();
            byte[] filesArchive = downloadManager.downloadMultipleFiles(files);
            if (!(filesArchive == null) && !(Arrays.toString(filesArchive).length() == 0)) {
                // Set the appropriate response headers to indicate a zip file download.
                Response.ResponseBuilder response = Response.ok(filesArchive);
                response.header("Content-Type", "application/zip");
                response.header(contentDisposition, "attachment; filename=files_archive.zip");
                return response.build();
            } else {
                // Handle the case when no logs are found or an error occurs.
                return Response.status(Response.Status.NOT_FOUND).entity("No files found.").build();
            }
        } catch (Exception e) {
            logger.warn("Failed to download files. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getLastLogs()
    {
        if (!isAdmin()) {
            logger.warn("Failed to get the logs due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            XWikiContext wikiContext = xcontextProvider.get();
            XWikiRequest xWikiRequest = wikiContext.getRequest();
            String noLines = xWikiRequest.getParameter("noLines");
            byte[] xWikiFileContent = downloadManager.getFileView(noLines, LogsDataResource.HINT);
            if (xWikiFileContent.length == 0) {
                return Response.status(404).build();
            }
            InputStream inputStream = new ByteArrayInputStream(xWikiFileContent);
            return Response.ok(inputStream).type(MediaType.TEXT_PLAIN_TYPE).build();
        } catch (Exception e) {
            logger.warn("Failed to get logs. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isAdmin()
    {
        XWikiContext wikiContext = xcontextProvider.get();
        DocumentReference user = wikiContext.getUserReference();
        WikiReference wikiReference = wikiContext.getWikiReference();
        return this.authorizationManager.hasAccess(Right.ADMIN, user, wikiReference);
    }
}