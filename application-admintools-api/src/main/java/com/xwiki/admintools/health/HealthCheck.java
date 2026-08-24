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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.jobs.JobResult;

/**
 * Check for issues in the current wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Unstable
public interface HealthCheck
{
    /**
     * Execute the health check on the wiki instance.
     *
     * @return a {@link JobResult} with the relevant info regarding the checked issue.
     */
    JobResult check();

    /**
     * Indicate whether this health check can be applied or not. Returns {@code true} by default.
     *
     * @return true if the check should be executed, false otherwise
     * @since 1.5
     */
    @Unstable
    default boolean isApplicable() throws Exception
    {
        return true;
    }
}
