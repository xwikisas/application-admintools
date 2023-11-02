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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link SecurityDataProvider}
 *
 * @version $Id$
 */
@ComponentTest
public class SecurityDataProviderTest
{
    static Map<String, String> defaultJson;

    private final String templatePath = "securityTemplate.vm";

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @InjectMockComponents
    private SecurityDataProvider securityDataProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Mock
    private Logger logger;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private ScriptContext scriptContext;

    @BeforeAll
    static void beforeAll()
    {
        // Prepare expected json.
        defaultJson = new HashMap<>();
        defaultJson.put("PWD", System.getenv("PWD"));
        defaultJson.put("LANG", System.getenv("LANG"));
        defaultJson.put("activeEncoding", "wiki_encoding");
        defaultJson.put("configurationEncoding", "configuration_encoding");
        defaultJson.put("fileEncoding", "file_encoding");

        // Set system properties that will be used.
        System.setProperty("file.encoding", "file_encoding");
    }

    @AfterAll
    static void afterAll()
    {
        System.clearProperty("file.encoding");
    }

    @Test
    void getIdentifier()
    {
        assertEquals(SecurityDataProvider.HINT, securityDataProvider.getIdentifier());
    }

    @Test
    void generateJsonThrowErrorConfigurationSourceNotInitialised()
    {
        doThrow(new NullPointerException("ConfigurationSourceNotFound")).when(configurationSource)
            .getProperty("xwiki.encoding", String.class);
        assertThrows(NullPointerException.class, () -> configurationSource.getProperty("xwiki.encoding", String.class));
    }

    @Test
    void getDataAsJSONSuccess() throws Exception
    {
        // Mock xwiki security info.
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenReturn("configuration_encoding");

        assertEquals(defaultJson, securityDataProvider.getDataAsJSON());
    }

    @Test
    void getRenderedDataWithSuccessfulExecution() throws Exception
    {
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenReturn("configuration_encoding");

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("serverFound", "true");

        // Verify the result and method invocations.
        assertEquals("success", securityDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(SecurityDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataWithCaughtError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(securityDataProvider, "logger", this.logger);
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenThrow(
            new NullPointerException("ConfigurationSourceNotFound"));

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenReturn("success");

        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        // Verify the result and method invocations.
        assertEquals("success", securityDataProvider.getRenderedData());
        verify(scriptContext).setAttribute(SecurityDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void getRenderedDataWithRenderingError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(securityDataProvider, "logger", this.logger);
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenThrow(
            new NullPointerException("ConfigurationSourceNotFound"));

        // Mock the renderer.
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(templateManager.render(templatePath)).thenThrow(new Exception("Render failed."));

        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        // Verify the result and method invocations.
        assertEquals(null, securityDataProvider.getRenderedData());
        verify(this.logger).warn("Failed to generate the instance security data. Root cause is: [{}]",
            "NullPointerException: ConfigurationSourceNotFound");
        verify(this.logger).warn("Failed to render custom template. Root cause is: [{}]", "Exception: Render failed.");
        verify(scriptContext).setAttribute(SecurityDataProvider.HINT, json, ScriptContext.ENGINE_SCOPE);
    }
}
