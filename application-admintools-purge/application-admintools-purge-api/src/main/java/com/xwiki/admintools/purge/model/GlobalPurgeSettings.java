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
package com.xwiki.admintools.purge.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.xwiki.stability.Unstable;

/**
 * The global automatic purge settings, used by every wiki that doesn't override them.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public final class GlobalPurgeSettings
{
    private final Map<PurgeType, PurgeTypeSettings> typeSettings;

    private final int maxHistoryEntries;

    /**
     * Create the global settings.
     *
     * @param typeSettings the default settings of each item type
     * @param maxHistoryEntries the number of purge history entries kept for each item type of each wiki
     */
    public GlobalPurgeSettings(Map<PurgeType, PurgeTypeSettings> typeSettings, int maxHistoryEntries)
    {
        // EnumMap's copy constructor rejects an empty map that is not an EnumMap.
        Map<PurgeType, PurgeTypeSettings> copy = new EnumMap<>(PurgeType.class);
        copy.putAll(typeSettings);
        this.typeSettings = Collections.unmodifiableMap(copy);
        this.maxHistoryEntries = maxHistoryEntries;
    }

    /**
     * @return the default settings of each item type
     */
    public Map<PurgeType, PurgeTypeSettings> getTypeSettings()
    {
        return this.typeSettings;
    }

    /**
     * @param type an item type
     * @return the default settings of the given item type
     */
    public PurgeTypeSettings getTypeSettings(PurgeType type)
    {
        return this.typeSettings.get(type);
    }

    /**
     * @return the number of purge history entries kept for each item type of each wiki
     */
    public int getMaxHistoryEntries()
    {
        return this.maxHistoryEntries;
    }

    /**
     * Implementation used to help with testing.
     *
     * @return {@code true} if the objects are equals, or {@code false} otherwise.
     */
    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GlobalPurgeSettings)) {
            return false;
        }
        GlobalPurgeSettings other = (GlobalPurgeSettings) object;
        return this.maxHistoryEntries == other.maxHistoryEntries && this.typeSettings.equals(other.typeSettings);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.typeSettings, this.maxHistoryEntries);
    }
}
