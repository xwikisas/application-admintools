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
 * Represents a request to start a health check on current wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class HealthCheckJobRequest extends AbstractRequest
{
    /**
     * Default constructor.
     */
    public HealthCheckJobRequest()
    {
        setDefaultId();
    }

    /**
     * Creates a request specific to the wiki from which the call was made.
     *
     * @param requestId the ID for the request.
     */
    public HealthCheckJobRequest(List<String> requestId)
    {
        setId(requestId);
    }

    private void setDefaultId()
    {
        List<String> id = List.of("adminTools", "healthCheck");
        setId(id);
    }
}
