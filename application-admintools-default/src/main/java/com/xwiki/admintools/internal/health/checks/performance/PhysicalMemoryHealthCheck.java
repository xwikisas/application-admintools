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

import java.text.DecimalFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

/**
 * Implementation of {@link HealthCheck} for checking if system memory meets XWiki requirements.
 *
 * @version $Id$
 */
@Component
@Named(PhysicalMemoryHealthCheck.HINT)
@Singleton
public class PhysicalMemoryHealthCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "physicalMemory";

    @Inject
    private Logger logger;

    @Override
    public JobResult check()
    {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();

        float totalMemory = (float) hardware.getMemory().getTotal() / (1024 * 1024 * 1024);
        DecimalFormat format = new DecimalFormat("0.#");
        if (totalMemory > 2) {
            return new JobResult("adminTools.dashboard.healthcheck.performance.memory.info",
                JobResultLevel.INFO);
        }
        String systemCapacityMessage = format.format(totalMemory);
        logger.warn("There is not enough memory to safely run the XWiki installation! Physical memory detected: [{}]",
            systemCapacityMessage);
        return new JobResult("adminTools.dashboard.healthcheck.performance.memory.warn",
            JobResultLevel.WARN, systemCapacityMessage);
    }
}
