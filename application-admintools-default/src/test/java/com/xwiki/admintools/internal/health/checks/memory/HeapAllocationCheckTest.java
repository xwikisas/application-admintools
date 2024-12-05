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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

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
class HeapAllocationCheckTest
{
    @InjectMockComponents
    private HeapAllocationCheck heapAllocationCheck;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private XWikiContext context;

    @MockComponent
    private ManagementFactory managementFactory;

    @Mock
    private MemoryMXBean memory;
    @Mock
    private MemoryUsage memoryUsage;

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
        when(xwiki.maxMemory()).thenReturn((long) (ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getInit()));

        assertEquals("adminTools.dashboard.healthcheck.memory.allocation.info",
            heapAllocationCheck.check().getMessage());
    }

    @Test
    void checkInitialHeapDifferent()
    {
        when(xwiki.maxMemory()).thenReturn((long) 1);

        assertEquals("adminTools.dashboard.healthcheck.memory.allocation.warn",
            heapAllocationCheck.check().getMessage());
        assertEquals("To improve performance -Xms  and -Xmx memory allocation should be identical.",
            logCapture.getMessage(0));
    }
}
