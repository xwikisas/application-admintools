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
@Named(SecuritySystemEncodingHealthCheck.HINT)
@Singleton
public class SecuritySystemEncodingHealthCheck extends AbstractConfigurationHealthCheck
{
    public final static String HINT = "SECURITY_SYSTEM_ENCODING_HEALTH_CHECK";

    private final List<String> acceptedEncodings = new ArrayList<>(List.of("UTF8", "UTF-8", "utf8", "utf-8"));

    @Override
    public HealthCheckResult check()
    {
        Map<String, String> securityJson = getJson(SecurityDataProvider.HINT);
        if (!acceptedEncodings.contains(securityJson.get("LANG").split("\\.")[1])
            || !acceptedEncodings.contains(securityJson.get("fileEncoding")))
        {
            logger.warn("System encoding should be UTF-8!");
            return new HealthCheckResult("xwiki_encoding err", "xwiki config tutorial link");
        }
        logger.info("System encoding is safe.");
        return new HealthCheckResult();
    }
}
