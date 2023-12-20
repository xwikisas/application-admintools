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
package com.xwiki.admintools.internal.health.checks.memory;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class MemoryHealthCheckTest
{
    @InjectMockComponents
    private MemoryHealthCheck memoryHealthCheck;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private XWikiContext context;

    @Mock
    private XWiki xwiki;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void beforeEach()
    {
        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWiki()).thenReturn(xwiki);
    }

    @Test
    void check()
    {
        when(xwiki.maxMemory()).thenReturn((long) (8 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));

        assertEquals("adminTools.dashboard.healthcheck.memory.info", memoryHealthCheck.check().getMessage());
    }

    @Test
    void checkInsufficientMaxMemory()
    {
        when(xwiki.maxMemory()).thenReturn((long) (0.8 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (0.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.2 * Math.pow(1024, 3)));

        assertEquals("adminTools.dashboard.healthcheck.memory.maxcapacity.error",
            memoryHealthCheck.check().getMessage());
        assertEquals("JVM memory is less than 1024MB. Currently: [819.2]", logCapture.getMessage(0));
    }

    @Test
    void checkCriticalFreeMemory()
    {
        when(xwiki.maxMemory()).thenReturn((long) (1.7 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.2 * Math.pow(1024, 3)));

        assertEquals("adminTools.dashboard.healthcheck.memory.free.error", memoryHealthCheck.check().getMessage());
        assertEquals("JVM instance has only [409.6001]MB free memory left!", logCapture.getMessage(0));
    }

    @Test
    void checkLowFreeMemory()
    {
        when(xwiki.maxMemory()).thenReturn((long) (2 * Math.pow(1024, 3)));
        when(xwiki.totalMemory()).thenReturn((long) (1.5 * Math.pow(1024, 3)));
        when(xwiki.freeMemory()).thenReturn((long) (0.2 * Math.pow(1024, 3)));

        assertEquals("adminTools.dashboard.healthcheck.memory.free.warn", memoryHealthCheck.check().getMessage());
        assertEquals("Instance memory is running low. Currently only [716.80005]MB free left.",
            logCapture.getMessage(0));
    }
}
