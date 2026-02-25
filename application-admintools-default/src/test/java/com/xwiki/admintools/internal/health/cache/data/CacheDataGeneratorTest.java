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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

import com.xwiki.admintools.health.cache.CacheInfo;
import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

import groovy.jmx.GroovyMBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CacheDataGenerator}
 *
 * @version $Id$
 */
@ComponentTest
class CacheDataGeneratorTest
{
    @InjectMockComponents
    private CacheDataGenerator cacheDataGenerator;

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
    private ObjectName objectStats1;

    @Mock
    private ObjectName objectStats2;

    @Mock
    private ObjectName objectStats3;

    @Mock
    private GroovyMBean groovyMBean1;

    @Mock
    private GroovyMBean groovyMBean2;

    @Mock
    private GroovyMBean groovyMBean3;

    @Mock
    private GroovyMBean groovyMBean4;

    @Mock
    private GroovyMBean groovyMBean5;

    @Mock
    private GroovyMBean groovyMBean6;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getCacheEntriesTest() throws JMException, IOException
    {
        Set<ObjectName> configs = new LinkedHashSet<>();
        configs.add(objectName1);
        configs.add(objectName2);
        configs.add(objectName3);
        when(dataUtil.getCacheSet("Configuration")).thenReturn(configs);
        when(dataUtil.getCacheSet("Statistics")).thenReturn(Set.of(objectStats1, objectStats2, objectStats3));

        when(objectName1.getKeyProperty("name")).thenReturn("\"filter key 1\"");
        when(objectName2.getKeyProperty("name")).thenReturn("\"filter key 2\"");
        when(objectName3.getKeyProperty("name")).thenReturn("\"key 3\"");
        when(groovyMBeanUtil.getGroovyMBean(objectName1)).thenReturn(groovyMBean1);
        when(groovyMBeanUtil.getGroovyMBean(objectName2)).thenReturn(groovyMBean2);
        when(groovyMBeanUtil.getGroovyMBean(objectName3)).thenReturn(groovyMBean3);

        when(objectStats1.getKeyProperty("name")).thenReturn("\"filter key 1\"");
        when(objectStats2.getKeyProperty("name")).thenReturn("\"filter key\"");
        when(objectStats3.getKeyProperty("name")).thenReturn("\"key 3\"");
        when(groovyMBeanUtil.getGroovyMBean(objectStats1)).thenReturn(groovyMBean4);
        when(groovyMBeanUtil.getGroovyMBean(objectStats2)).thenReturn(groovyMBean5);
        when(groovyMBeanUtil.getGroovyMBean(objectStats3)).thenReturn(groovyMBean6);

        when(groovyMBean1.getProperty("evictionSize")).thenReturn(20L);
        when(groovyMBean2.getProperty("evictionSize")).thenReturn(50L);
        when(groovyMBean3.getProperty("evictionSize")).thenReturn(80L);

        when(groovyMBean4.getProperty("numberOfEntries")).thenReturn(10L);
        when(groovyMBean5.getProperty("numberOfEntries")).thenThrow(new RuntimeException("Could not access property"));
        when(groovyMBean6.getProperty("numberOfEntries")).thenReturn(-1L);
        when(groovyMBean6.getProperty("approximateEntries")).thenReturn(28L);

        List<CacheInfo> results = cacheDataGenerator.getCacheEntries("ilter");
        assertEquals("Failed to retrieve number of entries for cache [\"filter key\"].", logCapture.getMessage(0));
        assertEquals(2, results.size());
        assertEquals("filter key 1", results.get(0).getCacheName());
        assertEquals("10/20", results.get(0).getFormattedCacheSize());

        assertEquals("filter key 2", results.get(1).getCacheName());
        assertEquals("-1/50", results.get(1).getFormattedCacheSize());
    }

    @Test
    void getDetailedCacheEntry() throws JMException, IOException
    {
        when(dataUtil.getCacheSet("Statistics")).thenReturn(Set.of(objectStats1, objectStats2, objectStats3));
        when(groovyMBeanUtil.getGroovyMBean(objectStats1)).thenReturn(groovyMBean4);
        when(groovyMBeanUtil.getGroovyMBean(objectStats2)).thenReturn(groovyMBean5);
        when(groovyMBeanUtil.getGroovyMBean(objectStats3)).thenReturn(groovyMBean6);

        when(objectStats1.getKeyProperty("name")).thenReturn("\"filter key 1\"");
        when(objectStats2.getKeyProperty("name")).thenReturn("\"filter key\"");
        when(objectStats3.getKeyProperty("name")).thenReturn("\"key 3\"");
        when(groovyMBean5.listAttributeNames()).thenReturn(
            List.of("approximateEntries", "numberOfEntries", "evictionSize"));

        when(groovyMBean5.getProperty("numberOfEntries")).thenThrow(new RuntimeException("Could not access property"));
        when(groovyMBean5.getProperty("approximateEntries")).thenReturn(28L);
        when(groovyMBean5.getProperty("evictionSize")).thenReturn(80L);

        Map<String, Object> results = cacheDataGenerator.getDetailedCacheEntry("filter key");
        assertEquals(2, results.size());
        assertEquals(28L, results.get("approximateEntries"));
        assertEquals(80L, results.get("evictionSize"));
        assertEquals("empty", results.getOrDefault("numberOfEntries", "empty"));
        assertEquals("Failed to retrieve attribute [numberOfEntries] for cache [filter key].",
            logCapture.getMessage(0));
    }
}
