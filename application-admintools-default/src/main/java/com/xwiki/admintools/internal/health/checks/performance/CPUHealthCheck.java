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
package com.xwiki.admintools.internal.health.checks.performance;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Implementation of {@link HealthCheck} for checking if system CPU meets XWiki requirements.
 *
 * @version $Id$
 */
@Component
@Named(CPUHealthCheck.HINT)
@Singleton
public class CPUHealthCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "cpuPerformance";

    @Inject
    private Logger logger;

    @Override
    public HealthCheckResult check()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();
        int cpuCores = processor.getPhysicalProcessorCount();
        long maxFreq = processor.getMaxFreq() / (1024 * 1024);

        if (cpuCores > 2 && maxFreq > 2048) {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.performance.cpu.info", "info");
        }
        String cpuSpecifications = String.format("CPU cores %d - frequency %d", cpuCores, maxFreq);
        logger.warn("The CPU does not satisfy the minimum system requirements! [{}]", cpuSpecifications);
        return new HealthCheckResult("adminTools.dashboard.healthcheck.performance.cpu.warn",
            "adminTools.dashboard.healthcheck.performance.cpu.recommendation", "error", cpuSpecifications);
    }
}
