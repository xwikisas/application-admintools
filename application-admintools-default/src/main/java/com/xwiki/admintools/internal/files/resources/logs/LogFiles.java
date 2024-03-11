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
import java.util.Comparator;
import java.util.List;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

/**
 * Executes operations on the log files.
 *
 * @version $Id$
 */
@Component(roles = LogFiles.class)
@Singleton
public class LogFiles
{
    /**
     * Get the last lines of text from a specific file.
     *
     * @param file the {@link File} from which to retrieve the lines.
     * @param requestedLines the number of lines that has been requested.
     * @return the last lines from the file as a {@link Byte} array.
     * @throws IOException when there are errors while handling the file.
     */
    public List<String> getLines(File file, int requestedLines) throws IOException
    {
        int linesCount = requestedLines;
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {

            long fileLength = randomAccessFile.length();
            List<String> logLines = new ArrayList<>();

            // Calculate the approximate position to start reading from based on line length.
            long startPosition = fileLength - 1;

            for (; startPosition > 0 && linesCount > 0; startPosition--) {
                randomAccessFile.seek(startPosition - 1);

                int currentByte = randomAccessFile.read();
                if (currentByte == '\n' || currentByte == '\r') {
                    // Found a newline character, add the line to the list.
                    logLines.add(randomAccessFile.readLine());
                    linesCount--;
                }
            }
            return logLines;
        }
    }

    /**
     * Get the list of files from a specific directory and with a specific starting hint.
     * @param directoryPath path to the directory.
     * @param specificLogsHint specific hint for a file name to start with.
     * @return a {@link File} array with items in descending order.
     */
    public File[] getLogFiles(String directoryPath, String specificLogsHint) {
        File folder = new File(directoryPath);
        File[] files = folder.listFiles();

        if (files == null) {
            files = new File[0];
        }
        // Filter files starting with the server filter.
        files = Arrays.stream(files).filter(file -> file.getName().startsWith(specificLogsHint))
            .toArray(File[]::new);

        // Sort files in descending order.
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());

        return files;
    }
}
