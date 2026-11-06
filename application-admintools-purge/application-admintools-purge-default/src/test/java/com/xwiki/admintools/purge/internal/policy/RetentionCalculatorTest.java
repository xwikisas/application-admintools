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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xwiki.admintools.purge.model.IntervalUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link RetentionCalculator}.
 *
 * @version $Id$
 */
@ComponentTest
class RetentionCalculatorTest
{
    private static final ZoneId UTC = ZoneOffset.UTC;

    private static final ZoneId BUCHAREST = ZoneId.of("Europe/Bucharest");

    @InjectMockComponents
    private RetentionCalculator retentionCalculator;

    @Test
    void getExpirationDateForEachUnit()
    {
        ZonedDateTime now = time("2026-10-23T10:15:30", UTC);

        assertEquals(instant("2026-10-20T10:15:30", UTC),
            this.retentionCalculator.getExpirationDate(now, 3, IntervalUnit.DAYS));
        assertEquals(instant("2026-10-09T10:15:30", UTC),
            this.retentionCalculator.getExpirationDate(now, 2, IntervalUnit.WEEKS));
        assertEquals(instant("2026-09-23T10:15:30", UTC),
            this.retentionCalculator.getExpirationDate(now, 1, IntervalUnit.MONTHS));
        assertEquals(instant("2024-10-23T10:15:30", UTC),
            this.retentionCalculator.getExpirationDate(now, 2, IntervalUnit.YEARS));
    }

    @Test
    void getExpirationDateOneMonthBeforeEndOfMonth()
    {
        assertEquals(instant("2026-02-28T12:00:00", UTC),
            this.retentionCalculator.getExpirationDate(time("2026-03-31T12:00:00", UTC), 1, IntervalUnit.MONTHS));
        assertEquals(instant("2028-02-29T12:00:00", UTC),
            this.retentionCalculator.getExpirationDate(time("2028-03-31T12:00:00", UTC), 1, IntervalUnit.MONTHS));
    }

    @Test
    void getExpirationDateOneYearBeforeLeapDay()
    {
        assertEquals(instant("2027-02-28T12:00:00", UTC),
            this.retentionCalculator.getExpirationDate(time("2028-02-29T12:00:00", UTC), 1, IntervalUnit.YEARS));
    }

    @Test
    void getExpirationDateAcrossDaylightSavingTimeChange()
    {
        // Daylight saving time starts in Bucharest on 2026-03-29 at 03:00, so between 10:00 on March 28 and 10:00 on
        // March 29 there are only 23 hours.
        ZonedDateTime now = time("2026-03-29T10:00:00", BUCHAREST);

        Instant expirationDate = this.retentionCalculator.getExpirationDate(now, 1, IntervalUnit.DAYS);

        assertEquals(instant("2026-03-28T10:00:00", BUCHAREST), expirationDate);
        assertEquals(Duration.ofHours(23), Duration.between(expirationDate, now.toInstant()));
    }

    @Test
    void getExpirationDateWithZeroInterval()
    {
        // An interval of 0 expires every item deleted until now, whatever the unit.
        ZonedDateTime now = time("2026-10-30T10:00:00", BUCHAREST);

        assertEquals(now.toInstant(), this.retentionCalculator.getExpirationDate(now, 0, IntervalUnit.DAYS));
        assertEquals(now.toInstant(), this.retentionCalculator.getExpirationDate(now, 0, IntervalUnit.YEARS));
    }

    @Test
    void getExpirationDateWithInvalidInterval()
    {
        ZonedDateTime now = ZonedDateTime.now();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.retentionCalculator.getExpirationDate(now, -1, IntervalUnit.DAYS));
        assertEquals("The purge interval must be at least 0, but was [-1].", exception.getMessage());
    }

    private static ZonedDateTime time(String localDateTime, ZoneId zone)
    {
        return LocalDateTime.parse(localDateTime).atZone(zone);
    }

    private static Instant instant(String localDateTime, ZoneId zone)
    {
        return time(localDateTime, zone).toInstant();
    }
}
