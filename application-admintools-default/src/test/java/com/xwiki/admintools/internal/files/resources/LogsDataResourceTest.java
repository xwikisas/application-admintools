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
package com.xwiki.admintools.internal.files.resources;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link LogsDataResource}
 *
 * @version $Id$
 */
@ComponentTest
public class LogsDataResourceTest
{
    @Mock
    ZipOutputStream zipOutputStream;

    @InjectMockComponents
    private LogsDataResource logsDataResource;

    @Mock
    private Logger logger;

    @XWikiTempDir
    private File tmpDir;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerIdentifier serverIdentifier;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki xWiki;

    private File testFile;

    private File testFile2;

    private List<String> logLines;

    private File logsDir;

    @BeforeComponent
    void setUp() throws IOException
    {
        logsDir = new File(tmpDir, "server_logs_folder");
        logsDir.mkdir();
        logsDir.deleteOnExit();
        testFile = new File(logsDir, "server.2023-10-06.log");
        testFile.createNewFile();

        testFile2 = new File(logsDir, "server.2023-10-09.log");
        testFile2.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        BufferedWriter writer2 = new BufferedWriter(new FileWriter(testFile2.getAbsolutePath()));
        for (int i = 0; i < 100; i++) {
            writer.append(String.format("log line %d\n", i));
            writer2.append(String.format("log line 2.%d\n", i));
        }
        writer.close();
        writer2.close();
    }

    @BeforeEach
    void beforeEach()
    {
        when(contextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getXWikiPreference("dateformat", "dd-MM-yyyy", wikiContext)).thenReturn("dd-MM-yyyy");
    }

    @Test
    void getIdentifier()
    {
        assertEquals(LogsDataResource.HINT, logsDataResource.getIdentifier());
    }

    @Test
    void getByteDataSuccess() throws Exception
    {
        assertTrue(testFile.exists());
        assertTrue(testFile.isFile());

        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLastLogFilePath()).thenReturn(testFile.getAbsolutePath());
        readLines(44);

        assertArrayEquals(String.join("\n", logLines).getBytes(), logsDataResource.getByteData("44"));
    }

    @Test
    void getByteDataFileNotFound()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(logsDataResource, "logger", this.logger);

        File testFile = new File("server.2023-10-06.log");
        assertFalse(testFile.exists());

        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLastLogFilePath()).thenReturn(testFile.getAbsolutePath());
        IOException exception = assertThrows(IOException.class, () -> {
            logsDataResource.getByteData(null);
        });
        assertEquals(String.format("Error while accessing log files at [%s].", testFile.getAbsolutePath()),
            exception.getMessage());
    }

    @Test
    void getByteDataServerNotFound()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(logsDataResource, "logger", this.logger);

        Exception exception = assertThrows(Exception.class, () -> {
            this.logsDataResource.getByteData("44");
        });
        assertEquals("Server not found! Configure path in extension configuration.", exception.getMessage());
        logsDir.delete();
    }

    @Test
    void getByteDataIncorrectInput()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(logsDataResource, "logger", this.logger);

        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLastLogFilePath()).thenReturn(testFile.getAbsolutePath());
        String invalidInput = "not a number";
        Exception exception = assertThrows(Exception.class, () -> logsDataResource.getByteData(invalidInput));
        assertEquals(String.format("The given [%s] lines number is not a valid number.", invalidInput),
            exception.getMessage());
    }

    @Test
    void addZipEntrySuccessNoFilters() throws IOException
    {
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
        when(serverIdentifier.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        logsDataResource.addZipEntry(zipOutputStream, null);
        verify(zipOutputStream, times(2)).closeEntry();
    }

    @Test
    void addZipEntrySuccessWithFilters() throws IOException
    {
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
        when(serverIdentifier.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));

        Map<String, String> filters = new HashMap<>();
        filters.put("from", "06-10-2023");
        filters.put("to", "07-10-2023");
        readLines(400);
        logsDataResource.addZipEntry(zipOutputStream, filters);
        byte[] buff = new byte[2048];
        int bytesRead;
        FileInputStream fileInputStream = new FileInputStream(testFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        bytesRead = bufferedInputStream.read(buff);

        verify(zipOutputStream).write(AdditionalMatchers.aryEq(buff), eq(0), eq(bytesRead));
    }

    @Test
    void addZipEntryFilesOutOfFiltersRange() throws IOException
    {
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
        when(serverIdentifier.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        when(xWiki.getXWikiPreference("dateformat", "dd-MM-yyyy", wikiContext)).thenReturn("dd yy MM");

        Map<String, String> filters = new HashMap<>();
        filters.put("from", "10 23 10");
        filters.put("to", null);

        logsDataResource.addZipEntry(zipOutputStream, filters);
        verify(zipOutputStream, never()).closeEntry();
    }

    @Test
    void addZipEntryDateParseError()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(logsDataResource, "logger", this.logger);

        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
        when(serverIdentifier.getLogsPattern()).thenReturn(Pattern.compile("\\bserver\\b"));
        Map<String, String> filters = new HashMap<>();
        filters.put("from", "2023-10-03");
        filters.put("to", "2023-10-05");
        logsDataResource.addZipEntry(zipOutputStream, filters);
        verify(logger).warn("Failed to get logs. Root cause is: [{}]",
            "DateTimeParseException: Text 'server' could not be parsed at index 0");
    }

    @Test
    void addZipEntryPatternNotFound() throws IOException
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(logsDataResource, "logger", this.logger);

        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
        when(serverIdentifier.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}_\\d{2}_\\d{2}"));
        Map<String, String> filters = new HashMap<>();
        filters.put("from", "2023-10-03");
        filters.put("to", "2023-10-05");
        logsDataResource.addZipEntry(zipOutputStream, filters);
        verify(zipOutputStream, never()).closeEntry();
    }

    private void readLines(int lines) throws IOException
    {
        RandomAccessFile randomAccessFile = new RandomAccessFile(testFile, "r");
        long fileLength = randomAccessFile.length();
        logLines = new ArrayList<>();
        long startPosition = fileLength - 1;
        for (long i = 0; i < lines && startPosition > 0 && i < 50000; startPosition--) {
            randomAccessFile.seek(startPosition - 1);
            int currentByte = randomAccessFile.read();
            if (currentByte == '\n' || currentByte == '\r') {
                logLines.add(randomAccessFile.readLine());
                i++;
            }
        }
        Collections.reverse(logLines);
    }
}
