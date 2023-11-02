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

import java.io.File;
import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Implementation of {@link HealthCheck} for checking system hardware meets XWiki requirements.
 *
 * @version $Id$
 */
@Component
@Named(PerformanceHealthCheck.HINT)
@Singleton
public class PerformanceHealthCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "PERFORMANCE_HEALTH_CHECK";

    @Inject
    protected ContextualLocalizationManager localization;

    @Inject
    private Logger logger;

    @Override
    public HealthCheckResult check()
    {
        boolean hasFreeSpace = hasFreeSpace();
        boolean hasMinimumCPURequirements = hasMinimumCPURequirements();
        boolean hasMinimumMemoryRequirements = hasMinimumMemoryRequirements();
        if (!hasFreeSpace || !hasMinimumCPURequirements || !hasMinimumMemoryRequirements) {
            return new HealthCheckResult("performance issues", "minimum sys req link");
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.performance.info"));
        return new HealthCheckResult();
    }

    private boolean hasFreeSpace()
    {
        File diskPartition;
        if (System.getProperty("os.name").contains("Windows")) {
            diskPartition = new File("C:");
        } else {
            diskPartition = new File("/");
        }
        long freePartitionSpace = diskPartition.getFreeSpace();
        float freeSpace = (float) freePartitionSpace / (1024 * 1024 * 1024);

        if (freeSpace > 2) {
            return true;
        } else {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.performance.space.warn"));
            return false;
        }
    }

    private boolean hasMinimumMemoryRequirements()
    {
        long memorySize =
            ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalPhysicalMemorySize();
        float totalMemory = (float) memorySize / (1024 * 1024 * 1024) + 1;

        if (totalMemory > 2) {
            return true;
        } else {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.performance.memory.warn"));
            return false;
        }
    }

    private boolean hasMinimumCPURequirements()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        int cpuCores = processor.getPhysicalProcessorCount();
        long maxFreq = processor.getMaxFreq() / (1024 * 1024);

        if (cpuCores > 2 && maxFreq > 2048) {
            return true;
        } else {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.performance.cpu.warn"));
            return false;
        }
    }
}
