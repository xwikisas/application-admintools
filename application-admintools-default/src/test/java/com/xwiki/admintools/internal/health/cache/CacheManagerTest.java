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
package com.xwiki.admintools.internal.health.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.JMException;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.health.cache.CacheInfo;
import com.xwiki.admintools.internal.health.cache.data.CacheDataGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CacheManager}
 *
 * @version $Id$
 */
@ComponentTest
class CacheManagerTest
{
    @InjectMockComponents
    private CacheManager cacheManager;

    @MockComponent
    private CacheDataGenerator dataGenerator;

    @Test
    void getJMXCachesTest() throws JMException, IOException
    {
        String filter = "filter";
        CacheInfo cacheInfo1 = new CacheInfo();
        CacheInfo cacheInfo2 = new CacheInfo();
        CacheInfo cacheInfo3 = new CacheInfo();
        cacheInfo1.setNumberOfEntries(100L);
        cacheInfo2.setNumberOfEntries(50L);
        cacheInfo3.setNumberOfEntries(75L);

        List<CacheInfo> cacheInfoList = new ArrayList<>(3);
        cacheInfoList.add(cacheInfo1);
        cacheInfoList.add(cacheInfo2);
        cacheInfoList.add(cacheInfo3);

        when(dataGenerator.getCacheEntries(filter)).thenReturn(cacheInfoList);

        List<CacheInfo> results = cacheManager.getJMXCaches(filter, "desc");
        assertEquals(cacheInfo1, results.get(0));
        assertEquals(cacheInfo3, results.get(1));
        assertEquals(cacheInfo2, results.get(2));

        results = cacheManager.getJMXCaches(filter, "asc");
        assertEquals(cacheInfo2, results.get(0));
        assertEquals(cacheInfo3, results.get(1));
        assertEquals(cacheInfo1, results.get(2));
    }

    @Test
    void getCacheDetailedViewTest() throws JMException, IOException
    {
        String name = "some cache name";
        when(dataGenerator.getDetailedCacheEntry(name)).thenReturn(Map.of("key", 1));
        assertEquals(1, cacheManager.getCacheDetailedView(name).get("key"));
    }
}
