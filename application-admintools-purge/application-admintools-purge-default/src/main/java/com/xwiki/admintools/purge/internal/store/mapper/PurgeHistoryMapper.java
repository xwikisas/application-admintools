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
package com.xwiki.admintools.purge.internal.store.mapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Converts between the purge history objects stored in the wiki and the purge history entries.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeHistoryMapper.class)
@Singleton
public class PurgeHistoryMapper
{
    private static final String TYPE = "type";

    private static final String DATE = "date";

    private static final String TRIGGERED_BY = "triggeredBy";

    private static final String STATUS = "status";

    private static final String REMOVED_COUNT = "removedCount";

    private static final String FAILED_COUNT = "failedCount";

    private static final String FAILURES = "failures";

    private static final String LATEST = "latest";

    private static final String SCHEDULED = "scheduled";

    private static final String LINE_SEPARATOR = "\n";

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    /**
     * Read the item type of a history object, without reading the whole entry.
     *
     * @param object a history object
     * @return the item type, or empty if it is missing or unknown, which can only come from a manual edit
     */
    public Optional<PurgeType> readType(BaseObject object)
    {
        return PurgeType.fromId(object.getStringValue(TYPE));
    }

    /**
     * Read the date of a history object, without reading the whole entry.
     *
     * @param object a history object
     * @return when the purge ran, or empty if the date is missing, which can only come from a manual edit
     */
    public Optional<Instant> readDate(BaseObject object)
    {
        // Instant.ofEpochMilli also works for the java.sql.Timestamp returned by the database.
        return Optional.ofNullable(object.getDateValue(DATE)).map(date -> Instant.ofEpochMilli(date.getTime()));
    }

    /**
     * Read a purge history entry from a history object. Missing counts are read as 0.
     *
     * @param object a history object
     * @return the history entry, or empty if the type, the date or the status is missing or invalid
     */
    public Optional<PurgeHistoryEntry> readEntry(BaseObject object)
    {
        return readEntry(object.getStringValue(TYPE), object.getDateValue(DATE), object.getStringValue(TRIGGERED_BY),
            object.getStringValue(STATUS), object.getLongValue(REMOVED_COUNT), object.getLongValue(FAILED_COUNT),
            readFailures(object.getLargeStringValue(FAILURES)));
    }

    /**
     * Read a purge history entry from the values of a history object returned by a query, without its failures.
     *
     * @param typeId the stored item type
     * @param date the stored date
     * @param triggeredBy the stored user who started the purge, or {@code scheduled}
     * @param statusId the stored status
     * @param removedCount the stored number of removed items
     * @param failedCount the stored number of items that couldn't be removed
     * @return the history entry, or empty if the type, the date or the status is missing or invalid
     */
    public Optional<PurgeHistoryEntry> readEntry(String typeId, Date date, String triggeredBy, String statusId,
        long removedCount, long failedCount)
    {
        return readEntry(typeId, date, triggeredBy, statusId, removedCount, failedCount, List.of());
    }

    /**
     * Write a purge history entry to a history object, as the newest entry of its item type. The page holding the
     * object still needs to be saved.
     *
     * @param object the history object to write to
     * @param entry the history entry
     */
    public void writeEntry(BaseObject object, PurgeHistoryEntry entry)
    {
        object.setStringValue(TYPE, entry.getType().getId());
        object.setDateValue(DATE, Date.from(entry.getDate()));
        object.setStringValue(TRIGGERED_BY,
            entry.getTriggeredBy().map(this.referenceSerializer::serialize).orElse(SCHEDULED));
        object.setStringValue(STATUS, entry.getStatus().getId());
        object.setLongValue(REMOVED_COUNT, entry.getRemovedCount());
        object.setLongValue(FAILED_COUNT, entry.getFailedCount());
        // A line break inside a failure would split it into two items when read.
        object.setLargeStringValue(FAILURES,
            entry.getFailures().stream().map(failure -> failure.replaceAll("[\\r\\n]+", " "))
                .collect(Collectors.joining(LINE_SEPARATOR)));
        object.setIntValue(LATEST, 1);
    }

    /**
     * Mark a history object as no longer the newest entry of its item type: its list of failures is emptied, keeping
     * its counts, and it loses the flag that lets a query find the newest entries. The page holding the object still
     * needs to be saved.
     *
     * @param object the history object
     */
    public void markAsOlder(BaseObject object)
    {
        object.setLargeStringValue(FAILURES, "");
        object.setIntValue(LATEST, 0);
    }

    /**
     * @param typeId the stored item type
     * @param date the stored date
     * @param triggeredBy the stored user who started the purge, or {@code scheduled}
     * @param statusId the stored status
     * @param removedCount the stored number of removed items
     * @param failedCount the stored number of items that couldn't be removed
     * @param failures the failures read
     * @return the history entry, or empty if the type, the date or the status is missing or invalid
     */
    private Optional<PurgeHistoryEntry> readEntry(String typeId, Date date, String triggeredBy, String statusId,
        long removedCount, long failedCount, List<String> failures)
    {
        Optional<PurgeType> type = PurgeType.fromId(typeId);
        Optional<PurgeStatus> status = PurgeStatus.fromId(statusId);
        if (type.isEmpty() || date == null || status.isEmpty()) {
            return Optional.empty();
        }
        // Instant.ofEpochMilli also works for the java.sql.Timestamp returned by the database.
        return Optional.of(new PurgeHistoryEntry(type.get(), Instant.ofEpochMilli(date.getTime()),
            readTriggeredBy(triggeredBy).orElse(null), status.get(), removedCount, failedCount, failures));
    }

    /**
     * Read who started a purge. A missing value is read as a scheduled purge too, since it is only displayed.
     *
     * @param triggeredBy the stored value: the full reference of the user who started the purge manually, or
     *     {@code scheduled}
     * @return the user who started the purge manually, or empty for a scheduled purge
     */
    private Optional<DocumentReference> readTriggeredBy(String triggeredBy)
    {
        if (StringUtils.isBlank(triggeredBy) || SCHEDULED.equals(triggeredBy)) {
            return Optional.empty();
        }
        return Optional.of(this.referenceResolver.resolve(triggeredBy));
    }

    /**
     * @param failures the stored failures, one per line, possibly empty
     * @return the failures, without empty lines
     */
    private List<String> readFailures(String failures)
    {
        if (StringUtils.isBlank(failures)) {
            return List.of();
        }
        return Arrays.stream(failures.split(LINE_SEPARATOR)).map(String::strip).filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());
    }
}
