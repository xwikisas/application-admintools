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
package com.xwiki.admintools.internal.files;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.download.DataResource;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.files.resources.LogsDataResource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ImportantFilesManager}
 *
 * @version $Id$
 */
@ComponentTest
public class ImportantFilesManagerTest
{
    private final String templatePath = "filesSectionTemplate.vm";

    @InjectMockComponents
    private ImportantFilesManager importantFilesManager;

    @MockComponent
    private Provider<List<DataResource>> dataResources;

    @MockComponent
    private DataResource archiverDataResource;

    @MockComponent
    private DataResource archiverLogsDataResource;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerIdentifier serverIdentifier;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private Logger logger;

    @Test
    void getFile() throws Exception
    {
        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        when(archiverDataResource.getByteData("input")).thenReturn(new byte[] { 2 });

        assertArrayEquals(new byte[] { 2 }, importantFilesManager.getFile("data_resource_identifier", "input"));
    }

    @Test
    void getFileResourceNotFound()
    {
        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFile("data_resource_identifier_invalid", "input");
        });
        assertEquals("Error while processing file content.", exception.getMessage());
    }

    @Test
    void getFileDataResourceError() throws Exception
    {
        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        when(archiverDataResource.getByteData("input")).thenThrow(new IOException("IO Error"));
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFile("data_resource_identifier", "input");
        });
        assertEquals("Error while managing file.", exception.getMessage());
    }

    @Test
    void downloadMultipleFiles() throws Exception
    {
        String[] files = { "data_resource_identifier", LogsDataResource.HINT };
        Map<String, String[]> request = new HashMap<>();
        request.put("files", files);
        request.put("from", new String[] { "" });
        request.put("to", new String[] { "" });
        Map<String, String> filters = new HashMap<>();
        filters.put("from", !Objects.equals(request.get("from")[0], "") ? request.get("from")[0] : null);
        filters.put("to", !Objects.equals(request.get("to")[0], "") ? request.get("to")[0] : null);

        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        dataResourceList.add(archiverLogsDataResource);

        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        when(archiverLogsDataResource.getIdentifier()).thenReturn(LogsDataResource.HINT);

        importantFilesManager.getFilesArchive(request);
        verify(archiverDataResource).addZipEntry(any(ZipOutputStream.class), isNull());
        verify(archiverLogsDataResource).addZipEntry(any(ZipOutputStream.class), eq(filters));
    }

    @Test
    void downloadMultipleFilesNoArchiverFound()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(importantFilesManager, "logger", this.logger);

        String[] files = { "data_resource_identifier_invalid", LogsDataResource.HINT };
        Map<String, String[]> request = new HashMap<>();
        request.put("files", files);
        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFilesArchive(request);
        });

        assertEquals("Error while generating the file archive.", exception.getMessage());
        verify(logger).warn("Error while generating the file archive. Root cause is: [{}]",
            "NullPointerException: ");
    }

    @Test
    void downloadMultipleFilesInvalidRequest()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(importantFilesManager, "logger", this.logger);

        String[] files = { "data_resource_identifier", LogsDataResource.HINT };
        Map<String, String[]> request = new HashMap<>();
        request.put("files", files);
        List<DataResource> dataResourceList = new ArrayList<>();
        dataResourceList.add(archiverDataResource);
        dataResourceList.add(archiverLogsDataResource);

        when(dataResources.get()).thenReturn(dataResourceList);
        when(archiverDataResource.getIdentifier()).thenReturn("data_resource_identifier");
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFilesArchive(request);
        });

        assertEquals("Error while generating the file archive.", exception.getMessage());
        verify(logger).warn("Error while generating the file archive. Root cause is: [{}]", "NullPointerException: ");
    }

    @Test
    void renderTemplate() throws Exception
    {
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        assertEquals("success", importantFilesManager.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void renderTemplateWithRenderingError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(importantFilesManager, "logger", this.logger);
        when(currentServer.getCurrentServer()).thenReturn(null);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenThrow(new Exception("Render failed."));

        assertNull(importantFilesManager.renderTemplate());
        verify(scriptContext).setAttribute("found", false, ScriptContext.ENGINE_SCOPE);
        verify(logger).warn("Failed to render custom template. Root cause is: [{}]", "Exception: Render failed.");
    }
}
