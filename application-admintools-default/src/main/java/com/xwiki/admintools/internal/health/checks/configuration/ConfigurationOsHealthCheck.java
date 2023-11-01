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
package com.xwiki.admintools.internal.health.checks.configuration;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;

@Component
@Named(ConfigurationOsHealthCheck.HINT)
@Singleton
public class ConfigurationOsHealthCheck extends AbstractConfigurationHealthCheck
{
    public final static String HINT = "CONFIG_OS_HEALTH_CHECK";

    @Override
    public HealthCheckResult check()
    {
        if (getJson(ConfigurationDataProvider.HINT).get("osName") == null
            || getJson(ConfigurationDataProvider.HINT).get("osVersion") == null
            || getJson(ConfigurationDataProvider.HINT).get("osArch") == null)
        {
            logger.warn(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.os.warn"));
            return new HealthCheckResult("os_issue", "os_support");
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.os.info"));
        return new HealthCheckResult();
    }
}
