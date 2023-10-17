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

import com.xwiki.admintools.ServerIdentifier;

/**
 * {@link ServerIdentifier} implementation used for identifying a Tomcat server and retrieving it's info.
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
    public static final String HINT = "Tomcat";

    @Override
    public boolean isUsed()
    {
        this.serverPath = null;
        String providedConfigServerPath = this.adminToolsConfig.getServerPath();
        if (providedConfigServerPath != null && !providedConfigServerPath.equals("")) {
            return checkAndSetServerPath(providedConfigServerPath);
        } else {
            String catalinaBase = System.getProperty("catalina.base");
            String catalinaHome = System.getenv("CATALINA_HOME");
            if (catalinaBase != null) {
                return checkAndSetServerPath(catalinaBase);
            } else if (catalinaHome != null) {
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
        this.serverCfgPossiblePaths =
            new String[] { String.format("%s/conf/server.xml", this.serverPath), "/usr/local/tomcat/conf/server.xml",
                "/opt/tomcat/conf/server.xml", "/var/lib/tomcat8/conf/server.xml", "/var/lib/tomcat9/conf/server.xml",
                "/var/lib/tomcat/conf/server.xml" };

        this.xwikiCfgPossiblePaths = new String[] { "/etc/xwiki/", "/usr/local/xwiki/WEB-INF/", "/opt/xwiki/WEB-INF/",
            String.format("%s/webapps/ROOT/WEB-INF/", this.serverPath),
            String.format("%s/webapps/xwiki/WEB-INF/", this.serverPath) };
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
