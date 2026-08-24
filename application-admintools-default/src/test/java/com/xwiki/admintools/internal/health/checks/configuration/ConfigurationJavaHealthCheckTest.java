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
package com.xwiki.admintools.internal.health.checks.configuration;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class ConfigurationJavaHealthCheckTest
{
    @MockComponent
    @Named(ConfigurationDataProvider.HINT)
    private static DataProvider dataProvider;

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private ConfigurationJavaHealthCheck javaHealthCheck;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @MockComponent
    private XWiki xwiki;

    @BeforeComponent
    void setup()
    {
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWiki()).thenReturn(this.xwiki);
    }

    @AfterAll
    static void afterAll()
    {
        System.clearProperty("java.specification.version");
    }

    @Test
    void check() throws InitializationException
    {
        when(this.xwiki.getVersion()).thenReturn("15.10");
        System.setProperty("java.specification.version", "17.2");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.info", this.javaHealthCheck.check().getMessage());

        when(this.xwiki.getVersion()).thenReturn("16.10.4");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.info", this.javaHealthCheck.check().getMessage());

        when(this.xwiki.getVersion()).thenReturn("17.10.3");
        System.setProperty("java.specification.version", "21.0.11");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.info", this.javaHealthCheck.check().getMessage());

        when(this.xwiki.getVersion()).thenReturn("18.4");
        System.setProperty("java.specification.version", "25.0.4");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.info", this.javaHealthCheck.check().getMessage());
    }

    @Test
    void checkNullJSON() throws InitializationException
    {
        System.setProperty("java.specification.version", "");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.warn", this.javaHealthCheck.check().getMessage());
        assertEquals("Java version not found!", this.logCapture.getMessage(0));
    }

    @Test
    void checkJavaVersionIncompatible() throws InitializationException
    {
        when(this.xwiki.getVersion()).thenReturn("18.4");
        System.setProperty("java.specification.version", "17.2");
        this.javaHealthCheck.initialize();
        assertEquals("adminTools.dashboard.healthcheck.java.error", this.javaHealthCheck.check().getMessage());
        assertEquals("Java version is not compatible with the current XWiki installation!",
            this.logCapture.getMessage(0));
    }
}
