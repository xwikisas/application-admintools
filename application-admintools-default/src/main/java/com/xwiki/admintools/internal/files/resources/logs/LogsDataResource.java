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
import java.util.HashMap;
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

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private LastLogsUtil lastLogsUtil;

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

            int requestedLines = getRequestedLines(params);
            if (requestedLines > 50000) {
                requestedLines = 50000;
            }
            return lastLogsUtil.getLastLinesOfLog(usedServer, requestedLines);
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
