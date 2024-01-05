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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.inject.Named;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentManager;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ImportantFilesManager}
 *
 * @version $Id$
 */
@ComponentTest
class ImportantFilesManagerTest
{
    private final String templatePath = "filesSectionTemplate.vm";

    private final Map<String, String[]> params = Map.of("input", new String[] { "good_input" });

    @InjectMockComponents
    private ImportantFilesManager importantFilesManager;

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

    @MockComponent
    @Named("context")
    private ComponentManager contextComponentManager;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp() throws Exception
    {
        when(archiverDataResource.getByteData(params)).thenReturn(new byte[] { 2 });
        when(contextComponentManager.getInstance(DataResource.class, "data_resource_identifier")).thenReturn(
            archiverDataResource);
    }

    @Test
    void getFile() throws Exception
    {
        assertArrayEquals(new byte[] { 2 }, importantFilesManager.getFile("data_resource_identifier", params));
    }

    @Test
    void getFileResourceNotFound() throws Exception
    {
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFile("data_resource_identifier_invalid", params);
        });
        assertEquals("Could not find a DataResource implementation for [data_resource_identifier_invalid].",
            exception.getMessage());
    }

    @Test
    void getFileDataResourceError() throws Exception
    {
        when(archiverDataResource.getByteData(params)).thenThrow(new IOException("IO Error"));
        Exception exception = assertThrows(Exception.class, () -> {
            importantFilesManager.getFile("data_resource_identifier", params);
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
        Map<String, String[]> filters = new HashMap<>();
        filters.put("from", new String[] { "" });
        filters.put("to", new String[] { "" });

        when(contextComponentManager.getInstance(DataResource.class, LogsDataResource.HINT)).thenReturn(
            archiverLogsDataResource);

        importantFilesManager.getFilesArchive(request);
        verify(archiverDataResource).addZipEntry(any(ZipOutputStream.class), any());
        verify(archiverLogsDataResource).addZipEntry(any(ZipOutputStream.class), any());
    }

    @Test
    void downloadMultipleFilesNoArchiverFound() throws Exception
    {
        String[] files = { "data_resource_identifier_invalid", LogsDataResource.HINT };
        Map<String, String[]> request = new HashMap<>();
        request.put("files", files);

        importantFilesManager.getFilesArchive(request);
        verify(archiverDataResource, never()).addZipEntry(any(ZipOutputStream.class), any());
    }

    @Test
    void downloadMultipleFilesInvalidRequest() throws Exception
    {
        String[] files = { "invalid_hint1", "invalid_hint2" };
        Map<String, String[]> request = new HashMap<>();
        request.put("files", files);
        when(contextComponentManager.getInstance(DataResource.class, LogsDataResource.HINT)).thenReturn(
            archiverLogsDataResource);
        importantFilesManager.getFilesArchive(request);
        verify(archiverDataResource, never()).addZipEntry(any(ZipOutputStream.class), any());
        verify(archiverLogsDataResource, never()).addZipEntry(any(ZipOutputStream.class), any());
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
        verify(logger).warn("Failed to render [{}] template. Root cause is: [{}]", "filesSectionTemplate.vm",
            "Exception: Render failed.");
    }
}
