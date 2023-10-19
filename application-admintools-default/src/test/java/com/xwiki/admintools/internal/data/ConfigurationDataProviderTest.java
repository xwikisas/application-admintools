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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    XWikiHibernateBaseStore baseStore;

    @Mock
    DatabaseMetaData metaData;

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

    @Mock
    private XWikiHibernateStore xWikiStoreInterface;

    @Mock
    private XWikiCacheStoreInterface xWikiCacheStore;

    @BeforeAll
    static void setUp()
    {
        // Prepare expected json.
        defaultJson = new HashMap<>();
        defaultJson.put("database", "MySQL - x.y.z");
        defaultJson.put("osVersion", "test_os_version");
        defaultJson.put("javaVersion", "used_java_version");
        defaultJson.put("osArch", "test_os_arch");
        defaultJson.put("tomcatConfPath", "server_config_folder_path");
        defaultJson.put("xwikiCfgPath", "xwiki_config_folder_path");
        defaultJson.put("usedServer", "test_server_name - test_server_version");
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
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(serverIdentifier.getXwikiCfgFolderPath()).thenReturn("xwiki_config_folder_path");
        when(serverIdentifier.getServerCfgPath()).thenReturn("server_config_folder_path");
        when(serverIdentifier.getServerNameAndVersion()).thenReturn("test_server_name - test_server_version");
        when(wiki.getStore()).thenReturn(xWikiCacheStore);
        when(((XWikiCacheStoreInterface) xWikiCacheStore).getStore()).thenReturn(xWikiStoreInterface);
    }

    @Test
    void getIdentifier()
    {
        assertEquals(ConfigurationDataProvider.HINT, configurationDataProvider.getIdentifier());
    }

    @Test
    void getDataAsJsonMetaDataFail() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);
        when(wiki.getStore()).thenReturn(null);

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
        verify(this.logger).warn("Failed to get database metadata. Root cause is: [{}]", "NullPointerException: ");
    }

    @Test
    void getDataAsJsonMetaDataNotFound() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);
        when(((XWikiCacheStoreInterface) xWikiCacheStore).getStore()).thenReturn(new XWikiCacheStore());

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
        verify(this.logger).warn("Failed to get database metadata.");
    }

    @Test
    void getDataAsJsonMetaDataError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        baseStore = (XWikiHibernateBaseStore) xWikiStoreInterface;
        when(baseStore.getDatabaseMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenThrow(new SQLException("metaData-err"));

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
        verify(this.logger).warn("Failed to compute database name and version. Root cause is: [{}]",
            "SQLException: metaData-err");
    }

    @Test
    void getDataAsJsonWithSuccessfulExecution() throws Exception
    {
        baseStore = (XWikiHibernateBaseStore) xWikiStoreInterface;
        when(baseStore.getDatabaseMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("x.y.z");

        Map<String, String> json = new HashMap<>(defaultJson);

        assertEquals(json, configurationDataProvider.getDataAsJSON());
    }

    @Test
    void getDataAsJsonWithErrorExecution()
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
    void getRenderedDataWithSuccessfulExecution() throws Exception
    {
        baseStore = (XWikiHibernateBaseStore) xWikiStoreInterface;
        when(baseStore.getDatabaseMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(metaData.getDatabaseProductVersion()).thenReturn("x.y.z");

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
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);

        when(((XWikiCacheStoreInterface) xWikiCacheStore).getStore()).thenReturn(new XWikiCacheStore());

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("database", null);
        json.put("serverFound", "true");

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        // Verify the result and method invocations.
        assertEquals("success", configurationDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
        verify(this.logger).warn("Failed to get database metadata.");
    }

    @Test
    void getRenderedDataWithFailedJsonGenerate() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(configurationDataProvider, "logger", this.logger);
        when(currentServer.getCurrentServer()).thenReturn(null);

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

        verify(this.logger, times(2)).warn("Failed to retrieve used server. Server not found.");
        verify(this.logger).warn("Failed to render custom template. Root cause is: [{}]", "Exception: Render failed.");
        verify(scriptContext).setAttribute(ConfigurationDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }
}
