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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerInfo;
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
class LogsDataResourceTest
{
    private final Map<String, String[]> params = Map.of("noLines", new String[] { "44" });

    @InjectMockComponents
    private LogsDataResource logsDataResource;

    @Mock
    private ZipOutputStream zipOutputStream;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @XWikiTempDir
    private File tmpDir;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerInfo serverInfo;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private LastLogsUtil lastLogsUtil;

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
        for (int i = 0; i < 10; i++) {
            writer.append(String.format("log line %d\n", i));
            writer2.append(String.format("log line 2.%d\n", i));
        }
        writer.close();
        writer2.close();
        logLines = new ArrayList<>();
        logLines.add("log line 1");
        logLines.add("log line 2");
        logLines.add("log line 3");
        logLines.add("log line 4");
    }

    @BeforeEach
    void beforeEach()
    {
        when(contextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getXWikiPreference("dateformat", "dd-MM-yyyy", wikiContext)).thenReturn("dd-MM-yyyy");
        when(currentServer.getCurrentServer()).thenReturn(serverInfo);
        when(serverInfo.getLogsFolderPath()).thenReturn(logsDir.getAbsolutePath());
    }

    @Test
    void getIdentifier()
    {
        assertEquals(LogsDataResource.HINT, logsDataResource.getIdentifier());
    }

    @Test
    void getByteDataSuccess() throws Exception
    {
        Collections.reverse(logLines);
        byte[] testBytes = String.join("\n", logLines).getBytes();
        when(lastLogsUtil.getLastLinesOfLog(serverInfo, 44)).thenReturn(testBytes);
        System.setProperty("os.name", "Linux");
        assertArrayEquals(testBytes, logsDataResource.getByteData(params));
        System.clearProperty("os.name");
    }



    @Test
    void getByteDataNullInput() throws IOException
    {
        Collections.reverse(logLines);
        byte[] testBytes = String.join("\n", logLines).getBytes();
        when(lastLogsUtil.getLastLinesOfLog(serverInfo, 1000)).thenReturn(testBytes);
        System.setProperty("os.name", "Linux");
        assertArrayEquals(String.join("\n", logLines).getBytes(), logsDataResource.getByteData(null));
        System.clearProperty("os.name");
    }

    @Test
    void getByteDataNullNoLines() throws IOException
    {
        Map<String, String[]> params = Map.of("noLines", new String[] { null });
        Collections.reverse(logLines);
        byte[] testBytes = String.join("\n", logLines).getBytes();
        when(lastLogsUtil.getLastLinesOfLog(serverInfo, 1000)).thenReturn(testBytes);
        System.setProperty("os.name", "Linux");
        assertArrayEquals(testBytes, logsDataResource.getByteData(params));
        System.clearProperty("os.name");
    }

    @Test
    void getByteDataFileNotFound() throws IOException
    {
        File testFile = new File("server.2023-10-06.log");
        assertFalse(testFile.exists());

        when(serverInfo.getLastLogFilePath()).thenReturn(testFile.getAbsolutePath());
        System.setProperty("os.name", "Linux");
        when(lastLogsUtil.getLastLinesOfLog(serverInfo, 1000)).thenThrow(new IOException(""));
        IOException exception = assertThrows(IOException.class, () -> logsDataResource.getByteData(null));
        assertEquals(String.format("Error while accessing log files at [%s].", testFile.getAbsolutePath()),
            exception.getMessage());
        System.clearProperty("os.name");
    }

    @Test
    void getByteDataServerNotFound()
    {
        when(currentServer.getCurrentServer()).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> {
            this.logsDataResource.getByteData(params);
        });
        assertEquals("Server not found! Configure path in extension configuration.", exception.getMessage());
        logsDir.delete();
    }

    @Test
    void getByteDataIncorrectInput()
    {
        String invalidInput = "not a number";
        Map<String, String[]> params = Map.of("noLines", new String[] { "not a number" });
        Exception exception = assertThrows(Exception.class, () -> logsDataResource.getByteData(params));
        assertEquals(String.format("The given [%s] lines number is not a valid number.", invalidInput),
            exception.getMessage());
    }

    @Test
    void addZipEntrySuccessNoFilters() throws IOException
    {
        when(serverInfo.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        logsDataResource.addZipEntry(zipOutputStream, null);
        verify(zipOutputStream, times(2)).closeEntry();
    }

    @Test
    void addZipEntrySuccessWithFilters() throws IOException
    {
        when(serverInfo.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));

        Map<String, String[]> filters = new HashMap<>();
        filters.put("from", new String[] { "06-10-2023" });
        filters.put("to", new String[] { "07-10-2023" });
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
        when(serverInfo.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        when(xWiki.getXWikiPreference("dateformat", "dd-MM-yyyy", wikiContext)).thenReturn("dd yy MM");

        Map<String, String[]> filters = new HashMap<>();
        filters.put("from", new String[] { "10 23 10" });
        filters.put("to", new String[] { null });

        logsDataResource.addZipEntry(zipOutputStream, filters);
        verify(zipOutputStream, never()).closeEntry();
    }

    @Test
    void addZipEntryDateParseError()
    {
        when(serverInfo.getLogsPattern()).thenReturn(Pattern.compile("\\bserver\\b"));
        Map<String, String[]> filters = new HashMap<>();
        filters.put("from", new String[] { "2023-10-03" });
        filters.put("to", new String[] { "2023-10-05" });
        logsDataResource.addZipEntry(zipOutputStream, filters);
        assertEquals("Failed to get logs. Root cause is: "
            + "[DateTimeParseException: Text 'server' could not be parsed at index 0]", logCapture.getMessage(0));
    }

    @Test
    void addZipEntryPatternNotFound() throws IOException
    {
        when(serverInfo.getLogsPattern()).thenReturn(Pattern.compile("\\d{4}_\\d{2}_\\d{2}"));
        Map<String, String[]> filters = new HashMap<>();
        filters.put("from", new String[] { "2023-10-03" });
        filters.put("to", new String[] { "2023-10-05" });
        logsDataResource.addZipEntry(zipOutputStream, filters);
        verify(zipOutputStream, never()).closeEntry();
    }
}
