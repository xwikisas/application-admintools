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
package com.xwiki.admintools.jobs;

import java.util.List;

import org.xwiki.job.AbstractRequest;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to start a package upload job.
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class PackageUploadJobRequest extends AbstractRequest
{
    private String fileRef;

    /**
     * Default constructor.
     */
    public PackageUploadJobRequest()
    {
        setDefaultId();
    }

    /**
     * Creates a specific request for package upload job.
     *
     * @param fileRef the attachment reference.
     * @param jobId the ID of the request.
     */
    public PackageUploadJobRequest(String fileRef, List<String> jobId)
    {
        this.fileRef = fileRef;
        setId(jobId);
    }

    /**
     * Get the attachment reference.
     *
     * @return a {@link String} representing the attachment reference.
     */
    public String getFileRef()
    {
        return this.fileRef;
    }

    private void setDefaultId()
    {
        setId(List.of("adminTools", "upload"));
    }
}
