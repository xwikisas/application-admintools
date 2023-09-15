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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.internal.util.DefaultTemplateRender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link SecurityDataProvider}
 *
 * @version $Id$
 */
@ComponentTest
public class SecurityDataProviderTest
{
    @InjectMockComponents
    private SecurityDataProvider securityDataProvider;
    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private Logger logger;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;



    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private DefaultTemplateRender defaultTemplateRender;

    @Test
    public void getIdentifierTest()
    {
        assertEquals(SecurityDataProvider.HINT, securityDataProvider.getIdentifier());
    }

    @Test
    public void generateJsonTestFail()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        assertNull(securityDataProvider.generateJson());
    }

    @Test
    public void testGetDataWithErrorExecution()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        assertNull(securityDataProvider.generateJson());
        Map<String, String> json = new HashMap<>();

        json.put("serverFound", null);
        when(defaultTemplateRender.getRenderedTemplate("data/securityTemplate.vm", json,
            SecurityDataProvider.HINT)).thenReturn("fail");
        assertEquals("fail", securityDataProvider.provideData());
    }

    @Test
    public void testGenerateJsonSuccess()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getEncoding()).thenReturn("rightEncoding");
        when(this.configurationSource.getProperty("xwiki.encoding", String.class)).thenReturn("right_encoding");
        Map<String, String> json = securityDataProvider.generateJson();

        assertNotNull(json);
    }
}
