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

import java.text.DecimalFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.health.HealthCheckResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking instance memory performance.
 *
 * @version $Id$
 */
@Component
@Named(MemoryHealthCheck.HINT)
@Singleton
public class MemoryHealthCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "MEMORY_HEALTH_CHECK";

    private static final String MB_UNIT = "MB";

    private static final String ERROR_LEVEL = "error";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    @Override
    public HealthCheckResult check()
    {
        XWiki wiki = xcontextProvider.get().getWiki();
        float maxMemory = wiki.maxMemory();
        float totalFreeMemory = (maxMemory - (wiki.totalMemory() - wiki.freeMemory())) / (1024.0f * 1024);
        float maxMemoryGB = maxMemory / (1024.0f * 1024 * 1024);
        DecimalFormat format = new DecimalFormat("0.#");
        if (maxMemoryGB < 1) {
            logger.error("JVM memory is less than 1024MB. Currently: [{}]", maxMemoryGB * 1024);
            return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.maxcapacity.error",
                HealthCheckResultLevel.ERROR, (maxMemoryGB * 1024f));
        }
        if (totalFreeMemory < 512) {
            logger.error("JVM instance has only [{}]MB free memory left!", totalFreeMemory);
            return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.free.error",
                HealthCheckResultLevel.ERROR, totalFreeMemory);
        } else if (totalFreeMemory < 1024) {
            logger.warn("Instance memory is running low. Currently only [{}]MB free left.", totalFreeMemory);
            return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.free.warn",
                HealthCheckResultLevel.WARN, totalFreeMemory);
        }
        return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.info", HealthCheckResultLevel.INFO,
            totalFreeMemory / 1024);
    }
}
