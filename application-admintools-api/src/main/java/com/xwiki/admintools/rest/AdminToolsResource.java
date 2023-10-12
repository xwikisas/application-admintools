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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
     * Access the content of a specific file, based on received type.
     *
     * @param type identifier of the requested file.
     * @return the content of this file.
     * @throws XWikiRestException if an error occurred while accessing the file.
     */
    @GET
    @Path("/files/{fileType}")
    Response getFile(@PathParam("fileType") String type) throws XWikiRestException;

    /**
     * Download an archive with the requested files.
     *
     * @return an archive with files.
     * @throws XWikiRestException if an error occurred while getting the archive.
     */
    @GET
    @Path("/files")
    Response getFiles() throws XWikiRestException;

    /**
     * Get last n lines of server logs.
     *
     * @param noLines number of lines to be retrieved from the log file.
     * @return a file containing the last n lines of server logs.
     * @throws XWikiRestException if an error occurred while retrieving the logs file .
     */
    @GET
    @Path("/files/logs/{noLines}")
    Response getLastLogs(@PathParam("noLines") @DefaultValue("1000") String noLines) throws XWikiRestException;
}
