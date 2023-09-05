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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Encapsulates functions used for identifying a Tomcat server and retrieving it's info.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(TomcatIdentifier.HINT)
@Singleton
public class TomcatIdentifier extends AbstractServerIdentifier
{
    /**
     * Component identifier.
     */
    public static final String HINT = "tomcat";

    /**
     * Verify if this server is the one that stores the XWiki instance.
     *
     * @return true if this server is used, false otherwise
     */
    @Override
    public boolean isUsed(String providedConfigServerPath)
    {
        if (tomcatIsUsed(providedConfigServerPath)) {
            updatePaths(providedConfigServerPath);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public void updatePaths(String providedConfigServerPath)
    {
        if (tomcatIsUsed(providedConfigServerPath)) {
            this.serverCfgPossiblePaths = new String[] { String.format("%s/conf/server.xml", this.serverPath),
                "/usr/local/tomcat/conf/server.xml", "/opt/tomcat/conf/server.xml", "/var/lib/tomcat8/conf/",
                "/var/lib/tomcat9/conf/", "/var/lib/tomcat/conf/" };

            this.xwikiCfgPossiblePaths = new String[] { "/etc/xwiki/xwiki.cfg",
                String.format("%s/webapps${request.contextPath}/WEB-INF/xwiki.cfg", this.serverPath),
                "/usr/local/xwiki/WEB-INF/xwiki.cfg", "/opt/xwiki/WEB-INF/xwiki.cfg",
                String.format("%s/webapps/ROOT/WEB-INF/xwiki.cfg", this.serverPath),
                String.format("%s/webapps/xwiki/WEB-INF/xwiki.cfg", this.serverPath) };
        }
    }

    /**
     * Function used to verify if a Tomcat server is used. If a server path is provided in the XWiki
     * configurations, it verifies if the path corresponds to a Tomcat server. Otherwise, it searches the Catalina
     * location in system properties and system environment.
     *
     * @param providedConfigServerPath the server path provided in the XWiki configuration page.
     * @return true if Tomcat is the used server, false otherwise.
     */
    private boolean tomcatIsUsed(String providedConfigServerPath)
    {
        if (providedConfigServerPath != null) {
            File file = new File(providedConfigServerPath + "conf/catalina.properties");
            if (file.exists()) {
                this.serverPath = providedConfigServerPath;
                return true;
            }
        }
        String catalinaBase = System.getProperty("catalina.base");
        String catalinaHome = System.getenv("CATALINA_HOME");

        if (catalinaBase != null) {
            this.serverPath = catalinaBase;
        } else if (catalinaHome != null) {
            this.serverPath = catalinaHome;
        } else {
            this.serverPath = null;
            return false;
        }
        return true;
    }
}