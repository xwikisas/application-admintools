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

import javax.inject.Inject;
import javax.inject.Named;

import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;

/**
 * Common methods for {@link ServerInfo} classes.
 *
 * @version $Id$
 */
public abstract class AbstractServerInfo implements ServerInfo
{
    protected String[] serverCfgPossiblePaths;

    protected String[] xwikiCfgPossiblePaths;

    protected String[] xwikiInstallPossiblePaths;

    @Inject
    @Named("default")
    protected AdminToolsConfiguration adminToolsConfig;

    /**
     * The path to the server.
     */
    protected String serverPath;

    @Override
    public String getServerPath()
    {
        return serverPath;
    }

    @Override
    public String getServerCfgPath()
    {
        for (String serverCfgPath : this.serverCfgPossiblePaths) {
            if ((new File(serverCfgPath)).exists()) {
                return serverCfgPath;
            }
        }
        return null;
    }

    @Override
    public String getXwikiCfgFolderPath()
    {
        for (String xwikiCfgFolderPath : this.xwikiCfgPossiblePaths) {
            if ((new File(xwikiCfgFolderPath + "xwiki.cfg")).exists()) {
                return xwikiCfgFolderPath;
            }
        }
        return null;
    }

    @Override
    public String getXWikiInstallFolderPath()
    {
        for (String xwikiLibraryFolderPath : this.xwikiInstallPossiblePaths) {
            if ((new File(xwikiLibraryFolderPath)).isDirectory()) {
                return xwikiLibraryFolderPath;
            }
        }
        return null;
    }
}
