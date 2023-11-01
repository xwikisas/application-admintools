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

@Component
@Named(SecurityXWikiEncodingHealthCheck.HINT)
@Singleton
public class SecurityXWikiEncodingHealthCheck extends AbstractConfigurationHealthCheck
{
    public final static String HINT = "SECURITY_XWIKI_ENCODING_HEALTH_CHECK";

    private final List<String> acceptedEncodings = new ArrayList<>(List.of("UTF8", "UTF-8", "utf8", "utf-8"));

    @Override
    public HealthCheckResult check()
    {
        Map<String, String> securityJson = getJson(SecurityDataProvider.HINT);
        String activeEnc = securityJson.get("activeEncoding");
        String configEnc = securityJson.get("configurationEncoding");
        if (!acceptedEncodings.contains(activeEnc) || !acceptedEncodings.contains(configEnc)) {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.security.xwiki.warn"),
                activeEnc);
            return new HealthCheckResult("xwiki_encoding err", "xwiki config tutorial link");
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.security.xwiki.info"));
        return new HealthCheckResult();
    }
}
