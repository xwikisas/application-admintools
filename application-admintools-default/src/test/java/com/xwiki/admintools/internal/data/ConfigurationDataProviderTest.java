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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConfigurationDataProvider}
 *
 * @version $Id$
 */
@ComponentTest
public class ConfigurationDataProviderTest
{
    static Map<String, String> defaultJson;

    private final String templatePath = "configurationTemplate.vm";

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @InjectMockComponents
    private ConfigurationDataProvider configurationDataProvider;

    @MockComponent
    private CurrentServer currentServer;

    @Mock
    private Logger logger;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Mock
    private ServerIdentifier serverIdentifier;

    @Mock
    private ScriptContext scriptContext;

    @XWikiTempDir
    private File tmpDir;

    @BeforeAll
    static void setUp()
    {
        // Prepare expected json.
        defaultJson = new HashMap<>();
        defaultJson.put("database", "MySQL");
        defaultJson.put("osVersion", "test_os_version");
        defaultJson.put("javaVersion", "used_java_version");
        defaultJson.put("osArch", "test_os_arch");
        defaultJson.put("tomcatConfPath", "server_config_folder_path");
        defaultJson.put("xwikiCfgPath", "xwiki_config_folder_path");
        defaultJson.put("usedServer", "test_server");
        defaultJson.put("osName", "test_os_name");
        defaultJson.put("xwikiVersion", "xwiki_version");

        // Set system properties that will be used.
        System.setProperty("java.version", "used_java_version");
        System.setProperty("os.name", "test_os_name");
        System.setProperty("os.version", "test_os_version");
        System.setProperty("os.arch", "test_os_arch");
    }

    @AfterAll
    static void afterAll()
    {
        System.clearProperty("os.name");
        System.clearProperty("os.version");
        System.clearProperty("os.arch");
        System.clearProperty("java.version");
    }

    @BeforeEach
    void beforeEach()
    {
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getVersion()).thenReturn("xwiki_version");
    }

    @Test
    void getIdentifier()
    {
        assertEquals(ConfigurationDataProvider.HINT, configurationDataProvider.getIdentifier());
    }

    @Test
    void getDataAsJsonDBNotSupported() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        // Mock file behaviour.
        File configDirectory = new File(tmpDir, "xwiki_cfg_folder");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();
        String cfgFolderPath = configDirectory.getAbsolutePath() + "/";
        File testFile = new File(configDirectory, "hibernate.cfg.xml");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        // Mock the behavior of CurrentServer to return a valid ServerIdentifier.
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgFolderPath);
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getComponentHint()).thenReturn("test_server");

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);
        json.put("xwikiCfgPath", cfgFolderPath);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
        verify(this.logger).warn("Failed to find database. Used database may not be supported!");
    }

    @Test
    void getDataAsJsonIdentifyDBFileNotFound() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        File configDirectory = new File(tmpDir, "xwiki_cfg_folder");
        configDirectory.mkdir();
        String cfgFolderPath = configDirectory.getAbsolutePath() + "/";

        // Mock the behavior of CurrentServer to return a valid ServerIdentifier.
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgFolderPath);
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getComponentHint()).thenReturn("test_server");

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);
        json.put("xwikiCfgPath", cfgFolderPath);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
        verify(this.logger).warn("Failed to open database configuration file. Root cause is: [{}]",
            "FileNotFoundException: " + cfgFolderPath + "hibernate.cfg.xml (No such file or directory)");
        configDirectory.delete();
    }

    @Test
    void getDataAsJsonIdentifyDBCurrentServerNotFound()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);
        when(currentServer.getCurrentServer()).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> {
            this.configurationDataProvider.getDataAsJSON();
        });
        assertEquals("Failed to generate the instance configuration data.", exception.getMessage());
        verify(this.logger).warn("Failed to retrieve used server. Server not found.");
    }

    @Test
    void getDataAsJsonWithSuccessfulExecution() throws Exception
    {
        // Mock file behaviour.
        File configDirectory = new File(tmpDir, "xwiki_cfg_folder");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();
        String cfgFolderPath = configDirectory.getAbsolutePath() + "/";
        File testFile = new File(configDirectory, "hibernate.cfg.xml");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        writer.append("file content\n");
        writer.append("file content1\n");
        writer.append("file content2\n");
        writer.append("<property name=\"connection.url\">jdbc:mysql://\n");
        writer.append("file content3\n");
        writer.close();

        // Mock the behavior of CurrentServer to return a valid ServerIdentifier.
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgFolderPath);
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getComponentHint()).thenReturn("test_server");

        Map<String, String> supportedDBs = new HashMap<>();
        supportedDBs.put("mysql", "MySQL");
        supportedDBs.put("hsqldb", "HSQLDB");
        supportedDBs.put("mariadb", "MariaDB");
        supportedDBs.put("postgresql", "PostgreSQL");
        supportedDBs.put("oracle", "Oracle");
        when(currentServer.getSupportedDBs()).thenReturn(supportedDBs);

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("xwikiCfgPath", cfgFolderPath);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
    }

    @Test
    void getDataAsJsonWithErrorExecution()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        Exception exception = assertThrows(Exception.class, () -> {
            this.configurationDataProvider.getDataAsJSON();
        });
        assertEquals("Failed to generate the instance configuration data.", exception.getMessage());
        verify(this.logger).warn("Failed to retrieve used server. Server not found.");
    }

    @Test
    void getRenderedDataWithSuccessfulExecution() throws Exception
    {
        // Mock file behaviour.
        File configDirectory = new File(tmpDir, "xwiki_cfg_folder");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();
        String cfgFolderPath = configDirectory.getAbsolutePath() + "/";
        File testFile = new File(configDirectory, "hibernate.cfg.xml");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        writer.append("file content\n");
        writer.append("file content1\n");
        writer.append("file content2\n");
        writer.append("<property name=\"connection.url\">jdbc:mysql://\n");
        writer.append("file content3\n");
        writer.close();

        // Mock the behavior of CurrentServer to return a valid ServerIdentifier.
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgFolderPath);
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getComponentHint()).thenReturn("test_server");

        Map<String, String> supportedDBs = new HashMap<>();
        supportedDBs.put("mysql", "MySQL");
        supportedDBs.put("hsqldb", "HSQLDB");
        supportedDBs.put("mariadb", "MariaDB");
        supportedDBs.put("postgresql", "PostgreSQL");
        supportedDBs.put("oracle", "Oracle");
        when(currentServer.getSupportedDBs()).thenReturn(supportedDBs);

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("serverFound", "true");
        json.put("xwikiCfgPath", cfgFolderPath);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        // Verify the result and method invocations.
        assertEquals("success", configurationDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataWithSuccessfulExecutionButUnsupportedDB() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        // Mock file behaviour.
        File configDirectory = new File(tmpDir, "xwiki_cfg_folder");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();
        String cfgFolderPath = configDirectory.getAbsolutePath() + "/";
        File testFile = new File(configDirectory, "hibernate.cfg.xml");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        // Mock the behavior of CurrentServer to return a valid ServerIdentifier.
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn(cfgFolderPath);
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getComponentHint()).thenReturn("test_server");

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);
        json.put("xwikiCfgPath", cfgFolderPath);
        json.put("serverFound", "true");

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        // Verify the result and method invocations.
        assertEquals("success", configurationDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
        verify(this.logger).warn("Failed to find database. Used database may not be supported!");
    }

    @Test
    void getRenderedDataWithFailedJsonGenerate() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");
        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        assertEquals("success", configurationDataProvider.getRenderedData());
        assertThrows(Exception.class, () -> configurationDataProvider.getDataAsJSON());
        verify(this.logger, times(2)).warn("Failed to retrieve used server. Server not found.");
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataExecutionFail() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenThrow(new Exception("Render failed."));
        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        // Verify that the method fails.
        assertNull(configurationDataProvider.getRenderedData());
        Exception exception = assertThrows(Exception.class, () -> {
            this.configurationDataProvider.getDataAsJSON();
        });
        assertEquals("Failed to generate the instance configuration data.", exception.getMessage());

        verify(this.logger, times(2)).warn("Failed to retrieve used server. Server not found.");
        verify(this.logger).warn("Failed to render custom template. Root cause is: [{}]", "Exception: Render failed.");
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }
}
