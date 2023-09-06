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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;

/**
 * Provides the APIs needed by the Admin Tools server in order to download configuration files and a logs archive.
 *
 * @version $Id$
 * @since 1.0
 */
@Path("/admintools/download")
public interface AdminToolsResources extends XWikiRestComponent
{
    /**
     * REST endpoint for accessing the XWiki configuration or properties download.
     *
     * @param type specifies the XWiki searched file.
     * @return Response with a configured header for file download.
     * @throws XWikiRestException
     */
    @GET
    @Path("/configs/{fileType}")
    Response getConfigs(@PathParam("fileType") String type) throws XWikiRestException;

//    /**
//     * REST endpoint for accessing the logs archive download.
//     *
//     * @param from filter date, starting from.
//     * @param to filter date, until.
//     * @return Response with a configured header for zip download.
//     * @throws XWikiRestException
//     */
//    @GET
//    @Path("/logs")
//    Response getLogs(@QueryParam("from") String from, @QueryParam("to") String to) throws XWikiRestException;

    /**
     * Rest endpoint to download all selected files.
     *
     * @param from filter date, starting from given value.
     * @param to filter date, until given value.
     * @return Response with a configured header for zip download.
     * @throws XWikiRestException
     */
    @POST
    @Path("/all")
    Response getFiles(@QueryParam("from") String from, @QueryParam("to") String to) throws XWikiRestException;
}
