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

import com.xwiki.admintools.health.HealthCheckResult;

/**
 * Extension of {@link AbstractSecurityHealthCheck} for checking system language configuration.
 *
 * @version $Id$
 */
@Component
@Named(LangEncodingHealthCheck.HINT)
@Singleton
public class LangEncodingHealthCheck extends AbstractSecurityHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "LANGUAGE_ENCODING_HEALTH_CHECK";

    private static final String WARN_LEVEL = "warn";

    @Override
    public HealthCheckResult check()
    {
        String langEnc = getJSON().get("LANG");
        if (langEnc == null) {
            logger.warn("Language encoding could not be detected!");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.security.system.lang.notFound", WARN_LEVEL);
        }
        boolean isSafeLangEnc = isSafeEncoding(langEnc.split("\\.")[1], "System language");
        if (!isSafeLangEnc) {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.security.system.lang.warn",
                "adminTools.dashboard.healthcheck.security.system.recommendation", WARN_LEVEL, langEnc);
        }
        return new HealthCheckResult("adminTools.dashboard.healthcheck.security.system.lang.info", "info");
    }
}
