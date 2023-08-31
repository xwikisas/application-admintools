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

import javax.inject.Inject;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.rest.XWikiRestException;

import com.xwiki.admintools.internal.downloads.DownloadsManager;
import com.xwiki.admintools.rest.AdminToolsRestApi;

/**
 * TBC.
 *
 * @version $Id$
 * @since 1.0
 */
public class DefaultRestApi implements AdminToolsRestApi
{
    @Inject
    private Logger logger;

    @Inject
    private DownloadsManager downloadsManager;

    @Override
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getConfigs(String type, String token) throws XWikiRestException
    {
//        if (token == null || !fileTokenManager.hasAccess(token)) {
//            logger.warn("Failed to get file [{}] due to invalid token or restricted rights.", fileId);
//            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
//        }
        try {
            byte[] xWikiFile = downloadsManager.downloadXWikiFile(type);
            String contentDisposition = "Content-Disposition";
            if (type.equals("properties")) {
                return Response.ok(xWikiFile, MediaType.APPLICATION_OCTET_STREAM)
                    .header(contentDisposition, "attachment; filename = xwiki.properties").build();
            } else if (type.equals("configuration")) {
                return Response.ok(xWikiFile, MediaType.APPLICATION_OCTET_STREAM)
                    .header(contentDisposition, "attachment; filename = xwiki.cfg").build();
            } else {
                return Response.status(401).build();
            }
        } catch (Exception e) {
            logger.warn("Failed to get file [{}]. Root cause: [{}]", type, ExceptionUtils.getRootCauseMessage(e));
            throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * TBC.
     *
     * @param type
     * @param token
     * @return TBC
     * @throws XWikiRestException
     */
    @Override
    public Response getLogs(String type, String token) throws XWikiRestException
    {
        return null;
    }
}
