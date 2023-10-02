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
import static org.mockito.Mockito.mock;
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
    private ScriptContext scriptContextMock;

    @BeforeAll
    static void initialize()
    {
        // Prepare expected json
        defaultJson = new HashMap<>();
        defaultJson.put("PWD", System.getenv("PWD"));
        defaultJson.put("LANG", System.getenv("LANG"));
        defaultJson.put("activeEncoding", "wiki_encoding");
        defaultJson.put("configurationEncoding", "configuration_encoding");
        defaultJson.put("fileEncoding", "file_encoding");

        // Set system properties that will be used
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

    // Mock environment info
    @Test
    void getDataAsJSONSuccess() throws Exception
    {
        // Mock xwiki security info
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenReturn("configuration_encoding");

        assertEquals(defaultJson, securityDataProvider.getDataAsJSON());
    }

    @Test
    void provideDataWithSuccessfulExecution() throws Exception
    {
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenReturn("configuration_encoding");

        // Mock the renderer
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContextMock);
        when(templateManager.render(templatePath)).thenReturn("success");

        Map<String, String> json = new HashMap<>(defaultJson);
        json.put("serverFound", "true");

        // Verify the result and method invocations
        assertEquals("success", securityDataProvider.getRenderedData());
        verify(scriptContextMock).setAttribute(SecurityDataProvider.HINT.toLowerCase(), json, ScriptContext.ENGINE_SCOPE);
    }

    @Test
    void provideDataWithCaughtError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(securityDataProvider, "logger", this.logger);
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWiki()).thenReturn(wiki);
        when(wiki.getEncoding()).thenReturn("wiki_encoding");
        when(configurationSource.getProperty("xwiki.encoding", String.class)).thenThrow(
            new NullPointerException("ConfigurationSourceNotFound"));

        // Mock the renderer
        ScriptContext scriptContextMock = mock(ScriptContext.class);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContextMock);
        when(templateManager.render(templatePath)).thenReturn("fail");

        Map<String, String> json = new HashMap<>();
        json.put("serverFound", "false");

        // Verify the result and method invocations
        assertEquals("fail", securityDataProvider.getRenderedData());
        verify(this.logger).warn(
            "Exception: Failed to generate the instance security data. Traceback error: Exception: Failed to generate xwiki security info: NullPointerException: ConfigurationSourceNotFound");
        verify(scriptContextMock).setAttribute(SecurityDataProvider.HINT.toLowerCase(), json, ScriptContext.ENGINE_SCOPE);
    }
}
