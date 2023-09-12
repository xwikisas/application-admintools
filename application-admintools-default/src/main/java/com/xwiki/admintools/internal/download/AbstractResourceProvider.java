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
package com.xwiki.admintools.internal.download;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import com.xwiki.admintools.ResourceProvider;

/**
 * Abstract implementation of {@link ResourceProvider}. Adds common functions for all server types.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractResourceProvider implements ResourceProvider
{
    @Inject
    private Logger logger;

//    @Override
//    public byte[] getConfigurationFileContent(String type, String xwikiCfgFolderPath) throws IOException
//    {
//        String filePath = "";
//        if (Objects.equals(type, "properties")) {
//            filePath = xwikiCfgFolderPath + "xwiki.properties";
//        } else if (Objects.equals(type, "config")) {
//            filePath = xwikiCfgFolderPath + "xwiki.cfg";
//        }
//        File inputFile = new File(filePath);
//
//        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
//        StringBuilder stringBuilder = new StringBuilder();
//
//        String currentLine;
//        List<String> wordsList = new ArrayList<>(
//            Arrays.asList("xwiki.authentication.validationKey", "xwiki.authentication.encryptionKey",
//                "xwiki.superadminpassword", "extension.repositories.privatemavenid.auth", "mail.sender.password"));
//
//        // Read line by line and do not add it if it contains sensitive info.
//        while ((currentLine = reader.readLine()) != null) {
//            String trimmedLine = currentLine.trim();
//            if (wordsList.stream().anyMatch(trimmedLine::contains)) {
//                continue;
//            }
//            stringBuilder.append(currentLine).append(System.getProperty("line.separator"));
//        }
//        reader.close();
//        return stringBuilder.toString().getBytes();
//    }

    /**
     * Identifies the logs location and applies the filters to the specified server pattern.
     *
     * @param filters {@link Map} that can contain the start and end date of the search. It can also be empty.
     * @param listOfFiles {@link File} list of all the log files.
     * @param pattern server specific {@link Pattern} used to identify the log date from the log name.
     * @return {@link Byte} array representing the logs archive.
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
     * Get server's last "noLines" logs. It is limited to 50000 lines.
     *
     * @param filePath {@link String} path to the log file.
     * @param noLines {@link Long} number of lines to be retrieved.
     * @return {@link Byte} array representing the last "noLine" logs.
     * @throws IOException
     */
    protected byte[] defaultRetrieveLastLogs(String filePath, long noLines) throws IOException
    {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long fileLength = randomAccessFile.length();
            List<String> logLines = new ArrayList<>();

            // Calculate the approximate position to start reading from based on line length
            long startPosition = fileLength - 1;
            for (long i = 0; i < noLines && startPosition > 0 && i < 50000; startPosition--) {
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

    /**
     * Check that the file date is in the filter range. Returns true if no filter is provided.
     *
     * @param filters {@link Map} that can contain the start and end date of the search. It can also be empty.
     * @param pattern server specific {@link Pattern} used to identify the log date from the log name.
     * @param file current {@link File} that is to be checked.
     * @return {@link Boolean} true if the file is between the provided dates or there is no filter, false otherwise
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
