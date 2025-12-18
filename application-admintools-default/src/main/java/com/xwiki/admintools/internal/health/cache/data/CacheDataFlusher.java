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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

import groovy.jmx.GroovyMBean;

import static com.xwiki.admintools.internal.health.cache.data.CacheDataUtil.NAME_KEY;

/**
 * Handles the flush of JMX managed cache.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = CacheDataFlusher.class)
@Singleton
public class CacheDataFlusher
{
    private static final String QUERY_CACHE_TARGET = "Cache";

    private static final String CLEAR_METHOD_KEY = "clear";

    @Inject
    private Logger logger;

    @Inject
    private GroovyMBeanUtil groovyMBeanUtil;

    @Inject
    private CacheDataUtil cacheDataUtil;

    /**
     * Flush all JMX managed cache.
     *
     * @return {@code true} if all cache was flushed successfully, or {@code false} otherwise
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public boolean clearAllCache() throws JMException, IOException
    {
        Set<ObjectName> cacheSet = cacheDataUtil.getCacheSet(QUERY_CACHE_TARGET);
        boolean noError = true;
        for (ObjectName cache : cacheSet) {
            GroovyMBean groovyMBean = groovyMBeanUtil.getGroovyMBean(cache);
            try {
                groovyMBean.invokeMethod(CLEAR_METHOD_KEY, new Object[0]);
            } catch (Exception e) {
                String errMessage = String.format("There was an error while flushing the cache for [%s]",
                    cache.getKeyProperty(NAME_KEY));
                logger.error(errMessage, e);
                noError = false;
            }
        }
        return noError;
    }

    /**
     * Flush a specific cache.
     *
     * @param cacheName the target cache that needs to be flushed
     * @return {@code true} if the given cache name was found, or {@code false} otherwise
     * @throws JMException if there are any errors during {@link ObjectName} or {@link GroovyMBean} creation
     * @throws IOException if there are any errors during the {@link GroovyMBean} creation
     */
    public boolean clearCache(String cacheName) throws JMException, IOException
    {
        Set<ObjectName> cacheSet = cacheDataUtil.getCacheSet(QUERY_CACHE_TARGET);
        for (ObjectName cache : cacheSet) {
            GroovyMBean groovyMBean = groovyMBeanUtil.getGroovyMBean(cache);
            String name = cache.getKeyProperty(NAME_KEY).replace("\"", "");
            if (name.equals(cacheName)) {
                groovyMBean.invokeMethod(CLEAR_METHOD_KEY, new Object[0]);
                return true;
            }
        }
        return false;
    }
}
