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
package com.xwiki.admintools.internal.data.identifiers;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.ServerInfo;

/**
 * {@link ServerInfo} implementation used for identifying a Tomcat server and retrieving its info.
 *
 * @version $Id$
 */
@Component
@Named(TomcatInfo.HINT)
@Singleton
public class TomcatInfo extends AbstractServerInfo
{
    /**
     * Component identifier.
     */
    public static final String HINT = "tomcat";

    private static final String ETC_XWIKI_INSTALL = "/etc/xwiki/";

    @Override
    public boolean foundServerPath()
    {
        this.serverPath = null;
        String providedConfigServerPath = this.adminToolsConfig.getServerPath();
        if (providedConfigServerPath != null && !providedConfigServerPath.isEmpty()) {
            return checkAndSetServerPath(providedConfigServerPath);
        } else {
            String catalinaBase = System.getProperty("catalina.base");
            String catalinaHome = System.getenv("CATALINA_HOME");
            if (catalinaBase != null && !catalinaBase.isEmpty()) {
                return checkAndSetServerPath(catalinaBase);
            } else if (catalinaHome != null && !catalinaHome.isEmpty()) {
                return checkAndSetServerPath(catalinaHome);
            }
        }
        return false;
    }

    @Override
    public String getComponentHint()
    {
        return HINT;
    }

    @Override
    public void updatePossiblePaths()
    {
        this.serverCfgPossiblePaths = List.of(
            String.format("%s/conf/server.xml", this.serverPath),
            "/usr/local/tomcat/conf/server.xml", "/opt/tomcat/conf/server.xml", "/var/lib/tomcat8/conf/server.xml",
            "/var/lib/tomcat9/conf/server.xml", "/var/lib/tomcat/conf/server.xml");

        this.xwikiCfgPossiblePaths = List.of(
            String.format("%s/webapps/xwiki/WEB-INF/", this.serverPath),
            String.format("%s/webapps/ROOT/WEB-INF/", this.serverPath),
            ETC_XWIKI_INSTALL, "/usr/local/xwiki/WEB-INF/", "/opt/xwiki/WEB-INF/");

        this.xwikiInstallPossiblePaths = List.of(
            String.format("%s/webapps/xwiki/", this.serverPath),
            ETC_XWIKI_INSTALL, "/usr/local/xwiki/", "/opt/xwiki/");
    }

    @Override
    public String getLogsFolderPath()
    {
        return this.serverPath + "/logs";
    }

    @Override
    public String getLastLogFilePath()
    {
        return this.serverPath + "/logs/catalina.out";
    }

    @Override
    public String getLogsHint()
    {
        return "catalina";
    }

    @Override
    public Pattern getLogsPattern()
    {
        return Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    }

    private boolean checkAndSetServerPath(String path)
    {
        File file = new File(path + "/conf/catalina.properties");
        if (file.exists()) {
            this.serverPath = path;
            return true;
        }
        return false;
    }
}
