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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.cache.CacheInfo;
import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

import groovy.jmx.GroovyMBean;

import static com.xwiki.admintools.internal.health.cache.data.CacheDataUtil.NAME_KEY;

/**
 * Handles the retrieval of cache info.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = CacheDataGenerator.class)
@Singleton
public class CacheDataGenerator
{
    private static final String QUERY_CONFIGURATION_TARGET = "Configuration";

    private static final String QUERY_STATISTICS_TARGET = "Statistics";

    private static final String REGEX = "\"";

    @Inject
    private Logger logger;

    @Inject
    private GroovyMBeanUtil groovyMBeanUtil;

    @Inject
    private CacheDataUtil cacheDataUtil;

    /**
     * Get a {@link List} with all the JMX managed cache.
     *
     * @param filter used to filter caches by name
     * @return a {@link List} of {@link CacheInfo}
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public List<CacheInfo> getCacheEntries(String filter) throws JMException, IOException
    {
        List<CacheInfo> cacheEntries = new ArrayList<>();
        String filterValue = filter == null ? "" : filter;
        Set<ObjectName> configCacheSet = cacheDataUtil.getCacheSet(QUERY_CONFIGURATION_TARGET);
        Map<String, Long> statsMap = loadStatisticsMap();

        for (ObjectName configCache : configCacheSet) {
            String cacheName = configCache.getKeyProperty(NAME_KEY);

            if (cacheName.contains(filterValue)) {
                GroovyMBean groovyMBean = groovyMBeanUtil.getGroovyMBean(configCache);
                long evictionSize = ((Number) groovyMBean.getProperty("evictionSize")).longValue();
                long numberOfEntries = statsMap.getOrDefault(cacheName, -1L);

                CacheInfo cacheInfo = new CacheInfo();
                cacheInfo.setCacheName(cacheName.replace(REGEX, ""));
                cacheInfo.setEvictionSize(evictionSize);
                cacheInfo.setNumberOfEntries(numberOfEntries);
                cacheEntries.add(cacheInfo);
            }
        }
        return cacheEntries;
    }

    /**
     * Get a {@link Map} with all the statistics for a given cache.
     *
     * @param name searched cache name
     * @return the full statistics of a given cache
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public Map<String, Object> getDetailedCacheEntry(String name) throws JMException, IOException
    {
        Map<String, Object> detailedEntryMap = new HashMap<>();
        Set<ObjectName> statsCacheSet = cacheDataUtil.getCacheSet(QUERY_STATISTICS_TARGET);
        for (ObjectName statsCache : statsCacheSet) {
            GroovyMBean bean = groovyMBeanUtil.getGroovyMBean(statsCache);
            String cacheName = statsCache.getKeyProperty(NAME_KEY);
            if (cacheName.replace(REGEX, "").equals(name)) {
                for (String attribute : bean.listAttributeNames()) {
                    try {
                        Object propertyValue = bean.getProperty(attribute);
                        detailedEntryMap.put(attribute, propertyValue);
                    } catch (Exception e) {
                        logger.warn(String.format("Failed to retrieve attribute [%s] for cache [%s].", attribute, name),
                            e);
                    }
                }
                break;
            }
        }
        return detailedEntryMap;
    }

    private Map<String, Long> loadStatisticsMap() throws JMException, IOException
    {
        Map<String, Long> statsMap = new HashMap<>();
        Set<ObjectName> statsNames = cacheDataUtil.getCacheSet(QUERY_STATISTICS_TARGET);

        for (ObjectName objectName : statsNames) {
            GroovyMBean bean = groovyMBeanUtil.getGroovyMBean(objectName);
            String name = objectName.getKeyProperty(NAME_KEY);
            try {
                long count = ((Number) bean.getProperty("numberOfEntries")).longValue();
                if (count == -1) {
                    count = ((Number) bean.getProperty("approximateEntries")).longValue();
                }
                statsMap.put(name, count);
            } catch (Exception e) {
                logger.warn(String.format("Failed to retrieve number of entries for cache [%s].", name), e);
            }
        }
        return statsMap;
    }
}
