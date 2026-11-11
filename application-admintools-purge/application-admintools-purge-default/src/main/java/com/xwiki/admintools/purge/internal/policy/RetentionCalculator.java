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
package com.xwiki.admintools.purge.internal.policy;

import java.time.Instant;
import java.time.ZonedDateTime;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.purge.model.IntervalUnit;

/**
 * Computes the expiration date of a purge: the items deleted before this instant have been in the recycle bin for
 * longer than the configured interval and must be purged.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = RetentionCalculator.class)
@Singleton
public class RetentionCalculator
{
    /**
     * Compute the expiration date relative to the given time. Calendar units are applied in the time zone of
     * {@code now}, so one month before March 31 is the last day of February, and one day before a given time keeps the
     * same local time across a daylight saving time change.
     *
     * @param now the time from which the interval is subtracted, usually {@link ZonedDateTime#now()}
     * @param interval the number of units the deleted items are kept for, at least 0; 0 gives {@code now}, so that
     *     every item deleted until now is expired
     * @param unit the unit of the interval
     * @return the instant before which deleted items must be purged
     * @throws IllegalArgumentException if the interval is negative, or if the unit is missing
     */
    public Instant getExpirationDate(ZonedDateTime now, int interval, IntervalUnit unit) throws IllegalArgumentException
    {
        if (interval < 0) {
            throw new IllegalArgumentException(
                String.format("The purge interval must be at least 0, but was [%d].", interval));
        }
        if (unit == null) {
            throw new IllegalArgumentException("The purge interval unit is missing.");
        }
        ZonedDateTime expirationDate;
        switch (unit) {
            case DAYS:
                expirationDate = now.minusDays(interval);
                break;
            case WEEKS:
                expirationDate = now.minusWeeks(interval);
                break;
            case MONTHS:
                expirationDate = now.minusMonths(interval);
                break;
            case YEARS:
                expirationDate = now.minusYears(interval);
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported interval unit [%s].", unit));
        }
        return expirationDate.toInstant();
    }
}
