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
package com.xwiki.admintools.internal.health.checks.cache;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;

import com.xwiki.admintools.health.HealthCheckComponent;
import com.xwiki.admintools.health.HealthCheckResult;

@Component
@Named(CacheHealthCheckComponent.HINT)
@Singleton
public class CacheHealthCheckComponent implements HealthCheckComponent
{
    public static final String HINT = "CACHE_HEALTH";

    @Inject
    JobProgressManager jobProgressManager;

    @Override
    public List<HealthCheckResult> check()
    {
        List<HealthCheckResult> results = new ArrayList<>();
        return results;
    }
}
