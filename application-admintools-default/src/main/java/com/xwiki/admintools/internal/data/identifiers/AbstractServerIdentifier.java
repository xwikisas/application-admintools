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

import com.xwiki.admintools.ServerIdentifier;

/**
 * Common methods for {@link ServerIdentifier} classes.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractServerIdentifier implements ServerIdentifier
{
    protected String[] serverCfgPossiblePaths;

    protected String[] xwikiCfgPossiblePaths;

    /**
     * The path to the server.
     */
    protected String serverPath;

    @Override
    public String getServerPath()
    {
        return serverPath;
    }

    /**
     * Get the server configuration file path.
     *
     * @return {@link String} path to Tomcat configuration file.
     */
    @Override
    public String getServerCfgPath()
    {
        for (String serverCfgPath : serverCfgPossiblePaths) {
            if ((new File(serverCfgPath)).exists()) {
                return serverCfgPath;
            }
        }
        return null;
    }

    /**
     * Get the configuration file path for the XWiki installation.
     *
     * @return {@link String} path to the XWiki configuration file.
     */
    @Override
    public String getXwikiCfgFolderPath()
    {
        for (String xwikiCfgPath : xwikiCfgPossiblePaths) {
            if ((new File(xwikiCfgPath + "xwiki.cfg")).exists()) {
                return xwikiCfgPath;
            }
        }
        return null;
    }
}
