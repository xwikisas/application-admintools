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
package com.xwiki.admintools;

import org.xwiki.component.annotation.Role;

/**
 * Exposes methods for accessing server specific information, like configurations, logs or other XWiki and server
 * files.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface ServerIdentifier
{
    /**
     * Verify if a specific server is used. If a server path is provided in the XWiki configurations, it verifies if the
     * path corresponds to a server. Otherwise, it searches the server location in system properties and system
     * environment.
     *
     * @param providedConfigServerPath {@link String} server path provided in the XWiki configuration page.
     * @return {@link Boolean} true if the server is used, false otherwise.
     */
    boolean isUsed(String providedConfigServerPath);

    /**
     * Extract the hint of a component.
     *
     * @return {@link String} component hint.
     */
    String getIdentifier();

    /**
     * Access the path to the server configuration file.
     *
     * @return {@link String} representing the path to the server configuration file.
     */
    String getServerCfgPath();

    /**
     * Access the path to the XWiki configuration folder.
     *
     * @return {@link String} representing the path to the XWiki configuration folder.
     */
    String getXwikiCfgFolderPath();

    /**
     * Update the possible paths to the configuration files.
     *
     * @param providedConfigServerPath {@link String} the server path provided in the XWiki configuration page.
     */
    void updatePaths(String providedConfigServerPath);
}
