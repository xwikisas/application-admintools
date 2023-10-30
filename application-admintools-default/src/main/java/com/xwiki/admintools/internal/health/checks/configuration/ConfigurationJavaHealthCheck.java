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
@Named(ConfigurationJavaHealthCheck.HINT)
@Singleton
public class ConfigurationJavaHealthCheck extends AbstractConfigurationHealthCheck
{
    public final static String HINT = "CONFIG_JAVA_HEALTH_CHECK";

    private static final String xwikiJavaCompatibilityLink =
        "https://dev.xwiki.org/xwiki/bin/view/Community/SupportStrategy/JavaSupportStrategy/";

    /**
     * @return
     */
    @Override
    public HealthCheckResult check()
    {
        String javaVersionString = getJson(ConfigurationDataProvider.HINT).get("javaVersion");
        if (javaVersionString == null) {
            logger.warn("Java version not found!");
            return new HealthCheckResult("java_version_not_found", "java_installation_link");
        }
        String xwikiVersionString = getJson(ConfigurationDataProvider.HINT).get("xwikiVersion");
        float xwikiVersion = parseFloat(xwikiVersionString);
        float javaVersion = parseFloat(javaVersionString);
        if (isJavaXWikiCompatible(xwikiVersion, javaVersion)) {
            logger.warn("Java version is not compatible with the current XWiki installation!");
            return new HealthCheckResult("java_xwiki_comp", xwikiJavaCompatibilityLink);
        }
        logger.info("Java status OK.");
        return new HealthCheckResult();
    }

    private static float parseFloat(String javaVersionString)
    {
        String[] parts = javaVersionString.split("\\.");
        return Float.parseFloat(parts[0] + "." + parts[1]);
    }

    private boolean isJavaXWikiCompatible(float xwikiVersion, float javaVersion)
    {
        return inInterval(xwikiVersion, 0, 6) && javaVersion != 1.6
            || inInterval(xwikiVersion, 6, 8.1f) && javaVersion != 1.7
            || inInterval(xwikiVersion, 8.1f, 11.3f) && javaVersion != 1.8 || inInterval(xwikiVersion, 11.2f, 14) && (
            javaVersion != 1.8 || inInterval(javaVersion, 10.99f, 12))
            || inInterval(xwikiVersion, 13.9f, 14.10f) && javaVersion >= 11
            || inInterval(xwikiVersion, 14.10f, Float.MAX_VALUE) && (inInterval(javaVersion, 10.99f, 12) || inInterval(
            javaVersion, 16.99f, 18));
    }

    private boolean inInterval(float checkedValue, float lowerBound, float upperBound)
    {
        return checkedValue > lowerBound && checkedValue < upperBound;
    }
}
