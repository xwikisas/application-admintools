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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.ObjectName;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.cache.CacheInfo;
import com.xwiki.admintools.internal.health.cache.data.CacheDataGenerator;

import groovy.jmx.GroovyMBean;

/**
 * Manager class that handles JMX managed cache operations.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = CacheManager.class)
@Singleton
public class CacheManager
{
    @Inject
    private CacheDataGenerator cacheDataGenerator;

    /**
     * Get a sorted and filtered {@code List} with the JMX managed caches.
     *
     * @param filter used to filter caches by name
     * @param order the sort order applied to the result, based on the number of entries
     * @return a sorted and filtered {@code List} with the JMX managed caches
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public List<CacheInfo> getJMXCaches(String filter, String order) throws JMException, IOException
    {
        List<CacheInfo> cacheEntries = cacheDataGenerator.getCacheEntries(filter);
        return getSortedAndFilteredCacheEntries(cacheEntries, order);
    }

    /**
     * Get detailed statistics for a specific cache.
     *
     * @param name cache name to be searched for
     * @return a {@link Map} with the detailed statistics
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public Map<String, Object> getCacheDetailedView(String name) throws JMException, IOException
    {
        return cacheDataGenerator.getDetailedCacheEntry(name);
    }

    private List<CacheInfo> getSortedAndFilteredCacheEntries(List<CacheInfo> cacheEntries, String order)
    {

        boolean descending = order.equals("desc");
        return cacheEntries.stream().sorted(
            descending ? Comparator.comparingLong(CacheInfo::getNumberOfEntries).reversed()
                : Comparator.comparingLong(CacheInfo::getNumberOfEntries)).collect(Collectors.toList());
    }
}
