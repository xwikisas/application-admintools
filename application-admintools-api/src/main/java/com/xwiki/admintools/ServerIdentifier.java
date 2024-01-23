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

import java.util.regex.Pattern;

import org.xwiki.component.annotation.Role;

/**
 * Exposes methods for accessing server specific information, like configurations, logs or other XWiki and server
 * files.
 *
 * @version $Id$
 */
@Role
public interface ServerIdentifier
{
    /**
     * Verify if a specific server is used. If a server path is provided in the XWiki configurations, it verifies if the
     * path corresponds to a server. Otherwise, it searches the server location in system properties and system
     * environment.
     *
     * @return {@code true} if the server is used, {@code false} otherwise.
     */
    boolean isUsed();

    /**
     * Extract the hint of a component.
     *
     * @return component hint.
     */
    String getComponentHint();

    /**
     * Access the path to the server configuration file.
     *
     * @return the path to the server configuration file.
     */
    String getServerCfgPath();

    /**
     * Access the path to the XWiki configuration folder.
     *
     * @return the path to the XWiki configuration folder.
     */
    String getXwikiCfgFolderPath();

    /**
     * Update the possible paths to the configuration files.
     */
    void updatePossiblePaths();

    /**
     * Get path to server.
     *
     * @return {@link String} with server path.
     */
    String getServerPath();

    /**
     * Get path to server logs folder.
     *
     * @return {@link String} with server logs folder path.
     */
    String getLogsFolderPath();

    /**
     * Get path to server last updated main log file.
     *
     * @return {@link String} path to server log file.
     */
    String getLastLogFilePath();

    /**
     * Get server pattern for identifying log files date.
     *
     * @return {@link Pattern} representing the date format in server log files names.
     */
    Pattern getLogsPattern();
}
