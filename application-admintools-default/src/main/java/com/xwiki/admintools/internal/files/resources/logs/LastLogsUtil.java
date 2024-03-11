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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.ServerInfo;

/**
 * Retrieves the required last lines of log from the server.
 *
 * @version $Id$
 */
@Component(roles = LastLogsUtil.class)
@Singleton
public class LastLogsUtil
{
    private static final String LINE_BREAK = "\n";

    private int remainingLines;

    /**
     * Get the last lines of logs.
     *
     * @param usedServer the server that is currently being used.
     * @param requestedLines the number of log lines that has been requested.
     * @return the last lines of log as a {@link Byte} array.
     * @throws IOException when there are errors while handling searched files.
     */
    public byte[] getLastLinesOfLog(ServerInfo usedServer, int requestedLines) throws IOException
    {
        this.remainingLines = requestedLines;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            File file = new File(usedServer.getLastLogFilePath());
            List<String> logData = readFileLines(file);
            Collections.reverse(logData);
            return String.join(LINE_BREAK, logData).getBytes();
        } else if (osName.contains("windows")) {
            return getWindowsByteData(usedServer);
        } else {
            throw new RuntimeException("OS not supported!");
        }
    }

    /**
     * Retrieve the last lines of logs by going in descending order through each log file, as Windows OS lacks a merged
     * file of the logs. Reading starts from the latest file last line, until the requested number of log lines is
     * reached.
     *
     * @param usedServer represents the currently used server.
     * @return the last lines of log as a {@link Byte} array.
     * @throws IOException if there are any errors while handling the log files.
     */
    private byte[] getWindowsByteData(ServerInfo usedServer) throws IOException
    {
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

        List<String> combinedLogs = new ArrayList<>(remainingLines);
        for (File file : files) {
            combinedLogs.addAll(readFileLines(file));
            if (remainingLines <= 0) {
                break;
            }
        }
        Collections.reverse(combinedLogs);
        return String.join(LINE_BREAK, combinedLogs).getBytes();
    }

    private List<String> readFileLines(File file) throws IOException
    {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {

            long fileLength = randomAccessFile.length();
            List<String> logLines = new ArrayList<>();

            // Calculate the approximate position to start reading from based on line length.
            long startPosition = fileLength - 1;

            for (; startPosition > 0 && remainingLines > 0; startPosition--) {
                randomAccessFile.seek(startPosition - 1);

                int currentByte = randomAccessFile.read();
                if (currentByte == '\n' || currentByte == '\r') {
                    // Found a newline character, add the line to the list.
                    logLines.add(randomAccessFile.readLine());
                    remainingLines--;
                }
            }
            return logLines;
        }
    }
}
