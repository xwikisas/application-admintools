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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheckResult;

/**
 * Extension of {@link AbstractConfigurationHealthCheck} for checking the OS configuration.
 *
 * @version $Id$
 */
@Component
@Named(ConfigurationOSHealthCheck.HINT)
@Singleton
public class ConfigurationOSHealthCheck extends AbstractConfigurationHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "configurationOS";

    @Override
    public HealthCheckResult check()
    {
        Map<String, String> dataJSON = getConfigurationProviderJSON();
        if (dataJSON.get("osName") == null || dataJSON.get("osVersion") == null || dataJSON.get("osArch") == null) {
            logger.warn("There has been an error while gathering OS info!");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.os.warn", "warn");
        }
        return new HealthCheckResult("adminTools.dashboard.healthcheck.os.info", "info");
    }
}
