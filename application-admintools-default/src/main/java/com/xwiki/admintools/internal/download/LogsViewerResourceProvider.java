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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.ResourceProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Encapsulates functions used for viewing last log lines.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(LogsViewerResourceProvider.HINT)
@Singleton
public class LogsViewerResourceProvider implements ResourceProvider<Long>
{
    /**
     * Component identifier.
     */
    public static final String HINT = "logsViewerResourceProvider";

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Override
    public byte[] getByteData(Long input) throws IOException
    {
        File file = new File(currentServer.getCurrentServer().getLogFilePath());
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("File not found: " + currentServer.getCurrentServer().getLogFilePath());
        }
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long fileLength = randomAccessFile.length();
            List<String> logLines = new ArrayList<>();

            // Calculate the approximate position to start reading from based on line length
            long startPosition = fileLength - 1;
            for (long i = 0; i < input && startPosition > 0 && i < 50000; startPosition--) {
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
}
