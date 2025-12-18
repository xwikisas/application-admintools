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
package com.xwiki.admintools.internal.health.cache.data;

import java.io.IOException;
import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

import groovy.jmx.GroovyMBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CacheDataFlusher}
 *
 * @version $Id$
 */
@ComponentTest
class CacheDataFlusherTest
{
    @InjectMockComponents
    private CacheDataFlusher cacheDataFlusher;

    @MockComponent
    private GroovyMBeanUtil groovyMBeanUtil;

    @MockComponent
    private CacheDataUtil dataUtil;

    @Mock
    private ObjectName objectName1;

    @Mock
    private ObjectName objectName2;

    @Mock
    private ObjectName objectName3;

    @Mock
    private GroovyMBean groovyMBean1;

    @Mock
    private GroovyMBean groovyMBean2;

    @Mock
    private GroovyMBean groovyMBean3;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @Test
    void clearAllCacheTest() throws JMException, IOException
    {
        when(dataUtil.getCacheSet("Cache")).thenReturn(Set.of(objectName1, objectName2, objectName3));
        when(groovyMBeanUtil.getGroovyMBean(objectName1)).thenReturn(groovyMBean1);
        when(groovyMBeanUtil.getGroovyMBean(objectName2)).thenReturn(groovyMBean2);
        when(groovyMBeanUtil.getGroovyMBean(objectName3)).thenReturn(groovyMBean3);

        when(groovyMBean2.invokeMethod(eq("clear"), any(Object.class))).thenThrow(new RuntimeException("failed"));
        when(objectName2.getKeyProperty("name")).thenReturn("name 2");

        assertFalse(cacheDataFlusher.clearAllCache());
        verify(groovyMBean1, times(1)).invokeMethod(eq("clear"), any(Object.class));
        verify(groovyMBean2, times(1)).invokeMethod(eq("clear"), any(Object.class));
        assertEquals("There was an error while flushing the cache for [name 2]", logCapture.getMessage(0));
    }

    @Test
    void clearCacheFound() throws JMException, IOException
    {
        when(dataUtil.getCacheSet("Cache")).thenReturn(Set.of(objectName1, objectName2, objectName3));
        when(groovyMBeanUtil.getGroovyMBean(objectName1)).thenReturn(groovyMBean1);
        when(groovyMBeanUtil.getGroovyMBean(objectName2)).thenReturn(groovyMBean2);
        when(groovyMBeanUtil.getGroovyMBean(objectName3)).thenReturn(groovyMBean3);

        when(objectName1.getKeyProperty("name")).thenReturn("name 1");
        when(objectName2.getKeyProperty("name")).thenReturn("name 2");
        when(objectName3.getKeyProperty("name")).thenReturn("name 3");

        assertTrue(cacheDataFlusher.clearCache("name 2"));
        verify(groovyMBean1, times(0)).invokeMethod(eq("clear"), any(Object.class));
        verify(groovyMBean2, times(1)).invokeMethod(eq("clear"), any(Object.class));
        verify(groovyMBean3, times(0)).invokeMethod(eq("clear"), any(Object.class));
    }

    @Test
    void clearCacheNotFound() throws JMException, IOException
    {
        when(dataUtil.getCacheSet("Cache")).thenReturn(Set.of(objectName1, objectName2, objectName3));
        when(groovyMBeanUtil.getGroovyMBean(objectName1)).thenReturn(groovyMBean1);
        when(groovyMBeanUtil.getGroovyMBean(objectName2)).thenReturn(groovyMBean2);
        when(groovyMBeanUtil.getGroovyMBean(objectName3)).thenReturn(groovyMBean3);

        when(objectName1.getKeyProperty("name")).thenReturn("name 1");
        when(objectName2.getKeyProperty("name")).thenReturn("name 2");
        when(objectName3.getKeyProperty("name")).thenReturn("name 3");

        assertFalse(cacheDataFlusher.clearCache("name 4"));
        verify(groovyMBean1, times(0)).invokeMethod(eq("clear"), any(Object.class));
        verify(groovyMBean2, times(0)).invokeMethod(eq("clear"), any(Object.class));
        verify(groovyMBean3, times(0)).invokeMethod(eq("clear"), any(Object.class));
    }
}
