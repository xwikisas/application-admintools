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
package com.xwiki.admintools.health.cache;

import org.xwiki.stability.Unstable;

/**
 * Holds JMX exposed information of name, eviction limit and size for a single cache.
 *
 * @version $Id$
 * @since 1.3
 */
@Unstable
public class CacheInfo
{
    private String cacheName;

    private long evictionSize;

    private long numberOfEntries;

    /**
     * Default constructor.
     */
    public CacheInfo()
    {

    }

    /**
     * {@link #setEvictionSize}.
     *
     * @return the eviction size
     */
    public long getEvictionSize()
    {
        return evictionSize;
    }

    /**
     * Set the maximum eviction size for the cache. This represents the configured upper limit after which entries may
     * be evicted.
     *
     * @param evictionSize the configured eviction size for the cache
     */
    public void setEvictionSize(long evictionSize)
    {
        this.evictionSize = evictionSize;
    }

    /**
     * {@link #setNumberOfEntries}.
     *
     * @return the number of entries
     */
    public long getNumberOfEntries()
    {
        return numberOfEntries;
    }

    /**
     * Set the current number of entries stored in the cache.
     *
     * @param numberOfEntries the current number of cache entries
     */
    public void setNumberOfEntries(long numberOfEntries)
    {
        this.numberOfEntries = numberOfEntries;
    }

    /**
     * {@link #setCacheName}.
     *
     * @return the cache name
     */
    public String getCacheName()
    {
        return cacheName;
    }

    /**
     * Set the name of the cache.
     *
     * @param cacheName name of the cache
     */
    public void setCacheName(String cacheName)
    {
        this.cacheName = cacheName;
    }

    /**
     * Get a formatted display of the cache load level, expressed as numberOfEntries/evictionSize.
     *
     * @return a formatted display of the cache size
     */
    public String getFormattedCacheSize()
    {
        return String.format("%d/%d", numberOfEntries, evictionSize);
    }
}
