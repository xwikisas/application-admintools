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

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking the Java configuration.
 *
 * @version $Id$
 */
@Component
@Named(ConfigurationJavaHealthCheck.HINT)
@Singleton
public class ConfigurationJavaHealthCheck extends AbstractConfigurationHealthCheck implements Initializable
{
    /**
     * Component identifier.
     */
    public static final String HINT = "configurationJava";

    private NavigableMap<Version, Set<Integer>> supportedJavaVersions;

    private String xwikiVersionString;

    private String javaVersionString;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        this.xwikiVersionString = getXWikiVersion();
        this.javaVersionString = System.getProperty("java.specification.version");
        this.supportedJavaVersions = buildSupportedJavaVersions();
    }

    @Override
    public JobResult check()
    {
        if (StringUtils.isBlank(this.javaVersionString)) {
            this.logger.warn("Java version not found!");
            return new JobResult("adminTools.dashboard.healthcheck.java.warn", JobResultLevel.WARN);
        }

        if (!isJavaCompatible()) {
            this.logger.error("Java version is not compatible with the current XWiki installation!");
            return new JobResult("adminTools.dashboard.healthcheck.java.error", JobResultLevel.ERROR,
                this.javaVersionString, this.xwikiVersionString);
        }
        return new JobResult("adminTools.dashboard.healthcheck.java.info", JobResultLevel.INFO);
    }

    private String getXWikiVersion()
    {
        XWikiContext wikiContext = this.contextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        return wiki.getVersion();
    }

    private NavigableMap<Version, Set<Integer>> buildSupportedJavaVersions()
    {
        NavigableMap<Version, Set<Integer>> versions = new TreeMap<>();
        versions.put(new DefaultVersion("15.3"), Set.of(11, 17));
        versions.put(new DefaultVersion("16.0.0"), Set.of(17, 21));
        versions.put(new DefaultVersion("17.10.3"), Set.of(17, 21, 25));
        versions.put(new DefaultVersion("18.0.0"), Set.of(21, 25));
        return Collections.unmodifiableNavigableMap(versions);
    }

    /**
     * @return {@code true} if the given Java version is supported for the given XWiki version, or {@code false}
     *     otherwise
     */
    private boolean isJavaCompatible()
    {
        int javaMajorVersion = parseJavaMajorVersion();
        Map.Entry<Version, Set<Integer>> rule =
            this.supportedJavaVersions.floorEntry(new DefaultVersion(this.xwikiVersionString));
        return rule != null && rule.getValue().contains(javaMajorVersion);
    }

    /**
     * Extracts the Java major version from a raw string.
     *
     * @return the Java major version, for example {@code 8}, {@code 11}, {@code 17}, {@code 21}
     */
    private int parseJavaMajorVersion()
    {
        String[] parts = this.javaVersionString.split("[._-]");
        int majorIndex = "1".equals(parts[0]) && parts.length > 1 ? 1 : 0;
        return Integer.parseInt(parts[majorIndex].replaceAll("\\D", ""));
    }
}
