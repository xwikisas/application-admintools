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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.internal.data.SecurityDataProvider;

/**
 * Extension of {@link AbstractConfigurationHealthCheck} for checking system security configuration.
 *
 * @version $Id$
 */
@Component
@Named(SecurityEncodingHealthCheck.HINT)
@Singleton
public class SecurityEncodingHealthCheck extends AbstractConfigurationHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "SECURITY_SYSTEM_ENCODING_HEALTH_CHECK";

    private static final String INVALID = "INVALID";

    private final List<String> acceptedEncodings = new ArrayList<>(List.of("UTF8", "UTF-8", "utf8", "utf-8"));

    @Override
    public HealthCheckResult check()
    {
        Map<String, String> securityJson = getJSON(SecurityDataProvider.HINT);
        String activeEnc = securityJson.getOrDefault("activeEncoding", INVALID);
        String configEnc = securityJson.getOrDefault("configurationEncoding", INVALID);
        String langEnc = securityJson.getOrDefault("LANG", "invalid.invalid").split("\\.")[1];
        String fileEnc = securityJson.getOrDefault("fileEncoding", INVALID);

        boolean isSafeLangEnc = isSafeEncoding(langEnc, "adminTools.dashboard.healthcheck.security.system.lang.warn");
        boolean isSafeFileEnc = isSafeEncoding(fileEnc, "adminTools.dashboard.healthcheck.security.system.file.warn");
        boolean isActiveEncSafe =
            isSafeEncoding(activeEnc, "adminTools.dashboard.healthcheck.security.xwiki.active.warn");
        boolean isConfigEncSafe =
            isSafeEncoding(configEnc, "adminTools.dashboard.healthcheck.security.xwiki.config.warn");

        if (!isSafeLangEnc || !isSafeFileEnc || !isActiveEncSafe || !isConfigEncSafe) {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.security.xwiki.config.warn", "xwiki config"
                + " tutorial link", "warn");
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.healthcheck.security.system.info"));
        return new HealthCheckResult("adminTools.dashboard.healthcheck.security.xwiki.config.warn", "xwiki config"
            + " tutorial link", "warn");
    }

    private boolean isSafeEncoding(String encoding, String message)
    {
        if (acceptedEncodings.contains(encoding)) {
            return true;
        }
        logger.warn(localization.getTranslationPlain(message), encoding);
        return false;
    }
}
