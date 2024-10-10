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
package com.xwiki.admintools.internal.health.checks.security;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.CustomJobResult;
import com.xwiki.admintools.jobs.CustomJobResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking XWiki configuration encoding.
 *
 * @version $Id$
 */
@Component
@Named(ConfigurationEncodingHealthCheck.HINT)
@Singleton
public class ConfigurationEncodingHealthCheck extends AbstractSecurityHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "configurationEncoding";

    @Override
    public CustomJobResult check()
    {
        String configEnc = getSecurityProviderJSON().get(HINT);
        if (configEnc == null) {
            logger.warn("Configuration encoding could not be detected!");
            return new CustomJobResult("adminTools.dashboard.healthcheck.security.xwiki.config.notFound",
                CustomJobResultLevel.WARN);
        }
        boolean isConfigEncSafe = isSafeEncoding(configEnc, "XWiki configuration");

        if (!isConfigEncSafe) {
            return new CustomJobResult("adminTools.dashboard.healthcheck.security.xwiki.config.warn",
                CustomJobResultLevel.WARN, configEnc);
        }
        return new CustomJobResult("adminTools.dashboard.healthcheck.security.xwiki.config.info",
            CustomJobResultLevel.INFO);
    }
}
