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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.health.HealthCheckResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking instance document cache performance.
 *
 * @version $Id$
 */
@Component
@Named(CacheMemoryHealthCheck.HINT)
@Singleton
public class CacheMemoryHealthCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "CACHE_MEMORY_HEALTH_CHECK";

    private static final String INFO_LEVEL = "info";

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @Inject
    private Logger logger;

    @Override
    public HealthCheckResult check()
    {
        String storeCacheCapacity = configurationSource.getProperty("xwiki.store.cache.capacity");
        if (storeCacheCapacity == null) {
            logger.warn("Store cache capacity not defined. Set by default at 500.");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.cache.null",
                HealthCheckResultLevel.INFO);
        }
        int cacheCapacity = Integer.parseInt(storeCacheCapacity);
        if (cacheCapacity < 500) {
            logger.warn("Store cache capacity is set to [{}].", storeCacheCapacity);
            return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.cache.low",
                HealthCheckResultLevel.WARN, cacheCapacity);
        }
        return new HealthCheckResult("adminTools.dashboard.healthcheck.memory.cache.info",
            HealthCheckResultLevel.INFO, cacheCapacity);
    }
}
