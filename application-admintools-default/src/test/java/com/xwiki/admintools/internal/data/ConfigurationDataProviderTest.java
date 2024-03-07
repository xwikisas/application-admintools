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

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.wikiUsage.UsageDataProvider;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ConfigurationDataProvider}
 *
 * @version $Id$
 */
@ComponentTest
class ConfigurationDataProviderTest
{
    private static Map<String, String> defaultJson;

    private final String templatePath = "configurationTemplate.vm";

    @InjectMockComponents
    private ConfigurationDataProvider configurationDataProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Mock
    private ServerInfo serverInfo;

    @Mock
    private ScriptContext scriptContext;

    @MockComponent
    private UsageDataProvider usageDataProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @Mock
    private Licensor licensor;

    private DocumentReference mainRef =
        new DocumentReference("wiki_id", Arrays.asList("AdminTools", "Code"), "ConfigurationClass");

    @BeforeAll
    static void setUp()
    {
        // Prepare expected json.
        defaultJson = new HashMap<>();
        defaultJson.put("databaseName", "MySQL");
        defaultJson.put("databaseVersion", "x.y.z");
        defaultJson.put("osVersion", "test_os_version");
        defaultJson.put("javaVersion", "used_java_version");
        defaultJson.put("osArch", "test_os_arch");
        defaultJson.put("tomcatConfPath", "server_config_folder_path");
        defaultJson.put("xwikiCfgPath", "xwiki_config_folder_path");
        defaultJson.put("usedServerName", "test_server_name");
        defaultJson.put("usedServerVersion", "test_server_version");
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
        when(xWikiContext.getWikiId()).thenReturn("wiki_id");
        when(licensorProvider.get()).thenReturn(licensor);
        when(licensor.hasLicensure(mainRef)).thenReturn(true);

        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getVersion()).thenReturn("xwiki_version");
        when(currentServer.getCurrentServer()).thenReturn(serverInfo);
        when(serverInfo.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverInfo.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(usageDataProvider.getServerMetadata()).thenReturn(
            Map.of("name", "test_server_name", "version", "test_server_version"));
        when(usageDataProvider.getDatabaseMetadata()).thenReturn(Map.of("name", "MySQL", "version", "x.y.z"));
    }

    @Test
    void getIdentifier()
    {
        assertEquals(ConfigurationDataProvider.HINT, configurationDataProvider.getIdentifier());
    }

    @Test
    void getDataAsJsonDatabaseFail() throws Exception
    {
        when(usageDataProvider.getDatabaseMetadata()).thenReturn(new HashMap<>());
        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("databaseName", null);
        json.put("databaseVersion", null);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
    }

    @Test
    void getDataAsJsonWithSuccessfulExecution() throws Exception
    {
        Map<String, String> json = new HashMap<>(defaultJson);
        assertEquals(json, configurationDataProvider.getDataAsJSON());
    }

    @Test
    void getDataAsJsonWithErrorExecution()
    {
        when(currentServer.getCurrentServer()).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> {
            this.configurationDataProvider.getDataAsJSON();
        });
        assertEquals("Failed to generate the instance configuration data.", exception.getMessage());
    }

    @Test
    void getRenderedDataWithSuccessfulExecution() throws Exception
    {

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("serverFound", "true");

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
        when(usageDataProvider.getDatabaseMetadata()).thenReturn(new HashMap<>());

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("databaseName", null);
        json.put("databaseVersion", null);
        json.put("serverFound", "true");

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        // Verify the result and method invocations.
        assertEquals("success", configurationDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataWithFailedJsonGenerate() throws Exception
    {
        when(currentServer.getCurrentServer()).thenReturn(null);

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");
        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        assertEquals("success", configurationDataProvider.getRenderedData());
        assertThrows(Exception.class, () -> configurationDataProvider.getDataAsJSON());
        assertEquals("Failed to generate the instance configuration data. Root cause is: [NullPointerException: "
            + "Failed to retrieve the current used server, check your configurations.]", logCapture.getMessage(0));
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataExecutionFail() throws Exception
    {
        when(currentServer.getCurrentServer()).thenReturn(null);

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
        assertEquals("Failed to generate the instance configuration data. Root cause is: [NullPointerException: "
            + "Failed to retrieve the current used server, check your configurations.]", logCapture.getMessage(0));
        assertEquals("Failed to render custom template. Root cause is: [Exception: Render failed.]",
            logCapture.getMessage(1));
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataInvalidLicense() throws Exception
    {
        when(licensor.hasLicensure(mainRef)).thenReturn(false);
        when(templateManager.render("licenseError.vm")).thenReturn("invalid license");
        assertEquals("invalid license", configurationDataProvider.getRenderedData());
    }
}
