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
package com.xwiki.admintools.internal.downloads;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import com.xwiki.admintools.LogsDownloader;

/**
 * Abstract implementation of {@link LogsDownloader}. Adds common functions to all server types.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractLogsDownloader implements LogsDownloader
{
    @Inject
    private Logger logger;

    /**
     * Identifies the logs location and applies the filters to the specified server pattern.
     *
     * @param filters Map that can contain the start and end date of the search. It can also be empty.
     * @param listOfFiles File list of all the log files.
     * @param pattern server specific pattern used to identify the log date from the log name.
     * @return byte array representing the logs archive.
     */
    protected byte[] generateArchive(Map<String, String> filters, File[] listOfFiles, Pattern pattern)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            // Go through all the files in the list.
            for (File file : listOfFiles != null ? listOfFiles : new File[0]) {
                // Check if the selected file is of file type and check filters.
                if (file.isFile() && checkFilters(filters, pattern, file)) {
                    // Create a new zip entry and add the content.
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zipOutputStream.putNextEntry(zipEntry);
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                        int bytesRead;
                        while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                            zipOutputStream.write(buffer, 0, bytesRead);
                        }
                        bufferedInputStream.close();
                    }
                    zipOutputStream.closeEntry();
                }
            }
            zipOutputStream.flush();
            byteArrayOutputStream.flush();
            zipOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            logger.warn("Failed to download logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Check that the file date is in the filter range. Returns true if no filter is provided.
     *
     * @param filters Map that can contain the start and end date of the search. It can also be empty.
     * @param pattern server specific pattern used to identify the log date from the log name.
     * @param file current file that is to be checked.
     * @return true if the file is between the provided dates or there is no filter, false otherwise
     */
    private boolean checkFilters(Map<String, String> filters, Pattern pattern, File file)
    {
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.find()) {
            String fromDateFilterKey = "from";
            String toDateFilterKey = "to";
            String fileDateString = matcher.group();
            LocalDate fileDate = LocalDate.parse(fileDateString);

            if (filters.get(fromDateFilterKey) != null && filters.get(toDateFilterKey) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(fromDateFilterKey));
                LocalDate toDate = LocalDate.parse(filters.get(toDateFilterKey));
                return fileDate.isAfter(fromDate.minusDays(1)) && fileDate.isBefore(toDate.plusDays(1));
            } else if (filters.get(fromDateFilterKey) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(fromDateFilterKey));
                return fileDate.isAfter(fromDate.minusDays(1));
            } else if (filters.get(toDateFilterKey) != null) {
                LocalDate toDate = LocalDate.parse(filters.get(toDateFilterKey));
                return fileDate.isBefore(toDate.plusDays(1));
            } else {
                return true;
            }
        }
        return false;
    }
}
