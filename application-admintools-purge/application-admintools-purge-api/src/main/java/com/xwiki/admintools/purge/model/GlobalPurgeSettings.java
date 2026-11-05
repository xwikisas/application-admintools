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
import java.util.Optional;

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
     * @param typeSettings the default settings of each item type; an item type can be left out, in which case it is
     *     not purged for the wikis that inherit the defaults
     * @param maxHistoryEntries the number of purge history entries kept for each item type of each wiki, at least
     *     1
     * @throws NullPointerException if the settings of the item types are {@code null}
     * @throws IllegalArgumentException if the number of purge history entries is lower than 1
     */
    public GlobalPurgeSettings(Map<PurgeType, PurgeTypeSettings> typeSettings, int maxHistoryEntries)
    {
        Objects.requireNonNull(typeSettings, "The settings of the item types are missing.");
        if (maxHistoryEntries < 1) {
            throw new IllegalArgumentException(
                String.format("The purge history size must be at least 1, but was [%d].", maxHistoryEntries));
        }
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
     * @return the default settings of the given item type, or an empty value if there are none, in which case the item
     *     type is not purged for the wikis that inherit the defaults
     */
    public Optional<PurgeTypeSettings> getTypeSettings(PurgeType type)
    {
        return Optional.ofNullable(this.typeSettings.get(type));
    }

    /**
     * @return the number of purge history entries kept for each item type of each wiki
     */
    public int getMaxHistoryEntries()
    {
        return this.maxHistoryEntries;
    }

    /**
     * Compare with other global settings. They are equal when they keep the same number of history entries and have
     * the same settings for the same item types.
     *
     * @param object the object to compare with
     * @return {@code true} if the given object is equal global settings, {@code false} otherwise
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
