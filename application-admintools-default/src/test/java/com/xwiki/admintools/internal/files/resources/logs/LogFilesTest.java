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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link LogFiles}
 *
 * @version $Id$
 */
@ComponentTest
public class LogFilesTest
{
    @InjectMockComponents
    private LogFiles logFiles;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerInfo serverInfo;

    @XWikiTempDir
    private File tmpDir;

    private File testFile;

    private File logsDir;

    @BeforeComponent
    void setUp() throws IOException
    {
        logsDir = new File(tmpDir, "server_logs_folder");
        logsDir.mkdir();
        logsDir.deleteOnExit();
        testFile = new File(logsDir, "server.2023-10-06.log");
        testFile.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        for (int i = 0; i < 100; i++) {
            writer.append(String.format("log line %d\n", i));
        }
        writer.close();
    }

    @Test
    void getLinesSuccess() throws Exception
    {
        assertTrue(testFile.exists());
        assertTrue(testFile.isFile());

        List<String> logLines = readLines(44, testFile);
        assertArrayEquals(logLines.toArray(), logFiles.getLines(testFile, 44).toArray());
    }

    @Test
    void getLinesFileError() throws Exception
    {
        File invalidFile = new File("");
        IOException exception = assertThrows(IOException.class,
            () -> logFiles.getLines(invalidFile, 1000));
        assertEquals(" (No such file or directory)", exception.getMessage());
    }

    private List<String> readLines(int lines, File testFile) throws IOException
    {
        RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "r");
        long fileLength = randomAccessFile.length();
        List<String> logLines = new ArrayList<>();
        long startPosition = fileLength - 1;
        for (long i = 0; i < lines && startPosition > 0 && i < 50000; startPosition--) {
            randomAccessFile.seek(startPosition - 1);
            int currentByte = randomAccessFile.read();
            if (currentByte == '\n' || currentByte == '\r') {
                logLines.add(randomAccessFile.readLine());
                i++;
            }
        }
        return logLines;
    }
}
