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
package com.xwiki.admintools.internal.health.checks.memory;

import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking JVM memory allocation.
 *
 * @version $Id$
 */
@Component
@Named(HeapAllocationCheck.HINT)
@Singleton
public class HeapAllocationCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "memoryHeapAllocation";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public JobResult check()
    {
        XWiki wiki = xcontextProvider.get().getWiki();
        float maxMemory = wiki.maxMemory() / (1024.0f * 1024);
        float initialMemoryAllocation =
            ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit() / (1024.0f * 1024);
        if (initialMemoryAllocation != maxMemory) {
            logger.warn("To improve performance -Xms  and -Xmx memory allocation should be identical.");
            return new JobResult("adminTools.dashboard.healthcheck.memory.allocation.warn", JobResultLevel.WARN);
        }
        return new JobResult("adminTools.dashboard.healthcheck.memory.allocation.info", JobResultLevel.INFO,
            maxMemory / 1024);
    }
}
