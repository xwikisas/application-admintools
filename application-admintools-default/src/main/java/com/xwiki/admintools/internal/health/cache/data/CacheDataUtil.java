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

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

/**
 * Abstract class for common code needed in JMX managed cache data handling.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = CacheDataUtil.class)
@Singleton
public class CacheDataUtil
{
    /**
     * Key used for the name field.
     */
    public static final String NAME_KEY = "name";

    @Inject
    private GroovyMBeanUtil groovyMBeanUtil;

    /**
     * Get the cache for a given component.
     *
     * @param target target component for which to retrieve the cache
     * @return a {@link Set} with the cache objects for the given target component
     * @throws MalformedObjectNameException if the string passed as a parameter does not have the right format
     */
    public Set<ObjectName> getCacheSet(String target) throws MalformedObjectNameException
    {
        MBeanServer server = groovyMBeanUtil.getMBeanServer();
        ObjectName queryStatistics = new ObjectName(getFormattedQuery(target));
        return server.queryNames(queryStatistics, null);
    }

    private String getFormattedQuery(String target)
    {
        return String.format("org.xwiki.infinispan:component=%s,*", target);
    }
}
