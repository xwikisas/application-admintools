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
import java.time.ZonedDateTime;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xwiki.admintools.purge.internal.policy.RetentionCalculator;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeletedAttachmentsPurgeHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class DeletedAttachmentsPurgeHandlerTest
{
    private static final String WIKI_ID = "subwiki";

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-09-30T01:00:00Z");

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-10-30T01:00:00Z");

    private static final Instant DATE = NOW.toInstant();

    private static final PurgeTypeSettings LIMIT = new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS);

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DeletedAttachmentsPurgeHandler handler;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private RetentionCalculator retentionCalculator;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private AttachmentRecycleBinStore recycleBinStore;

    @BeforeEach
    void setUp()
    {
        when(this.retentionCalculator.getExpirationDate(NOW, 1, IntervalUnit.MONTHS)).thenReturn(EXPIRATION_DATE);
    }

    @Test
    void hint()
    {
        assertEquals(PurgeType.DELETED_ATTACHMENTS.getId(), DeletedAttachmentsPurgeHandler.HINT);
    }

    @Test
    void countExpired() throws Exception
    {
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery(
            "select count(item.id) from DeletedAttachment as item where item.date < :expirationDate",
            Query.HQL)).thenReturn(query);
        when(query.execute()).thenReturn(List.of(3L));

        assertEquals(3, this.handler.countExpired(WIKI_ID, LIMIT, NOW));
    }

    @Test
    void purge() throws Exception
    {
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery("select item.id from DeletedAttachment as item "
            + "where item.date < :expirationDate and item.id > :lastId order by item.id", Query.HQL)).thenReturn(query);
        when(query.<Long>execute()).thenReturn(List.of(1L, 2L));
        Query nameQuery = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery(
            "select item.docName, item.filename from DeletedAttachment as item where item.id = :id",
            Query.HQL)).thenReturn(nameQuery);
        when(nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { "Space.Page", "image.png" }));
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getAttachmentRecycleBinStore()).thenReturn(this.recycleBinStore);
        doThrow(new XWikiException(0, 0, "Failed to delete")).when(this.recycleBinStore)
            .deleteFromRecycleBin(1, this.wikiContext, true);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, LIMIT, NOW, null);

        assertEquals(new PurgeHistoryEntry(PurgeType.DELETED_ATTACHMENTS, DATE, null, PurgeStatus.PARTIAL, 1, 1,
            List.of("Space.Page@image.png\tXWikiException: Error number 0 in 0: Failed to delete")), entry);
        verify(this.recycleBinStore).deleteFromRecycleBin(2, this.wikiContext, true);
        verify(nameQuery).bindValue("id", 1L);
        assertEquals("Failed to purge the [deletedAttachments] item [Space.Page@image.png] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }
}
