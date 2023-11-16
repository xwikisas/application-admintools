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
 * Result of a health check. May store the error message, severity level, recommendation and the current value of the
 * checked resource. The severity level is used as "info", for informative result, "warn" for warnings and "error"
 * for critical issues.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class HealthCheckResult
{
    private String message;

    private String recommendation;

    private String level;

    private String currentValue;

    /**
     * Used for registering a result.
     *
     * @param message Error message representing the summary of the found issue.
     * @param recommendation suggestion in context of the message.
     * @param level severity level of a result.
     * @param currentValue Current value of the checked resource.
     */
    public HealthCheckResult(String message, String recommendation, String level, String currentValue)
    {
        this.message = message;
        this.recommendation = recommendation;
        this.level = level;
        this.currentValue = currentValue;
    }

    /**
     * Partial result definition.
     *
     * @param message Error message representing the summary of the found issue.
     * @param recommendation suggestion in context of the message.
     * @param level severity level of a result.
     */
    public HealthCheckResult(String message, String recommendation, String level)
    {
        this(message, recommendation, level, null);
    }

    /**
     * Simple result definition.
     *
     * @param message Error message representing the summary of the found issue.
     * @param level severity level of a result.
     */
    public HealthCheckResult(String message, String level)
    {
        this(message, null, level);
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
    public String getRecommendation()
    {
        return recommendation;
    }

    /**
     * Get the severity level a detected error.
     *
     * @return the severity level of an error.
     */
    public String getLevel()
    {
        return level;
    }

    /**
     * Get the value of a checked resource.
     *
     * @return the value of a checked resource.
     */
    public String getCurrentValue()
    {
        return currentValue;
    }
}
