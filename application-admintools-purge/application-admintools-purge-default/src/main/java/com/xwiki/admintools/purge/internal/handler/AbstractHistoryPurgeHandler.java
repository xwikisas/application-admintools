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
package com.xwiki.admintools.purge.internal.handler;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the old versions of histories, with the limits of {@link HistoryPurgeTypeSettings}: an age limit, a number of
 * versions kept, or both. When both are set, a version is purged only if it is past both limits: it is older than the
 * age limit, and it is beyond the newest versions kept. The versions are purged oldest first, the age limit stops at
 * the first version that isn't old enough, and the current version is always kept.
 *
 * @version $Id$
 * @since 1.5
 */
public abstract class AbstractHistoryPurgeHandler extends AbstractPurgeHandler
{
    /**
     * The number of items read at once when counting.
     */
    protected static final int COUNT_BATCH_SIZE = 1000;

    /**
     * The largest count of a history: counting stops there, since counting a whole history can be slow.
     */
    protected static final int MAX_COUNT = 1000;

    /**
     * Resolves the full names of the pages, shared with the subclasses for the same reason as {@link #logger}.
     */
    @Inject
    @Named("current")
    protected DocumentReferenceResolver<String> referenceResolver;

    /**
     * Check that the limit has a complete age limit, a number of versions, or both.
     *
     * @param limit the limit of a purge
     * @throws IllegalArgumentException if the age limit has only an interval or only a unit, or if there is no limit
     */
    @Override
    protected void checkLimit(PurgeTypeSettings limit)
    {
        if (limit.getInterval().isPresent() != limit.getUnit().isPresent()) {
            throw new IllegalArgumentException(String.format(
                "The age limit of the purge of [%s] needs both an interval and a unit.", getType().getId()));
        }
        if (limit.getInterval().isEmpty() && limit.getMaxVersions().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "The purge of [%s] needs an age limit, a number of versions, or both.", getType().getId()));
        }
    }

    /**
     * Compute how many of the oldest versions of a history are past the limit: the versions past the age limit, if
     * there is one, that are also beyond the number of versions kept, if there is one. The current version is never
     * counted.
     *
     * @param versions the number of versions of the history, the current one included
     * @param oldVersions the number of oldest versions past the age limit, or {@code null} if there is no age limit
     * @param maxVersions the number of versions kept, or {@code null} if there is no such limit
     * @return the number of versions past the limit
     */
    protected long getVersionsToPurge(long versions, Long oldVersions, Integer maxVersions)
    {
        long versionsToPurge = versions - 1;
        if (oldVersions != null) {
            versionsToPurge = Math.min(versionsToPurge, oldVersions);
        }
        if (maxVersions != null) {
            versionsToPurge = Math.min(versionsToPurge, versions - maxVersions);
        }
        return Math.max(0, versionsToPurge);
    }

    /**
     * @param dates the dates of the versions of a history, oldest first
     * @param expirationDate the versions before this date are past the age limit, or {@code null} if there is none
     * @return the number of versions older than the age limit before the first one that isn't, or {@code null} if
     *     there is no age limit
     */
    protected Long getLeadingOldVersions(List<Instant> dates, Instant expirationDate)
    {
        if (expirationDate == null) {
            return null;
        }
        return dates.stream().takeWhile(date -> date.isBefore(expirationDate)).count();
    }
}
