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
package com.xwiki.admintools.internal.files.resources.logs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.download.DataResource;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static java.lang.Integer.parseInt;

/**
 * {@link DataResource} implementation for accessing log files.
 *
 * @version $Id$
 */
@Component
@Named(LogsDataResource.HINT)
@Singleton
public class LogsDataResource implements DataResource
{
    /**
     * Component identifier.
     */
    public static final String HINT = "logs";

    private static final String FROM = "from";

    private static final String TO = "to";

    private static final String NO_LINES = "noLines";

    private static final String DEFAULT_NO_LINES = "1000";

    private static final String LINE_BREAK = "\n";

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private LogFile logFile;

    /**
     * Number of log lines that have been read.
     */
    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public byte[] getByteData(Map<String, String[]> params) throws IOException, NumberFormatException
    {
        try {
            ServerInfo usedServer = currentServer.getCurrentServer();
            if (usedServer == null) {
                throw new NullPointerException("Server not found! Configure path in extension configuration.");
            }

            int linesCount = getRequestedLines(params);
            if (linesCount > 50000) {
                linesCount = 50000;
            }
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("linux")) {
                return getLinuxByteData(usedServer, linesCount);
            } else if (osName.contains("windows")) {
                return getWindowsByteData(usedServer, linesCount);
            } else {
                throw new RuntimeException("OS not supported!");
            }
        } catch (IOException exception) {
            throw new IOException(String.format("Error while accessing log files at [%s].",
                currentServer.getCurrentServer().getLastLogFilePath()), exception);
        } catch (NumberFormatException exception) {
            throw new NumberFormatException(
                String.format("The given [%s] lines number is not a valid number.", params.get(NO_LINES)[0]));
        }
    }

    @Override
    public void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String[]> params)
    {
        Map<String, String> filters = getFilters(params);
        byte[] buffer = new byte[2048];
        try {
            File logsFolder = new File(currentServer.getCurrentServer().getLogsFolderPath());
            File[] listOfFiles = logsFolder.listFiles();
            // Go through all the files in the list.
            for (File file : listOfFiles != null ? listOfFiles : new File[0]) {
                // Check if the selected file is of file type and check filters.
                if (file.isFile()) {
                    if (!filters.isEmpty() && (!checkFilters(file, filters))) {
                        continue;
                    }
                    // Create a new zip entry and add the content.
                    try (FileInputStream fileInputStream = new FileInputStream(file)) {
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
                        ZipEntry zipEntry = new ZipEntry("logs/" + file.getName());
                        zipOutputStream.putNextEntry(zipEntry);
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
            logger.warn("Failed to get logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private byte[] getLinuxByteData(ServerInfo usedServer, int linesCount) throws IOException
    {
        File file = new File(usedServer.getLastLogFilePath());
        List<String> logData = logFile.getLines(file, linesCount);
        Collections.reverse(logData);
        return String.join(LINE_BREAK, logData).getBytes();
    }

    /**
     * Retrieve the last lines of logs by going in descending order through each log file, as Windows OS lacks a merged
     * file of the logs. Reading starts from the latest file last line, until the requested number of log lines is
     * reached.
     *
     * @param usedServer represents the currently used server.
     * @param requestedLines the number of lines that has been requested.
     * @return the last lines of log as a {@link Byte} array.
     * @throws IOException if there are any errors while handling the log files.
     */
    private byte[] getWindowsByteData(ServerInfo usedServer, int requestedLines) throws IOException
    {
        int linesCount = requestedLines;
        String directoryPath = usedServer.getLogsFolderPath();

        // Get list of files in the directory.
        File folder = new File(directoryPath);
        File[] files = folder.listFiles();

        if (files == null) {
            files = new File[0];
        }
        // Filter files starting with the server filter.
        files = Arrays.stream(files).filter(file -> file.getName().startsWith(usedServer.getLogsHint()))
            .toArray(File[]::new);

        // Sort files in descending order.
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        List<String> combinedLogs = new ArrayList<>(linesCount);
        for (File file : files) {
            List<String> retrievedLines = logFile.getLines(file, linesCount);
            linesCount -= retrievedLines.size();
            combinedLogs.addAll(retrievedLines);
            if (linesCount <= 0) {
                break;
            }
        }
        Collections.reverse(combinedLogs);
        return String.join(LINE_BREAK, combinedLogs).getBytes();
    }

    private static Map<String, String> getFilters(Map<String, String[]> params)
    {
        Map<String, String> filters = new HashMap<>();
        if (params != null) {
            filters.put(FROM, !Objects.equals(params.get(FROM)[0], "") ? params.get(FROM)[0] : null);
            filters.put(TO, !Objects.equals(params.get(TO)[0], "") ? params.get(TO)[0] : null);
        }
        return filters;
    }

    private int getRequestedLines(Map<String, String[]> params)
    {
        String noLines;
        if (params == null) {
            noLines = DEFAULT_NO_LINES;
        } else {
            noLines = params.get(NO_LINES)[0];
            if (noLines == null || noLines.isEmpty()) {
                noLines = DEFAULT_NO_LINES;
            }
        }
        return parseInt(noLines);
    }

    /**
     * Check that the file date is in the filter range. Returns {@code true} if no filter is provided.
     *
     * @param filters represents the date range of the search.
     * @param file current {@link File} that is to be checked.
     * @return {@code true} if the file is between the provided dates or there is no filter, {@code false} otherwise
     */
    private boolean checkFilters(File file, Map<String, String> filters)
    {
        // Get the server specific Pattern used to identify the log date from the log name.
        Pattern pattern = currentServer.getCurrentServer().getLogsPattern();
        Matcher matcher = pattern.matcher(file.getName());
        if (matcher.find()) {
            XWikiContext wikiContext = contextProvider.get();
            XWiki xWiki = wikiContext.getWiki();
            String userDateFormat = xWiki.getXWikiPreference("dateformat", "dd-MM-yyyy", wikiContext);
            String fileDateString = matcher.group();
            LocalDate fileDate = LocalDate.parse(fileDateString);
            DateTimeFormatter filtersFormatter = DateTimeFormatter.ofPattern(userDateFormat);
            if (filters.get(FROM) != null && filters.get(TO) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(FROM), filtersFormatter);
                LocalDate toDate = LocalDate.parse(filters.get(TO), filtersFormatter);
                return fileDate.isAfter(fromDate.minusDays(1)) && fileDate.isBefore(toDate.plusDays(1));
            } else if (filters.get(FROM) != null) {
                LocalDate fromDate = LocalDate.parse(filters.get(FROM), filtersFormatter);
                return fileDate.isAfter(fromDate.minusDays(1));
            } else if (filters.get(TO) != null) {
                LocalDate toDate = LocalDate.parse(filters.get(TO), filtersFormatter);
                return fileDate.isBefore(toDate.plusDays(1));
            } else {
                return true;
            }
        }
        return false;
    }
}
