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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.inject.Named;

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

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link XWikiConfigFileDataResource}
 *
 * @version $Id$
 */
@ComponentTest
public class XWikiConfigFileDataResourceTest
{
    @InjectMockComponents
    private XWikiConfigFileDataResource configFileDataResource;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerIdentifier serverIdentifier;

    @MockComponent
    @Named("default")
    private AdminToolsConfiguration adminToolsConfiguration;

    @Mock
    private ZipOutputStream zipOutputStream;

    @Mock
    private Logger logger;

    @XWikiTempDir
    private File tmpDir;

    private File testFile;

    private File cfgDir;

    private String cfgDirPath;

    private List<String> excludedLines;

    @BeforeComponent
    void setUp() throws IOException
    {
        cfgDir = new File(tmpDir, "xwiki_cfg_folder");
        cfgDir.mkdir();
        cfgDir.deleteOnExit();
        testFile = new File(cfgDir, "xwiki.cfg");
        testFile.createNewFile();
        cfgDirPath = cfgDir.getAbsolutePath() + "/";
        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        for (int i = 0; i < 100; i++) {
            writer.append(String.format("cfg line %d\n", i));
        }
        writer.append("excl l1\n");
        writer.append("excl l2\n");
        writer.append("excl l4\n");
        excludedLines = new ArrayList<>();
        excludedLines.add("excl l1");
        excludedLines.add("excl l2");
        excludedLines.add("excl l3");

        writer.close();
    }

    @Test
    void getIdentifier()
    {
        assertEquals(XWikiConfigFileDataResource.HINT, configFileDataResource.getIdentifier());
    }

    @Test
    void getByteData() throws Exception
    {
        when(adminToolsConfiguration.getExcludedLines()).thenReturn(excludedLines);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgDirPath);

        assertArrayEquals(readLines(), configFileDataResource.getByteData(null));
    }

    @Test
    void getByteDataFileNotFound()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configFileDataResource, "logger", this.logger);

        File cfgDir2 = new File(tmpDir, "xwiki_cfg_folder_fail");
        cfgDir2.mkdir();
        cfgDir2.deleteOnExit();

        when(adminToolsConfiguration.getExcludedLines()).thenReturn(excludedLines);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgDir2.getAbsolutePath() + "/");
        Exception exception = assertThrows(Exception.class, () -> {
            configFileDataResource.getByteData(null);
        });
        assertEquals("Error while handling xwiki.cfg file.", exception.getMessage());
    }

    @Test
    void getByteDataConfigError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configFileDataResource, "logger", this.logger);

        File propertiesDir2 = new File(tmpDir, "xwiki_properties_folder_fail");
        propertiesDir2.mkdir();
        propertiesDir2.deleteOnExit();

        when(adminToolsConfiguration.getExcludedLines()).thenThrow(new RuntimeException("CONFIGURATION ERROR"));
        Exception exception = assertThrows(Exception.class, () -> {
            configFileDataResource.getByteData(null);
        });
        assertEquals("Error while retrieving data from Admin Tools configuration.", exception.getMessage());
        verify(logger).warn("Error while retrieving data from Admin Tools configuration. Root cause is: [{}]",
            "RuntimeException: CONFIGURATION ERROR");
    }

    @Test
    void getByteDataServerNotFound() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configFileDataResource, "logger", this.logger);

        File cfgDir2 = new File(tmpDir, "xwiki_cfg_folder_fail");
        cfgDir2.mkdir();
        cfgDir2.deleteOnExit();

        when(adminToolsConfiguration.getExcludedLines()).thenReturn(excludedLines);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenThrow(new NullPointerException("SERVER NOT FOUND"));
        Exception exception = assertThrows(Exception.class, () -> {
            configFileDataResource.getByteData(null);
        });
        assertEquals("Server not found.", exception.getMessage());
        verify(logger).warn("Server not found. Root cause is: [{}]",
            "NullPointerException: SERVER NOT FOUND");
    }

    @Test
    void addZipEntry() throws Exception
    {
        when(adminToolsConfiguration.getExcludedLines()).thenReturn(excludedLines);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgDirPath);
        configFileDataResource.addZipEntry(zipOutputStream, null);
        byte[] buff = readLines();
        int buffLength = buff.length;
        verify(zipOutputStream).write(AdditionalMatchers.aryEq(buff), eq(0), eq(buffLength));
    }

    @Test
    void addZipEntryGetByteFail() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configFileDataResource, "logger", this.logger);

        File cfgDir2 = new File(tmpDir, "xwiki_cfg_folder_fail");
        cfgDir2.mkdir();
        cfgDir2.deleteOnExit();

        when(adminToolsConfiguration.getExcludedLines()).thenReturn(excludedLines);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgDir2.getAbsolutePath() + "/");

        configFileDataResource.addZipEntry(zipOutputStream, null);
        verify(zipOutputStream, never()).write(any(), eq(0), anyInt());
        verify(logger).warn("Could not add {} to the archive. Root cause is: [{}]", "xwiki.cfg",
            "FileNotFoundException: " + cfgDir2.getAbsolutePath() + "/xwiki.cfg (No such file or "
                + "directory)");
    }

    private byte[] readLines() throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(testFile));
        StringBuilder stringBuilder = new StringBuilder();
        String currentLine;

        // Read line by line and do not add it if it contains sensitive info.
        while ((currentLine = reader.readLine()) != null) {
            String trimmedLine = currentLine.trim();
            if (excludedLines.stream().anyMatch(trimmedLine::contains)) {
                continue;
            }
            stringBuilder.append(currentLine).append(System.getProperty("line.separator"));
        }
        reader.close();
        return stringBuilder.toString().getBytes();
    }
}
