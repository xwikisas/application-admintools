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
package com.xwiki.admintools.internal.download.resources;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.xwiki.admintools.download.DataResource;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Collection of functions used for accessing XWiki properties file.
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
    public static final String HINT = "xwikiPropertiesFileDataResource";

    private final List<String> excludedLinesHints = new ArrayList<>(
        Arrays.asList("xwiki.authentication.validationKey", "xwiki.authentication.encryptionKey",
            "xwiki.superadminpassword", "extension.repositories.privatemavenid.auth", "mail.sender.password"));

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Logger logger;

    @Override
    public void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String> filters) throws IOException
    {
        if (filters == null) {
            createArchiveEntry(zipOutputStream);
        }
    }

    @Override
    public byte[] getByteData(String input) throws IOException
    {
        String filePath = currentServer.getCurrentServer().getXwikiCfgFolderPath() + "xwiki.properties";
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
            reader.close();
            return stringBuilder.toString().getBytes();
        } catch (Exception e) {
            logger.warn("Failed to download logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    private void createArchiveEntry(ZipOutputStream zipOutputStream) throws IOException
    {
        ZipEntry zipEntry = new ZipEntry("xwiki.properties");

        zipOutputStream.putNextEntry(zipEntry);

        byte[] buffer = getByteData(null);
        zipOutputStream.write(buffer, 0, buffer.length);
        zipOutputStream.closeEntry();
    }
}
