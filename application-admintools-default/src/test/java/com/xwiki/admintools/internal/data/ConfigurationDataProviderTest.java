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

import javax.script.ScriptContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.util.DefaultFileOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
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
    static Map<String, String> json;

    private final String templatePath = "configurationTemplate.vm";

    @InjectMockComponents
    private ConfigurationDataProvider configurationDataProvider;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private DefaultFileOperations fileOperations;

    @Mock
    private Logger logger;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @BeforeEach
    public void setUp() throws InitializationException
    {
        configurationDataProvider.initialize();
    }

    @BeforeAll
    public static void initialize()
    {
        // Prepare expected json
        json = new HashMap<>();
        json.put("database", "MySQL");
        json.put("osVersion", "test_os_version");
        json.put("javaVersion", "used_java_version");
        json.put("osArch", "test_os_arch");
        json.put("tomcatConfPath", "server_config_folder_path");
        json.put("xwikiCfgPath", "xwiki_config_folder_path");
        json.put("usedServer", "test_server");
        json.put("osName", "test_os_name");

        // Set system properties that will be used
        System.setProperty("java.version", "used_java_version");
        System.setProperty("os.name", "test_os_name");
        System.setProperty("os.version", "test_os_version");
        System.setProperty("os.arch", "test_os_arch");
    }

    @AfterAll
    public static void afterAll()
    {
        System.clearProperty("os.name");
        System.clearProperty("os.version");
        System.clearProperty("os.arch");
        System.clearProperty("java.version");
    }

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
    public void identifyDBTest() throws Exception
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(fileOperations.hasNextLine()).thenReturn(true, true);
        when(fileOperations.nextLine()).thenReturn("test", "<property name=\"connection.url\">jdbc:mysql://");
        assertEquals("MySQL", configurationDataProvider.identifyDB());
    }

    @Test
    public void identifyDBTestNotSupported() throws Exception
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:notSupportedDB://");

        Assertions.assertEquals("Database not supported.", configurationDataProvider.identifyDB());
    }

    @Test
    public void identifyDBTestFileNotFound() throws Exception
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path/");

        doThrow(new NullPointerException("CONFIGURATION_FILE_NOT_FOUND")).when(fileOperations).initializeScanner();

        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:notSupportedDB://");

        assertThrows(Exception.class, () -> configurationDataProvider.identifyDB());
    }

    @Test
    public void identifyDBTestCurrentServerNotFound() throws Exception
    {
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(mockServerIdentifier);
        when(mockServerIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path/");

        doThrow(new NullPointerException("CURRENT_SERVER_NOT_FOUND")).when(currentServer).getCurrentServer();

        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:notSupportedDB://");

        assertThrows(Exception.class, () -> configurationDataProvider.identifyDB());
    }

    @Test
    public void testProvideJsonWithSuccessfulExecution() throws Exception
    {
        // Mock the behavior of CurrentServer to return a valid ServerIdentifier
        ServerIdentifier serverIdentifierMock = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifierMock);
        when(serverIdentifierMock.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverIdentifierMock.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifierMock.getComponentHint()).thenReturn("test_server");

        // DB mock
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:mysql://");
        // Mock java version
        System.setProperty("java.version", "used_java_version");

        // Verify the result and method invocations
        assertEquals(json, configurationDataProvider.provideJson());
    }

    @Test
    public void testProvideJsonWithErrorExecution() throws Exception
    {
        assertThrows(Exception.class, () -> configurationDataProvider.provideJson());
    }

    @Test
    public void testProvideDataWithSuccessfulExecution() throws Exception
    {
        // Mock the behavior of CurrentServer to return a valid ServerIdentifier
        ServerIdentifier serverIdentifierMock = mock(ServerIdentifier.class);
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifierMock);
        when(serverIdentifierMock.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverIdentifierMock.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifierMock.getComponentHint()).thenReturn("test_server");

        // DB mock
        when(fileOperations.hasNextLine()).thenReturn(true);
        when(fileOperations.nextLine()).thenReturn("<property name=\"connection.url\">jdbc:mysql://");

        // Mock the renderer
        ScriptContext scriptContextMock = mock(ScriptContext.class);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContextMock);
        when(templateManager.render(templatePath)).thenReturn("success");

        // Verify the result and method invocations
        assertEquals("success", configurationDataProvider.provideData());
    }

    @Test
    public void testProvideDataWithErrorExecution() throws Exception
    {

        when(logger.isWarnEnabled()).thenReturn(true);

        // Verify the json generator fails

        // Mock the error message
        when(currentServer.getSupportedServers()).thenReturn(Arrays.asList("serverTest1", "serverTest2"));

        // Mock the renderer
        ScriptContext scriptContextMock = mock(ScriptContext.class);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContextMock);
        when(templateManager.render(templatePath)).thenReturn("fail");

        // Verify that the method fails
        assertEquals("fail", configurationDataProvider.provideData());
        assertThrows(Exception.class, () -> configurationDataProvider.provideJson());
    }
}
