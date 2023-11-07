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
package com.xwiki.admintools.internal.health.checks;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class MemoryHealthCheckTest
{
    @MockComponent
    private static ContextualLocalizationManager localization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @MockComponent
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @InjectMockComponents
    private MemoryHealthCheck memoryHealthCheck;

    @Mock
    private Logger logger;

    @BeforeComponent
    static void setUp() throws Exception
    {
        when(localization.getTranslationPlain("adminTools.dashboard.healthcheck.memory.cache.info")).thenReturn(
            "Cache status OK");
        when(localization.getTranslationPlain("adminTools.dashboard.healthcheck.memory.info")).thenReturn(
            "Memory status OK");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.memory.cache.null")).thenReturn(
            "Store cache capacity is set at 500. Check Cache recommendations in solutions.");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.memory.cache.low")).thenReturn(
            "Store cache capacity is set at [{}]. Check Cache recommendations in solutions.");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.memory.free.error")).thenReturn(
            "Your JVM instance has only [{}]MB free memory left! Consult the solution link for support!");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.memory.free.warn")).thenReturn(
            "Your instance memory is running low. Currently only [{}]MB free left.");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.memory.maxcapacity.error")).thenReturn(
            "JVM memory is less than 1024MB. Currently: [{}]MB");
    }

    @BeforeEach
    void beforeEach()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(memoryHealthCheck, "logger", this.logger);

        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
        when(configurationSource.getProperty("xwiki.store.cache.capacity")).thenReturn("1000");
    }

    @Test
    void check()
    {
        when(xwiki.maxMemory()).thenReturn((long) (8 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));

        assertNull(memoryHealthCheck.check().getErrorMessage());
        verify(logger).info("Cache status OK");
        verify(logger).info("Memory status OK");
    }

    @Test
    void checkCacheIssues()
    {
        when(xwiki.maxMemory()).thenReturn((long) (8 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));
        when(configurationSource.getProperty("xwiki.store.cache.capacity")).thenReturn("400");
        assertEquals("There are memory issues!", memoryHealthCheck.check().getErrorMessage());
        verify(logger).warn("Store cache capacity is set at [{}]. Check Cache recommendations in solutions.", "400");
        verify(logger).info("Memory status OK");
    }

    @Test
    void checkMaxMemoryIssues()
    {
        when(xwiki.maxMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));

        assertEquals("There are memory issues!", memoryHealthCheck.check().getErrorMessage());
        verify(logger).info("Cache status OK");
        verify(logger).error("JVM memory is less than 1024MB. Currently: [{}]MB", 512.0f);
    }

    @Test
    void checkCriticalFreeMemory()
    {
        when(xwiki.maxMemory()).thenReturn((long) (1.7 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.2 * Math.pow(1024, 3)));

        assertEquals("There are memory issues!", memoryHealthCheck.check().getErrorMessage());
        verify(logger).info("Cache status OK");
        verify(logger).error(
            "Your JVM instance has only [{}]MB free memory left! Consult the solution link for support!", 409.6001f);
    }

    @Test
    void checkLowFreeMemory()
    {
        when(xwiki.maxMemory()).thenReturn((long) (2 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.2 * Math.pow(1024, 3)));

        assertNull(memoryHealthCheck.check().getErrorMessage());
        verify(logger).info("Cache status OK");
        verify(logger).warn("Your instance memory is running low. Currently only [{}]MB free left.", 716.80005f);
    }
}
