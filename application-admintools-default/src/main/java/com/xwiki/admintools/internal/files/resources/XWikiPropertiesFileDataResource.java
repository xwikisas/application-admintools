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
package com.xwiki.admintools.internal.files.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.download.DataResource;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * {@link DataResource} implementation for accessing the xwiki.properties file.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(XWikiPropertiesFileDataResource.HINT)
@Singleton
public class XWikiPropertiesFileDataResource implements DataResource
{
    /**
     * Component identifier.
     */
    public static final String HINT = "xwikiProperties";

    private static final String XWIKI_PROPERTIES = "xwiki.properties";

    private static final String ERROR_SOURCE = " ";

    @Inject
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Logger logger;

    @Override
    public void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String> filters)
    {
        addZipEntry(zipOutputStream);
    }

    @Override
    public byte[] getByteData(String input) throws IOException
    {
        try {
            ServerIdentifier usedServer = currentServer.getCurrentServer();
            if (usedServer == null) {
                throw new NullPointerException("Server not found! Configure path in extension configuration.");
            }
            List<String> excludedLinesHints = adminToolsConfig.getExcludedLines();
            String filePath = usedServer.getXwikiCfgFolderPath() + XWIKI_PROPERTIES;
            File inputFile = new File(filePath);
            try (BufferedReader reader = new BufferedReader(new FileReader(inputFile))) {
                StringBuilder stringBuilder = new StringBuilder();
                String currentLine;

                // Read line by line and do not add it if it contains sensitive info.
                while ((currentLine = reader.readLine()) != null) {
                    String trimmedLine = currentLine.trim();
                    if (excludedLinesHints.stream().anyMatch(trimmedLine::contains)) {
                        continue;
                    }
                    stringBuilder.append(currentLine).append(System.getProperty("line.separator"));
                }
                return stringBuilder.toString().getBytes();
            }
        } catch (IOException exception) {
            throw new IOException(String.format("Error while handling %s file.", XWIKI_PROPERTIES), exception);
        }
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    private void addZipEntry(ZipOutputStream zipOutputStream)
    {
        try {
            byte[] buffer = getByteData(null);
            ZipEntry zipEntry = new ZipEntry(XWIKI_PROPERTIES);
            zipOutputStream.putNextEntry(zipEntry);
            zipOutputStream.write(buffer, 0, buffer.length);
            zipOutputStream.closeEntry();
        } catch (Exception exception) {
            logger.error("Could not add [{}] to the archive. Root cause is: [{}]", XWIKI_PROPERTIES,
                ExceptionUtils.getRootCauseMessage(exception));
        }
    }
}
