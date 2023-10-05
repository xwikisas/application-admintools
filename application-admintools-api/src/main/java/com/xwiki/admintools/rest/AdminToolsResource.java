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
package com.xwiki.admintools.rest;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;

/**
 * Provides the APIs needed by the Admin Tools server in order to download and view configuration files and logs.
 *
 * @version $Id$
 * @since 1.0
 */
@Path("/admintools")
public interface AdminToolsResource extends XWikiRestComponent
{
    /**
     * Access the XWiki configuration or properties files.
     *
     * @param type {link String} specifies the XWiki searched file.
     * @return {@link Response} with the content of the file, or specific error code.
     * @throws XWikiRestException
     */
    @GET
    @Path("/files/{fileType}")
    Response getFileView(@PathParam("fileType") String type) throws XWikiRestException;

    /**
     * Download files from system server.
     *
     * @return {@link Response} with a configured header for zip download, or specific error code.
     * @throws XWikiRestException
     */
    @POST
    @Path("/files")
    Response getFiles() throws XWikiRestException;

    /**
     * Retrieve last logs from server.
     *
     * @return {@link Response} with the content of the last logs, or specific error code.
     * @throws XWikiRestException
     */
    @POST
    @Path("/files/logs")
    Response getLastLogs() throws XWikiRestException;
}
