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

/**
 * Extension of {@link AbstractConfigurationHealthCheck} for checking the Java configuration.
 *
 * @version $Id$
 */
@Component
@Named(ConfigurationJavaHealthCheck.HINT)
@Singleton
public class ConfigurationJavaHealthCheck extends AbstractConfigurationHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "CONFIG_JAVA_HEALTH_CHECK";

    @Override
    public HealthCheckResult check()
    {
        String javaVersionString = getJson(ConfigurationDataProvider.HINT).get("javaVersion");
        if (javaVersionString == null) {
            logger.warn(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.java.warn.found"));
            return new HealthCheckResult("java_version_not_found", "java_installation_link");
        }
        String xwikiVersionString = getJson(ConfigurationDataProvider.HINT).get("xwikiVersion");
        float xwikiVersion = parseFloat(xwikiVersionString);
        float javaVersion = parseFloat(javaVersionString);
        if (isJavaXWikiCompatible(xwikiVersion, javaVersion)) {
            logger.warn(
                localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.java.warn.incompatible"));
            return new HealthCheckResult("java_xwiki_not_compatible", "java_issue_rec");
        }
        logger.info(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.java.info"));
        return new HealthCheckResult();
    }

    private static float parseFloat(String javaVersionString)
    {
        String[] parts = javaVersionString.split("\\.");
        return Float.parseFloat(parts[0] + "." + parts[1]);
    }

    private boolean isJavaXWikiCompatible(float xwikiVersion, float javaVersion)
    {
        boolean isCompatible = false;

        if (inInterval(xwikiVersion, 0, 6)) {
            isCompatible = javaVersion != 1.6;
        } else if (inInterval(xwikiVersion, 6, 8.1f)) {
            isCompatible = javaVersion != 1.7;
        } else if (inInterval(xwikiVersion, 8.1f, 11.3f)) {
            isCompatible = javaVersion != 1.8;
        } else if (inInterval(xwikiVersion, 11.2f, 14)) {
            isCompatible = (javaVersion != 1.8) || inInterval(javaVersion, 10.99f, 12);
        } else if (inInterval(xwikiVersion, 13.9f, 14.10f)) {
            isCompatible = javaVersion >= 11;
        } else if (inInterval(xwikiVersion, 14.10f, Float.MAX_VALUE)) {
            isCompatible = inInterval(javaVersion, 10.99f, 12) || inInterval(javaVersion, 16.99f, 18);
        }

        return isCompatible;
    }

    private boolean inInterval(float checkedValue, float lowerBound, float upperBound)
    {
        return checkedValue > lowerBound && checkedValue < upperBound;
    }
}
