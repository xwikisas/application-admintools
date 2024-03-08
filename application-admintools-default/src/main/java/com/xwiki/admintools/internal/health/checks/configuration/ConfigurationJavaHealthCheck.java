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
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.health.HealthCheckResultLevel;
import com.xwiki.admintools.health.XWikiVersions;

/**
 * Implementation of {@link HealthCheck} for checking the Java configuration.
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
    public static final String HINT = "configurationJava";

    private static final String REGEX = "\\.";

    @Override
    public HealthCheckResult check()
    {
        Map<String, String> configurationJson = getConfigurationProviderJSON();
        String javaVersionString = configurationJson.get("javaVersion");
        if (javaVersionString == null) {
            logger.warn("Java version not found!");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.java.warn", HealthCheckResultLevel.WARN);
        }
        String xwikiVersionString = configurationJson.get("xwikiVersion");
        float javaVersion = parseJavaVersionFloat(javaVersionString);
        if (!isJavaXWikiCompatible(xwikiVersionString, javaVersion)) {
            logger.error("Java version is not compatible with the current XWiki installation!");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.java.error", HealthCheckResultLevel.ERROR,
                javaVersionString, xwikiVersionString);
        }
        return new HealthCheckResult("adminTools.dashboard.healthcheck.java.info", HealthCheckResultLevel.INFO);
    }

    private float parseJavaVersionFloat(String javaVersionString)
    {
        String[] parts = javaVersionString.split(REGEX);
        return Float.parseFloat(parts[0] + "." + parts[1]);
    }

    private boolean isJavaXWikiCompatible(String xwikiVersion, float javaVersion)
    {
        boolean isCompatible = false;
        if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_8_1.getVersion(), XWikiVersions.XWIKI_11_3.getVersion())) {
            isCompatible = javaVersion == 1.8f;
        } else if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_11_3.getVersion(),
            XWikiVersions.XWIKI_14_0.getVersion()))
        {
            isCompatible = (javaVersion == 1.8f) || isJavaCompatible(javaVersion, 10.99f, 12.0f);
        } else if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_14_0.getVersion(),
            XWikiVersions.XWIKI_14_10_9.getVersion()))
        {
            isCompatible = isJavaCompatible(javaVersion, 10.99f, 12.0f);
        } else if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_14_10_9.getVersion(),
            XWikiVersions.XWIKI_15_3.getVersion()))
        {
            isCompatible = isJavaCompatible(javaVersion, 16.99f, 18.0f) || isJavaCompatible(javaVersion, 10.99f, 12.0f);
        } else if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_15_3.getVersion(),
            XWikiVersions.XWIKI_16_0.getVersion()))
        {
            isCompatible = isJavaCompatible(javaVersion, 10.99f, 12f) || isJavaCompatible(javaVersion, 16.99f, 18f);
        } else if (isInInterval(xwikiVersion, XWikiVersions.XWIKI_16_0.getVersion(),
            XWikiVersions.XWIKI_17_0.getVersion()))
        {
            isCompatible = isJavaCompatible(javaVersion, 16.99f, 22f);
        }

        return isCompatible;
    }

    private boolean isJavaCompatible(float checkedValue, float lowerBound, float upperBound)
    {
        return checkedValue > lowerBound && checkedValue < upperBound;
    }

    private boolean isInInterval(String checkedValue, String lowerBound, String upperBound)
    {
        Version checkedVersion = new DefaultVersion(checkedValue);
        Version lowerBoundVersion = new DefaultVersion(lowerBound);
        Version upperBoundVersion = new DefaultVersion(upperBound);

        return checkedVersion.compareTo(lowerBoundVersion) >= 0 && checkedVersion.compareTo(upperBoundVersion) < 0;
    }
}
