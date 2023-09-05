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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.pages.ModifiablePageResource;

import com.xwiki.admintools.internal.downloads.DownloadsManager;
import com.xwiki.admintools.rest.AdminToolsResources;

/**
 * Default implementation of {@link AdminToolsResources}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("com.xwiki.admintools.internal.rest.DefaultResources")
@Singleton
public class DefaultResources extends ModifiablePageResource implements AdminToolsResources
{
    private final String contentDisposition = "Content-Disposition";

    @Inject
    private Logger logger;

    /**
     * Handles downloads requests.
     */
    @Inject
    private Provider<DownloadsManager> downloadsManagerProvider;

    @Override
    public Response getConfigs(String type) throws XWikiRestException
    {
//        boolean a = request.isUserInRole("admin");
        // Check to see if the request was made by a user with admin rights.
        if (downloadsManagerProvider.get().isAdmin()) {
            logger.warn("Failed to get file xwiki.[{}] due to restricted rights.", type);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            byte[] xWikiFileContent = downloadsManagerProvider.get().downloadXWikiFile(type);
            InputStream inputStream = new ByteArrayInputStream(xWikiFileContent);

            Response.ResponseBuilder response = Response.ok(inputStream);
            response.type(MediaType.TEXT_PLAIN_TYPE);

            // Set the appropriate response headers to indicate a file download.
            if (type.equals("properties")) {
                response.header(contentDisposition, "attachment; filename=xwiki.properties");
                return response.build();
            } else if (type.equals("config")) {
                response.header(contentDisposition, "attachment; filename=xwiki.cfg");
                return response.build();
            } else {
                return Response.status(404).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get file [{}]. Root cause: [{}]", type, ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Response getLogs(String from, String to) throws XWikiRestException
    {
        try {
            Map<String, String> filters = new HashMap<>();
            filters.put("from", from);
            filters.put("to", to);
            byte[] logsArchive = downloadsManagerProvider.get().downloadLogs(filters);
            if (logsArchive != null) {
                // Set the appropriate response headers to indicate a zip file download.
                Response.ResponseBuilder response = Response.ok(logsArchive);
                response.header("Content-Type", "application/zip");
                response.header(contentDisposition, "attachment; filename=logs_archive.zip");
                return response.build();
            } else {
                // Handle the case when no logs are found or an error occurs.
                return Response.status(Response.Status.NOT_FOUND).entity("No logs found.").build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get logs. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
