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

import java.io.IOException;
import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Provides functions for logs retrieval.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface FilesResourceProvider
{
    /**
     * Generates the logs archive from a specified path that are to be downloaded after applying the filters.
     *
     * @param filter {@link Map} representing the filters that can be applied to the search.
     * @param path {@link String} path to where the logs are stored.
     * @return {@link Byte} array representing the logs archive.
     */
    byte[] generateLogsArchive(Map<String, String> filter, String path);

    /**
     * Extract the hint of a component.
     *
     * @return {@link String} component hint.
     */
    String getIdentifier();

    /**
     * Retreive the last "noLines" logs from the server.
     *
     * @param serverPath {@link String} path to the server log file.
     * @param noLines {@link Long} number of lines of log to be retrieved.
     * @return {@link Byte} array representing the last "noLines" logs from the server.
     * @throws IOException
     */
    byte[] retrieveLastLogs(String serverPath, long noLines) throws IOException;

    /**
     * Identifies the searched file and filters the sensitive info from it. The searched As all servers types have the
     * same path to the xwiki properties configuration files, it is not needed to call this function in a server
     * specific class.
     *
     * @param type {@link String} identifies the searched file.
     * @param xwikiCfgFolderPath {@link String} path to the xwiki configuration folder.
     * @return {@link Byte} array representing the filtered file content.
     * @throws IOException
     */
    byte[] getConfigurationFileContent(String type, String xwikiCfgFolderPath) throws IOException;

}
