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
package com.xwiki.admintools.internal.health.checks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.ContextualLocalizationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;

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
    public static final String HINT = "CACHE_HEALTH_CHECK";

    @Inject
    protected ContextualLocalizationManager localization;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @Inject
    private Logger logger;

    @Override
    public HealthCheckResult check()
    {
        boolean cacheOk = isEnoughCache();
        boolean memoryOk = hasEnoughMemory();
        if (cacheOk && memoryOk) {
            return new HealthCheckResult();
        }
        return new HealthCheckResult("There are memory issues!", "dummy memory help link");
    }

    private boolean isEnoughCache()
    {
        String storeCacheCapacity = configurationSource.getProperty("xwiki.store.cache.capacity");
        if (storeCacheCapacity == null) {

            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.memory.cache.null.warn"));
            return false;
        }
        if (Integer.parseInt(storeCacheCapacity) <= 500) {
            logger.warn(localization.getTranslationPlain(
                "adminTools.dashboard.section.healthcheck.memory.cache.value.warn"), storeCacheCapacity);
            return false;
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.memory.cache.info"));
        return true;
    }

    private boolean hasEnoughMemory()
    {
        XWiki wiki = xcontextProvider.get().getWiki();
        float maxMemory = wiki.maxMemory();
        float totalFreeMemory = (maxMemory - (wiki.totalMemory() - wiki.freeMemory())) / (1024.0f * 1024);
        float maxMemoryGB = maxMemory / (1024.0f * 1024 * 1024);

        if (maxMemoryGB < 1) {
            logger.error(localization.getTranslationPlain(
                "adminTools.dashboard.section.healthcheck.memory.mem.maxcapacity.error"), maxMemoryGB * 1024);
            return false;
        }
        if (totalFreeMemory < 512) {
            logger.error(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.memory.mem.free.error"),
                totalFreeMemory);
            return false;
        } else if (totalFreeMemory < 1024) {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.memory.mem.free.warn"),
                totalFreeMemory);
            return true;
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.memory.mem.info"));
        return true;
    }
}
