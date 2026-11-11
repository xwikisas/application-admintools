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
import java.util.Optional;

import org.xwiki.stability.Unstable;

/**
 * The automatic purge settings of a history item type, see {@link PurgeType#isHistory()}: besides the age limit of the
 * versions, they can limit the number of versions kept. A version is purged as soon as it is past either limit, and
 * the current version is always kept. The age limit and the number of versions can each be missing, but an enabled
 * item type needs at least one of them.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public final class HistoryPurgeTypeSettings extends PurgeTypeSettings
{
    private static final long serialVersionUID = 1L;

    private final Integer maxVersions;

    /**
     * Create the settings of a history item type.
     *
     * @param mode how the automatic purge is configured for the item type
     * @param interval the number of units the versions are kept for before being purged, or {@code null} if there is
     *     no age limit
     * @param unit the unit of the interval, or {@code null} if there is no age limit
     * @param maxVersions the number of versions kept, the current one included, or {@code null} if there is no such
     *     limit
     */
    public HistoryPurgeTypeSettings(PurgeMode mode, Integer interval, IntervalUnit unit, Integer maxVersions)
    {
        super(mode, interval, unit);
        this.maxVersions = maxVersions;
    }

    /**
     * @return the number of versions kept, the current one included, or an empty value if there is no such limit
     */
    public Optional<Integer> getMaxVersions()
    {
        return Optional.ofNullable(this.maxVersions);
    }

    /**
     * Compare with other history item type settings. They are equal when they have the same mode, interval, unit and
     * number of versions kept.
     *
     * @param object the object to compare with
     * @return {@code true} if the given object is equal history item type settings, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object)
    {
        return super.equals(object)
            && Objects.equals(this.maxVersions, ((HistoryPurgeTypeSettings) object).maxVersions);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), this.maxVersions);
    }
}
