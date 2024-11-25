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

import org.xwiki.stability.Unstable;

/**
 * Result of a job. Stores a custom message for the summary of the result, the severity level represented by
 * {@link JobResultLevel}. The result may also contain a {@link List} of additional parameters used if there is
 * need to store more information about the result.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class JobResult
{
    private String message;

    private JobResultLevel level;

    private List<?> parameters;

    /**
     * Used for registering a result.
     *
     * @param message error message representing the summary of the found issue.
     * @param level severity level of a result.
     * @param parameters current value of the checked resource.
     */
    public JobResult(String message, JobResultLevel level, Object... parameters)
    {
        this.message = message;
        this.level = level;
        this.parameters = List.of(parameters);
    }

    /**
     * Get the error body.
     *
     * @return a summary of the error.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Get the recommendation for the set error.
     *
     * @return a suggestion for fixing the error.
     */
    public List<?> getParameters()
    {
        return parameters;
    }

    /**
     * Get the severity level a detected error.
     *
     * @return the severity level of an error.
     */
    public JobResultLevel getLevel()
    {
        return level;
    }
}
