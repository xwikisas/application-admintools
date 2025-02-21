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
import org.xwiki.stability.Unstable;

/**
 * Provides the APIs needed by the Admin Tools server in order to download and view configuration files and logs.
 *
 * @version $Id$
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
     * Flush the cache of the XWiki instance, including all wikis, plugins and renderers.
     *
     * @return the status of the operation.
     * @throws XWikiRestException if an error occurred while flushing the cache.
     */
    @POST
    @Path("/flushCache")
    Response flushCache() throws XWikiRestException;

    /**
     * Start a package upload job based on the attachment reference that was sent.
     *
     * @param attachReference the reference of the attachment.
     * @param jobId a unique id for the job.
     * @return HTML status code 202 to hint that the upload job has started;
     *      Return status code 102 if the job already exists and is in progress;
     *      Return status code 401 if the user does not have admin rights;
     *      Return status code 500 if there is any error.
     * @throws XWikiRestException if an error occurred while creating the job, or if the user lacks admin rights.
     * @since 1.1
     */
    @POST
    @Path("/upload")
    @Unstable
    Response uploadPackageArchive(@QueryParam("attach") String attachReference,
        @QueryParam("jobId") String jobId) throws XWikiRestException;
}
