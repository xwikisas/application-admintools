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
import com.xwiki.admintools.internal.files.ImportantFilesManager;
import com.xwiki.admintools.internal.files.resources.LogsDataResource;
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
    private AuthorizationManager authorizationManager;

    @Override
    public Response getFile(String hint)
    {
        // Check to see if the request was made by a user with admin rights.
        if (!isAdmin()) {
            logger.warn("Failed to get file from DataResource [{}] due to restricted rights.", hint);
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            byte[] fileContent;
            if (hint.equals(LogsDataResource.HINT)) {
                XWikiContext wikiContext = xcontextProvider.get();
                XWikiRequest xWikiRequest = wikiContext.getRequest();
                String noLines = xWikiRequest.getParameter("noLines");
                if (noLines == null || noLines.equals("")) {
                    noLines = "1000";
                }
                fileContent = importantFilesManager.getFile(hint, noLines);
            } else {
                fileContent = importantFilesManager.getFile(hint, null);
            }
            InputStream inputStream = new ByteArrayInputStream(fileContent);
            return Response.ok(inputStream).type(MediaType.TEXT_PLAIN_TYPE).build();
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
        if (!isAdmin()) {
            logger.warn("Failed to get files due to restricted rights.");
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        try {
            XWikiContext wikiContext = xcontextProvider.get();
            XWikiRequest xWikiRequest = wikiContext.getRequest();
            Map<String, String[]> formParameters = xWikiRequest.getParameterMap();
            byte[] filesArchive = importantFilesManager.getFilesArchive(formParameters);
            // Set the appropriate response headers to indicate a zip file files.
            return Response.ok(filesArchive).type("application/zip")
                .header("Content-Disposition", "attachment; filename=AdminToolsFiles.zip").build();
        } catch (Exception e) {
            logger.warn("Failed to get zip archive. Root cause: [{}]", ExceptionUtils.getRootCauseMessage(e));
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
