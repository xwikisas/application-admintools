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
package com.xwiki.admintools.internal.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.util.DefaultFileOperations;
import com.xwiki.admintools.internal.util.DefaultTemplateRender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConfigurationDataProvider}
 *
 * @version $Id$
 */
@ComponentTest
public class ConfigurationDataProviderTest
{
    @InjectMocks
    private ConfigurationDataProvider configurationDataProvider;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private DefaultFileOperations fileOperations;

    @MockComponent
    private DefaultTemplateRender defaultTemplateRender;

    @Mock
    private Logger logger;

    @Test
    public void getIdentifierTest()
    {
        assertEquals(ConfigurationDataProvider.HINT, configurationDataProvider.getIdentifier());
    }

    @Test
    public void getJavaVersionTest()
    {
        System.setProperty("java.version", "java_version");
        assertEquals("java_version", configurationDataProvider.getJavaVersion());
        System.clearProperty("java.version");
    }

    @Test
    public void identifyDBTest() throws InitializationException
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:mysql://");
        configurationDataProvider.initialize();
        assertEquals("MySQL", configurationDataProvider.identifyDB());
    }

    @Test
    public void identifyDBTestNotSupported() throws InitializationException
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:notSupportedDB://");
        configurationDataProvider.initialize();

        Assertions.assertNotEquals("MySQL", configurationDataProvider.identifyDB());
    }

    @Test
    public void testProvideJsonWithSuccessfulExecution()
    {
        // Mock the behavior of CurrentServer to return a valid ServerIdentifier
        ServerIdentifier serverIdentifierMock = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifierMock);
        when(serverIdentifierMock.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverIdentifierMock.getServerCfgPath()).thenReturn("server_config_folder_path");

        // DB mock
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:mysql");

        // Mock template render
        when(serverIdentifierMock.getComponentHint()).thenReturn("test_server");
        Map<String, String> json = configurationDataProvider.generateJson();

        // Verify the result and method invocations
        assertEquals(json, configurationDataProvider.generateJson());
    }

    @Test
    public void testProvideJsonWithErrorExecution()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        assertNull(configurationDataProvider.generateJson());
    }

    @Test
    public void testProvideDataWithSuccessfulExecution() throws InitializationException
    {
        // Mock the behavior of CurrentServer to return a valid ServerIdentifier
        ServerIdentifier serverIdentifierMock = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifierMock);
        when(serverIdentifierMock.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverIdentifierMock.getServerCfgPath()).thenReturn("server_config_folder_path");

        // DB mock
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:mysql://default");
        configurationDataProvider.initialize();
        // Mock template render
        when(serverIdentifierMock.getComponentHint()).thenReturn("test_server");
        Map<String, String> json = configurationDataProvider.generateJson();
        json.put("serverFound", "found");

        when(defaultTemplateRender.getRenderedTemplate("data/configurationTemplate.vm", json,
            ConfigurationDataProvider.HINT)).thenReturn("success");

        // Verify the result and method invocations
        assertEquals("success", configurationDataProvider.provideData());
    }

    @Test
    public void testGetDataWithErrorExecution()
    {
        when(logger.isWarnEnabled()).thenReturn(true);

        // Verify the json generator fails
        assertNull(configurationDataProvider.generateJson());

        // Mock the error message
        when(currentServer.getSupportedServers()).thenReturn(Arrays.asList("serverTest1", "serverTest2"));

        // Generate the expected result
        Map<String, String> json = new HashMap<>();
        json.put("serverFound", null);
        json.put("supportedServers", Arrays.asList("serverTest1", "serverTest2").toString());

        when(defaultTemplateRender.getRenderedTemplate("data/configurationTemplate.vm", json,
            ConfigurationDataProvider.HINT)).thenReturn("fail");

        // Verify that the method fails
        assertEquals("fail", configurationDataProvider.provideData());
    }
}
