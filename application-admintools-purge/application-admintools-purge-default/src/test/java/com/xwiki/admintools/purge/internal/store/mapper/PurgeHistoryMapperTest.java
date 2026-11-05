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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeHistoryMapper}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeHistoryMapperTest
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

    private static final String USER = "xwiki:XWiki.Admin";

    private static final Instant NOW = Instant.parse("2026-10-28T01:00:00Z");

    @InjectMockComponents
    private PurgeHistoryMapper mapper;

    @MockComponent
    private EntityReferenceSerializer<String> referenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @Mock
    private BaseObject object;

    @Mock
    private DocumentReference userReference;

    @Test
    void readType()
    {
        assertEquals(Optional.empty(), this.mapper.readType(this.object));

        when(this.object.getStringValue(TYPE)).thenReturn("unknown");
        assertEquals(Optional.empty(), this.mapper.readType(this.object));

        when(this.object.getStringValue(TYPE)).thenReturn("deletedAttachments");
        assertEquals(Optional.of(DELETED_ATTACHMENTS), this.mapper.readType(this.object));
    }

    @Test
    void readDate()
    {
        assertEquals(Optional.empty(), this.mapper.readDate(this.object));

        when(this.object.getDateValue(DATE)).thenReturn(Date.from(NOW));
        assertEquals(Optional.of(NOW), this.mapper.readDate(this.object));

        // The database returns a java.sql.Timestamp.
        when(this.object.getDateValue(DATE)).thenReturn(Timestamp.from(NOW));
        assertEquals(Optional.of(NOW), this.mapper.readDate(this.object));
    }

    @Test
    void readManualEntry()
    {
        mockValidEntry();
        when(this.object.getStringValue(TRIGGERED_BY)).thenReturn(USER);
        when(this.referenceResolver.resolve(USER)).thenReturn(this.userReference);
        when(this.object.getLongValue(REMOVED_COUNT)).thenReturn(40L);
        when(this.object.getLongValue(FAILED_COUNT)).thenReturn(120L);
        when(this.object.getLargeStringValue(FAILURES)).thenReturn("Space.Page: locked\n\n Space.Other: error \n");
        when(this.object.getStringValue(STATUS)).thenReturn("partial");

        assertEquals(Optional.of(
            new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, this.userReference, PurgeStatus.PARTIAL, 40, 120,
                List.of("Space.Page: locked", "Space.Other: error"))), this.mapper.readEntry(this.object));
    }

    @Test
    void readScheduledEntry()
    {
        mockValidEntry();
        when(this.object.getStringValue(TRIGGERED_BY)).thenReturn(SCHEDULED);
        when(this.object.getLongValue(REMOVED_COUNT)).thenReturn(3L);

        assertEquals(Optional.of(new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, null, PurgeStatus.SUCCESS, 3, 0, null)),
            this.mapper.readEntry(this.object));
        verify(this.referenceResolver, never()).resolve(anyString());
    }

    @Test
    void readEntryWithoutTriggeredByIsScheduled()
    {
        mockValidEntry();

        assertEquals(Optional.empty(), this.mapper.readEntry(this.object).orElseThrow().getTriggeredBy());
        verify(this.referenceResolver, never()).resolve(anyString());
    }

    @Test
    void readInvalidEntry()
    {
        when(this.object.getStringValue(TYPE)).thenReturn("deletedDocuments");
        when(this.object.getStringValue(STATUS)).thenReturn("success");
        assertEquals(Optional.empty(), this.mapper.readEntry(this.object));

        when(this.object.getStringValue(TYPE)).thenReturn("unknown");
        when(this.object.getDateValue(DATE)).thenReturn(Date.from(NOW));
        assertEquals(Optional.empty(), this.mapper.readEntry(this.object));

        // The status is stored, not computed, so an entry without a valid status can't be read.
        when(this.object.getStringValue(TYPE)).thenReturn("deletedDocuments");
        when(this.object.getStringValue(STATUS)).thenReturn("done");
        assertEquals(Optional.empty(), this.mapper.readEntry(this.object));
    }

    @Test
    void readQueriedEntry()
    {
        when(this.referenceResolver.resolve(USER)).thenReturn(this.userReference);

        assertEquals(Optional.of(
                new PurgeHistoryEntry(DELETED_ATTACHMENTS, NOW, this.userReference, PurgeStatus.INTERRUPTED, 7, 1, null)),
            this.mapper.readEntry("deletedAttachments", Timestamp.from(NOW), USER, "interrupted", 7, 1));
        assertEquals(Optional.empty(), this.mapper.readEntry("deletedAttachments", null, USER, "interrupted", 7, 1));
    }

    @Test
    void writeManualEntry()
    {
        when(this.referenceSerializer.serialize(this.userReference)).thenReturn(USER);

        this.mapper.writeEntry(this.object,
            new PurgeHistoryEntry(DELETED_ATTACHMENTS, NOW, this.userReference, PurgeStatus.PARTIAL, 40, 2,
                List.of("Space.Page@file.txt: first\r\nsecond", "Space.Page@other.txt: error")));

        verify(this.object).setStringValue(TYPE, "deletedAttachments");
        verify(this.object).setDateValue(DATE, Date.from(NOW));
        verify(this.object).setStringValue(TRIGGERED_BY, USER);
        verify(this.object).setStringValue(STATUS, "partial");
        verify(this.object).setLongValue(REMOVED_COUNT, 40);
        verify(this.object).setLongValue(FAILED_COUNT, 2);
        // A line break inside a failure is replaced, so that each failure stays on its own line.
        verify(this.object).setLargeStringValue(FAILURES,
            "Space.Page@file.txt: first second\nSpace.Page@other.txt: error");
        // A new entry is the newest of its item type.
        verify(this.object).setIntValue(LATEST, 1);
    }

    @Test
    void writeScheduledEntry()
    {
        this.mapper.writeEntry(this.object,
            new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, null, PurgeStatus.FAILED, 0, 1,
                List.of("Space.Page: error")));

        verify(this.object).setStringValue(TRIGGERED_BY, SCHEDULED);
        verify(this.object).setStringValue(STATUS, "failed");
        verify(this.object).setLargeStringValue(FAILURES, "Space.Page: error");
    }

    @Test
    void writeInterruptedEntryWithoutFailures()
    {
        this.mapper.writeEntry(this.object,
            new PurgeHistoryEntry(DELETED_DOCUMENTS, NOW, null, PurgeStatus.INTERRUPTED, 5, 0, null));

        verify(this.object).setStringValue(STATUS, "interrupted");
        verify(this.object).setLargeStringValue(FAILURES, "");
    }

    @Test
    void markAsOlder()
    {
        this.mapper.markAsOlder(this.object);

        verify(this.object).setLargeStringValue(FAILURES, "");
        verify(this.object).setIntValue(LATEST, 0);
        verify(this.object, never()).setLongValue(anyString(), anyLong());
    }

    /**
     * Give the history object a valid type, date and status.
     */
    private void mockValidEntry()
    {
        when(this.object.getStringValue(TYPE)).thenReturn("deletedDocuments");
        when(this.object.getDateValue(DATE)).thenReturn(Date.from(NOW));
        when(this.object.getStringValue(STATUS)).thenReturn("success");
    }
}
