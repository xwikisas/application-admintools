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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * One entry of the purge history of a wiki: the statistics of one purge run of one item type.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public final class PurgeHistoryEntry
{
    private final PurgeType type;

    private final Instant date;

    private final DocumentReference triggeredBy;

    private final PurgeStatus status;

    private final long removedCount;

    private final long failedCount;

    private final List<String> failures;

    /**
     * Create a purge history entry.
     *
     * @param type the purged item type
     * @param date when the purge ran
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge
     * @param status the outcome of the purge
     * @param removedCount the number of items removed
     * @param failedCount the number of items that couldn't be removed, which can be larger than the number of
     *     failures listed
     * @param failures one line per item that couldn't be removed, possibly limited to the first failures;
     *     {@code null} or empty if there is none, or if the list was cleared
     */
    public PurgeHistoryEntry(PurgeType type, Instant date, DocumentReference triggeredBy, PurgeStatus status,
        long removedCount, long failedCount, List<String> failures)
    {
        this.type = type;
        this.date = date;
        this.triggeredBy = triggeredBy;
        this.status = status;
        this.removedCount = removedCount;
        this.failedCount = failedCount;
        this.failures = failures != null ? List.copyOf(failures) : List.of();
    }

    /**
     * @return the purged item type
     */
    public PurgeType getType()
    {
        return this.type;
    }

    /**
     * @return when the purge ran
     */
    public Instant getDate()
    {
        return this.date;
    }

    /**
     * @return the user who started the purge manually, or empty for a scheduled purge
     */
    public Optional<DocumentReference> getTriggeredBy()
    {
        return Optional.ofNullable(this.triggeredBy);
    }

    /**
     * @return the number of items removed
     */
    public long getRemovedCount()
    {
        return this.removedCount;
    }

    /**
     * @return the number of items that couldn't be removed
     */
    public long getFailedCount()
    {
        return this.failedCount;
    }

    /**
     * @return one line per item that couldn't be removed, possibly limited to the first failures; empty if there is
     *     none, or if the list was cleared
     */
    public List<String> getFailures()
    {
        return this.failures;
    }

    /**
     * @return the outcome of the purge
     */
    public PurgeStatus getStatus()
    {
        return this.status;
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
        if (!(object instanceof PurgeHistoryEntry)) {
            return false;
        }
        PurgeHistoryEntry other = (PurgeHistoryEntry) object;
        return this.type == other.type && Objects.equals(this.date, other.date) && Objects.equals(this.triggeredBy,
            other.triggeredBy) && this.status == other.status && this.removedCount == other.removedCount
            && this.failedCount == other.failedCount && this.failures.equals(other.failures);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.type, this.date, this.triggeredBy, this.status, this.removedCount, this.failedCount,
            this.failures);
    }
}
