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

import javax.inject.Singleton;
import org.xwiki.component.annotation.Component;

/**
 * Encapsulates functions used for retrieving configuration data.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = ConfigurationInfoUtil.class)
@Singleton
public class ConfigurationInfoUtil
{
    /**
     * Stores the path to the server.
     */
    private final String serverSystemPath;

    /**
     * Stores the possible paths for tomcat.
     */
    private final String[] tomcatPossiblePaths;

    /**
     * Stores the possible paths for the XWiki installation.
     */
    private final String[] xwikiPossiblePaths;

    /**
     * The class constructor.
     */
    public ConfigurationInfoUtil()
    {
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null) {
            serverSystemPath = catalinaBase;
        } else {
            serverSystemPath = System.getenv("CATALINA_HOME");
        }

        tomcatPossiblePaths = new String[] {
            String.format("%s/conf/server.xml", serverSystemPath),
            "/usr/local/tomcat/conf/server.xml",
            "/opt/tomcat/conf/server.xml",
            "/var/lib/tomcat8/conf/",
            "/var/lib/tomcat9/conf/",
            "/var/lib/tomcat/conf/" };

        xwikiPossiblePaths = new String[] {
            "/etc/xwiki/xwiki.cfg",
            String.format("%s/webapps${request.contextPath}/WEB-INF/xwiki.cfg", serverSystemPath),
            "/usr/local/xwiki/WEB-INF/xwiki.cfg",
            "/opt/xwiki/WEB-INF/xwiki.cfg",
            String.format("%s/webapps/ROOT/WEB-INF/xwiki.cfg", serverSystemPath),
            String.format("%s/webapps/xwiki/WEB-INF/xwiki.cfg", serverSystemPath) };
    }

    /**
     * Function used to retrieve the configuration info json.
     *
     * @return xwiki configuration info json.
     * @since 1.0
     */
    public Map<String, String> getSystemInfo()
    {
        Map<String, String> systemInfo = new HashMap<>();

        systemInfo.put("xwikiCfgPath", this.getXwikiCfgPath());
        systemInfo.put("tomcatConfPath", this.getTomcatConfPath());
        systemInfo.put("javaVersion", this.getJavaVersion());
        systemInfo.put("osInfo", this.getOSInfo());

        return systemInfo;
    }

    /**
     * Function used to retrieve the configuration file path for the XWiki.
     *
     * @return the XWiki configuration file path.
     * @since 1.0
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
     * Function used to retrieve the configuration file path for Tomcat.
     *
     * @return path to Tomcat configuration file.
     * @since 1.0
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
     * Function used to retrieve the used version of Java.
     *
     * @return the used Java version.
     * @since 1.0
     */
    private String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    /**
     * Function used to retrieve info about the OS XWiki is running on.
     *
     * @return info about the OS.
     * @since 1.0
     */
    private String getOSInfo()
    {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        return String.format("Operating System: %s %s (%s)", osName, osVersion, osArch);
    }
}
