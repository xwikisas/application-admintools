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
package com.xwiki.admintools.internal.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.internal.download.DownloadManager;
import com.xwiki.admintools.internal.download.resources.LogsDataResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultAdminToolsResource}
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultAdminToolsResourceTest
{
    XWikiContext xWikiContext;

    @Mock
    XWikiRequest xWikiRequest;

    @InjectMockComponents
    private DefaultAdminToolsResource defaultAdminToolsResource;

    @MockComponent
    private DownloadManager downloadManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private DocumentReference user;

    @Mock
    private WikiReference wikiReference;

    @Mock
    private Logger logger;

    @BeforeComponent
    void beforeComponent()
    {
        // We need this before component because XWikiResource is calling the context in Initialize call.
        this.xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xWikiContext);
    }

    @BeforeEach
    void setUp()
    {
        when(contextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getUserReference()).thenReturn(user);
        when(xWikiContext.getWikiReference()).thenReturn(wikiReference);
        when(authorizationManager.hasAccess(Right.ADMIN, user, wikiReference)).thenReturn(true);
        when(xWikiContext.getRequest()).thenReturn(xWikiRequest);
    }

    @Test
    void getFile() throws Exception
    {
        when(downloadManager.getFile("resource_hint", null)).thenReturn(new byte[] { 2 });

        assertEquals(200, defaultAdminToolsResource.getFile("resource_hint").getStatus());
    }

    @Test
    void getFileNotFound() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(downloadManager.getFile("resource_hint", null)).thenThrow(new IOException("FILE NOT FOUND"));

        assertEquals(404, defaultAdminToolsResource.getFile("resource_hint").getStatus());
        verify(logger).warn("Could not find file from DataResource[{}]. Root cause: [{}]", "resource_hint",
            "IOException: FILE NOT FOUND");
    }

    @Test
    void getFileDownloadManagerError()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        Exception exception = assertThrows(Exception.class, () -> {
            this.defaultAdminToolsResource.getFile("resource_hint");
        });
        assertEquals("HTTP 500 Internal Server Error", exception.getMessage());
        verify(logger).warn("Failed to get data from DataResource [{}]. Root cause: [{}]", "resource_hint",
            "NullPointerException: ");
    }

    @Test
    void getFileNotAdmin()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(authorizationManager.hasAccess(Right.ADMIN, user, wikiReference)).thenReturn(false);
        Exception exception = assertThrows(Exception.class, () -> {
            this.defaultAdminToolsResource.getFile("resource_hint");
        });
        assertEquals("HTTP 401 Unauthorized", exception.getMessage());
        verify(logger).warn("Failed to get file from DataResource [{}] due to restricted rights.", "resource_hint");
    }

    @Test
    void getFiles() throws Exception
    {
        Map<String, String[]> formParameters = new HashMap<>();
        when(xWikiRequest.getParameterMap()).thenReturn(formParameters);
        when(downloadManager.downloadMultipleFiles(formParameters)).thenReturn(new byte[] { 2 });

        assertEquals(200, defaultAdminToolsResource.getFiles().getStatus());
    }

    @Test
    void getFilesDownloadManagerError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        Map<String, String[]> formParameters = new HashMap<>();
        when(xWikiRequest.getParameterMap()).thenReturn(formParameters);
        when(downloadManager.downloadMultipleFiles(formParameters)).thenThrow(
            new Exception("DOWNLOAD MANAGER EXCEPTION"));
        Exception exception = assertThrows(Exception.class, () -> {
            defaultAdminToolsResource.getFiles();
        });
        assertEquals("HTTP 500 Internal Server Error", exception.getMessage());
        verify(logger).warn("Failed to download files. Root cause: [{}]", "Exception: DOWNLOAD MANAGER EXCEPTION");
    }

    @Test
    void getFilesNotAdmin()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(authorizationManager.hasAccess(Right.ADMIN, user, wikiReference)).thenReturn(false);
        Exception exception = assertThrows(Exception.class, () -> {
            this.defaultAdminToolsResource.getFiles();
        });
        assertEquals("HTTP 401 Unauthorized", exception.getMessage());
        verify(logger).warn("Failed to get files due to restricted rights.");
    }

    @Test
    void getLastLogs() throws Exception
    {
        when(downloadManager.getFile(LogsDataResource.HINT, "30")).thenReturn(new byte[] { 2 });
        when(xWikiRequest.getParameter("noLines")).thenReturn("30");
        assertEquals(200, defaultAdminToolsResource.getLastLogs().getStatus());
    }

    @Test
    void getLastDownloadManagerError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(downloadManager.getFile(LogsDataResource.HINT, "1000")).thenThrow(new IOException("FILE NOT FOUND"));

        assertEquals(404, defaultAdminToolsResource.getLastLogs().getStatus());
        verify(logger).warn("Could not retrieve logs from server. Root cause: [{}]", "IOException: FILE NOT FOUND");
    }

    @Test
    void getLastLogsDownloadManagerError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(downloadManager.getFile(LogsDataResource.HINT, "1000")).thenThrow(new Exception("INTERNAL ERROR"));
        Exception exception = assertThrows(Exception.class, () -> {
            this.defaultAdminToolsResource.getLastLogs();
        });
        assertEquals("HTTP 500 Internal Server Error", exception.getMessage());
        verify(logger).warn("Failed to get logs. Root cause: [{}]", "Exception: INTERNAL ERROR");
    }

    @Test
    void getLastLogsNotAdmin()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(defaultAdminToolsResource, "logger", this.logger);

        when(authorizationManager.hasAccess(Right.ADMIN, user, wikiReference)).thenReturn(false);
        Exception exception = assertThrows(Exception.class, () -> {
            this.defaultAdminToolsResource.getLastLogs();
        });
        assertEquals("HTTP 401 Unauthorized", exception.getMessage());
        verify(logger).warn("Failed to get the logs due to restricted rights.");
    }
}
