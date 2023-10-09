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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import static java.lang.Integer.parseInt;

/**
 * Collection of functions used for accessing log files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(LogsDataResource.HINT)
@Singleton
public class LogsDataResource implements DataResource
{
    /**
     * Component identifier.
     */
    public static final String HINT = "logsDataResource";

    private static final String FROM_DATE_FILTER_KEY = "from";

    private static final String TO_DATE_FILTER_KEY = "to";

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Override
    public void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String> filters)
    {
        createZipEntry(zipOutputStream, filters);
    }

    @Override
    public byte[] getByteData(String input) throws IOException
    {
        File file = new File(currentServer.getCurrentServer().getLastLogFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + currentServer.getCurrentServer().getLastLogFilePath());
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            int lines = parseInt(input);

            long fileLength = randomAccessFile.length();
            List<String> logLines = new ArrayList<>();

            // Calculate the approximate position to start reading from based on line length
            long startPosition = fileLength - 1;
            for (long i = 0; i < lines && startPosition > 0 && i < 50000; startPosition--) {
                randomAccessFile.seek(startPosition - 1);
                int currentByte = randomAccessFile.read();
                if (currentByte == '\n' || currentByte == '\r') {
                    // Found a newline character, add the line to the list
                    logLines.add(randomAccessFile.readLine());
                    i++;
                }
            }
            // Reverse the list to get the lines in the correct order
            Collections.reverse(logLines);
            // Join the lines with newline characters
            return String.join("\n", logLines).getBytes();
        } catch (IOException e) {
            logger.warn("Failed to retrieve logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    private void createZipEntry(ZipOutputStream zipOutputStream, Map<String, String> filters)
    {
        byte[] buffer = new byte[2048];
        try {
            File logsFolder = new File(currentServer.getCurrentServer().getLogsFolderPath());
            File[] listOfFiles = logsFolder.listFiles();
            // Go through all the files in the list.
            for (File file : listOfFiles != null ? listOfFiles : new File[0]) {
                // Check if the selected file is of file type and check filters.
                if (file.isFile() && checkFilters(filters, file)) {
                    // Create a new zip entry and add the content.
                    ZipEntry zipEntry = new ZipEntry("logs/" + file.getName());
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
        } catch (Exception e) {
            logger.warn("Failed to download logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Check that the file date is in the filter range. Returns {@code true} if no filter is provided.
     *
     * @param filters represents the date range of the search.
     * @param file current {@link File} that is to be checked.
     * @return {@code true} if the file is between the provided dates or there is no filter, {@code false} otherwise
     */
    private boolean checkFilters(Map<String, String> filters,  File file)
    {
        // Get the server specific Pattern used to identify the log date from the log name.
        Pattern pattern = currentServer.getCurrentServer().getLogsPattern();
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.find()) {
            String fileDateString = matcher.group();
            LocalDate fileDate = LocalDate.parse(fileDateString);

            if (filters.get(FROM_DATE_FILTER_KEY) != null && filters.get(TO_DATE_FILTER_KEY) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(FROM_DATE_FILTER_KEY));
                LocalDate toDate = LocalDate.parse(filters.get(TO_DATE_FILTER_KEY));
                return fileDate.isAfter(fromDate.minusDays(1)) && fileDate.isBefore(toDate.plusDays(1));
            } else if (filters.get(FROM_DATE_FILTER_KEY) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(FROM_DATE_FILTER_KEY));
                return fileDate.isAfter(fromDate.minusDays(1));
            } else if (filters.get(TO_DATE_FILTER_KEY) != null) {
                LocalDate toDate = LocalDate.parse(filters.get(TO_DATE_FILTER_KEY));
                return fileDate.isBefore(toDate.plusDays(1));
            } else {
                return true;
            }
        }
        return false;
    }
}
