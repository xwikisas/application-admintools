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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class CacheMemoryHealthCheckTest
{
    @InjectMockComponents
    private CacheMemoryHealthCheck cacheMemoryHealthCheck;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void check()
    {
        when(configurationSource.getProperty("xwiki.store.cache.capacity")).thenReturn("1000");
        assertEquals("adminTools.dashboard.healthcheck.memory.cache.info", cacheMemoryHealthCheck.check().getMessage());
    }

    @Test
    void checkCacheLow()
    {
        when(configurationSource.getProperty("xwiki.store.cache.capacity")).thenReturn("400");
        assertEquals("adminTools.dashboard.healthcheck.memory.cache.low", cacheMemoryHealthCheck.check().getMessage());
        assertEquals("Store cache capacity is set to [400].", logCapture.getMessage(0));
    }


    @Test
    void checkCacheNotDefined()
    {
        when(configurationSource.getProperty("xwiki.store.cache.capacity")).thenReturn(null);
        assertEquals("adminTools.dashboard.healthcheck.memory.cache.null", cacheMemoryHealthCheck.check().getMessage());
        assertEquals("Store cache capacity not defined. Set by default at 500.", logCapture.getMessage(0));
    }
}
