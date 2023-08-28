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
package com.xwiki.admintools.internal.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;

import com.xwiki.admintools.configuration.AdminToolsConfiguration;

/**
 * Encapsulates functions used for retrieving configuration data.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(ConfigurationDataProvider.HINT)
@Singleton
public class ConfigurationDataProvider extends AbstractDataProvider
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "configuration";

    /**
     * Stores the path to the server.
     */
    private String serverSystemPath;

    /**
     * Stores the possible paths for tomcat.
     */
    private String[] tomcatPossiblePaths;

    /**
     * Stores the possible paths for the XWiki installation.
     */
    private String[] xwikiPossiblePaths;

    /**
     * Provides the XWiki configuration data.
     */
    @Inject
    private Provider<AdminToolsConfiguration> adminToolsConfig;

    /**
     * Initialize the component.
     *
     * @throws InitializationException
     */
    public void initialize() throws InitializationException
    {
        updatePaths();
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    /**
     * Get the configuration info json.
     *
     * @return xwiki configuration info json.
     */
    public String provideData()
    {
        updatePaths();
        Map<String, String> systemInfo = new HashMap<>();

        systemInfo.put("xwikiCfgPath", this.getXwikiCfgPath());
        systemInfo.put("tomcatConfPath", this.getTomcatConfPath());
        systemInfo.put("javaVersion", this.getJavaVersion());
        systemInfo.put("osInfo", this.getOSInfo());

        return templateGenerator(systemInfo, "data/configurationTemplate.vm", HINT);
    }

    /**
     * Function used to update the possible paths to the xwiki and tomcat installations.
     */
    private void updatePaths()
    {
        String providedConfigServerPath = adminToolsConfig.get().getServerPath();
        if (providedConfigServerPath != null) {
            this.serverSystemPath = providedConfigServerPath;
        } else {
            String catalinaBase = System.getProperty("catalina.base");
            if (catalinaBase != null) {
                this.serverSystemPath = catalinaBase;
            } else {
                this.serverSystemPath = System.getenv("CATALINA_HOME");
            }
        }

        this.tomcatPossiblePaths = new String[] { String.format("%s/conf/server.xml", this.serverSystemPath),
            "/usr/local/tomcat/conf/server.xml", "/opt/tomcat/conf/server.xml", "/var/lib/tomcat8/conf/",
            "/var/lib/tomcat9/conf/", "/var/lib/tomcat/conf/" };

        this.xwikiPossiblePaths = new String[] { "/etc/xwiki/xwiki.cfg",
            String.format("%s/webapps${request.contextPath}/WEB-INF/xwiki.cfg", this.serverSystemPath),
            "/usr/local/xwiki/WEB-INF/xwiki.cfg", "/opt/xwiki/WEB-INF/xwiki.cfg",
            String.format("%s/webapps/ROOT/WEB-INF/xwiki.cfg", this.serverSystemPath),
            String.format("%s/webapps/xwiki/WEB-INF/xwiki.cfg", this.serverSystemPath) };
    }

    /**
     * Get the configuration file path for the XWiki.
     */
    private String getXwikiCfgPath()
    {
        for (String xwCfgPath : xwikiPossiblePaths) {
            if ((new File(xwCfgPath)).exists()) {
                return xwCfgPath;
            }
        }
        return null;
    }

    /**
     * Get the configuration file path for Tomcat.
     *
     * @return path to Tomcat configuration file.
     */
    private String getTomcatConfPath()
    {
        for (String tomConfPath : tomcatPossiblePaths) {
            if ((new File(tomConfPath)).exists()) {
                return tomConfPath;
            }
        }
        return null;
    }

    /**
     * Get the used version of Java.
     *
     * @return the used Java version.
     */
    private String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    /**
     * Get info about the OS XWiki is running on.
     *
     * @return info about the OS.
     */
    private String getOSInfo()
    {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        return String.format("Operating System: %s %s (%s)", osName, osVersion, osArch);
    }
}
