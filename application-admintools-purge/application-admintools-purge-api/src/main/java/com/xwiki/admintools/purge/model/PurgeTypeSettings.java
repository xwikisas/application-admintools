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

import java.util.Objects;

import org.xwiki.stability.Unstable;

/**
 * The automatic purge settings of one item type, as configured globally or for a specific wiki.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public final class PurgeTypeSettings
{
    private final PurgeMode mode;

    private final int interval;

    private final IntervalUnit unit;

    /**
     * Create the settings of an item type.
     *
     * @param mode how the automatic purge is configured for the item type
     * @param interval the number of units the deleted items are kept for before being purged
     * @param unit the unit of the interval
     */
    public PurgeTypeSettings(PurgeMode mode, int interval, IntervalUnit unit)
    {
        this.mode = mode;
        this.interval = interval;
        this.unit = unit;
    }

    /**
     * @return how the automatic purge is configured for the item type
     */
    public PurgeMode getMode()
    {
        return this.mode;
    }

    /**
     * @return the number of units the deleted items are kept for before being purged
     */
    public int getInterval()
    {
        return this.interval;
    }

    /**
     * @return the unit of the interval
     */
    public IntervalUnit getUnit()
    {
        return this.unit;
    }

    /**
     * Compare with other item type settings. They are equal when they have the same mode, interval and unit.
     *
     * @param object the object to compare with
     * @return {@code true} if the given object is equal item type settings, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PurgeTypeSettings)) {
            return false;
        }
        PurgeTypeSettings other = (PurgeTypeSettings) object;
        return this.mode == other.mode && this.interval == other.interval && this.unit == other.unit;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.mode, this.interval, this.unit);
    }
}
