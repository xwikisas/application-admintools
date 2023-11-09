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
package com.xwiki.admintools.health;

import org.xwiki.stability.Unstable;

/**
 * Result of a health check. Stores the error message and recommendation.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class HealthCheckResult
{
    private String errorMessage;

    private String recommendation;

    /**
     * Used for registering an error.
     *
     * @param errorMessage Error message representing the summary of the found issue.
     * @param recommendation Suggestion for fixing the issue.
     */
    public HealthCheckResult(String errorMessage, String recommendation)
    {
        this.errorMessage = errorMessage;
        this.recommendation = recommendation;
    }

    /**
     * Used for initializing an empty body result.
     */
    public HealthCheckResult()
    {
        this(null, null);
    }

    /**
     * Get the error body.
     * @return a summary of the error.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }

    /**
     * Get the recommendation for the set error.
     *
     * @return a suggestion for fixing the error.
     */
    public String getRecommendation()
    {
        return recommendation;
    }

    /**
     * Set the message error.
     * @param err summary of the error.
     */
    public void setErrorMessage(String err)
    {
        this.errorMessage = err;
    }

    /**
     * Set the error recommendation.
     * @param recommendation summary of the fix.
     */
    public void setRecommendation(String recommendation)
    {
        this.recommendation = recommendation;
    }
}
