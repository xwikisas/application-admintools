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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xwiki.admintools.purge.internal.policy.RetentionCalculator;
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PageHistoryPurgeHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class PageHistoryPurgeHandlerTest
{
    private static final String WIKI_ID = "subwiki";

    private static final ZonedDateTime NOW = ZonedDateTime.parse("2026-10-30T01:00:00Z");

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-09-30T01:00:00Z");

    private static final Date OLD = Date.from(Instant.parse("2026-01-01T00:00:00Z"));

    private static final Date RECENT = Date.from(Instant.parse("2026-10-01T00:00:00Z"));

    private static final PurgeTypeSettings AGE_LIMIT =
        new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, null);

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "Admin");

    private static final DocumentReference PAGE = new DocumentReference(WIKI_ID, "Space", "Page");

    private static final String PAGE_ID = "rcs.id.docId";

    private static final String COUNT_COLUMNS = "rcs.id.docId, count(rcs.id.docId)";

    private static final String OLD_VERSION_COUNT = ", sum(case when rcs.date < :expirationDate then 1 else 0 end)";

    private static final String HAS_OLD_VERSIONS = "min(rcs.date) < :expirationDate";

    private static final String HAS_TOO_MANY_VERSIONS = "count(rcs.id.docId) > :maxVersions";

    private static final String NAME_QUERY =
        "select doc.fullName, doc.language from XWikiDocument as doc where doc.id = :id";

    private static final String LAST_ID = "lastId";

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PageHistoryPurgeHandler handler;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private RetentionCalculator retentionCalculator;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiDocumentArchive archive;

    private Query nameQuery;

    @BeforeEach
    void setUp() throws Exception
    {
        this.nameQuery = mockQuery(NAME_QUERY);
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.wikiContext.getWikiId()).thenReturn(WIKI_ID);
        when(this.wikiContext.getWiki()).thenReturn(this.wiki);
        when(this.retentionCalculator.getExpirationDate(NOW, 1, IntervalUnit.MONTHS)).thenReturn(EXPIRATION_DATE);
        when(this.referenceResolver.resolve("Space.Page", new WikiReference(WIKI_ID))).thenReturn(PAGE);
        when(this.wiki.getDocument(any(DocumentReference.class), any(XWikiContext.class))).thenReturn(this.document);
        when(this.document.getDocumentArchive(this.wikiContext)).thenReturn(this.archive);
    }

    @Test
    void hint()
    {
        assertEquals(PurgeType.PAGE_HISTORY.getId(), PageHistoryPurgeHandler.HINT);
    }

    @Test
    void countExpiredWithBothLimits() throws Exception
    {
        Query query = mockQuery(pagesQuery(COUNT_COLUMNS + OLD_VERSION_COUNT,
            HAS_OLD_VERSIONS + " or " + HAS_TOO_MANY_VERSIONS));
        // A version is purged as soon as it is past either limit: 10 versions, 3 too old and 5 too many purge 5; 4
        // versions, all too old, purge 3, since the current version is kept.
        when(query.<Object[]>execute()).thenReturn(List.of(new Object[] { 1L, 10L, 3L }, new Object[] { 2L, 4L, 4L }));

        assertEquals(8, this.handler.countExpired(WIKI_ID,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, 5), NOW));

        verify(query).setWiki(WIKI_ID);
        verify(query).bindValue(LAST_ID, Long.MIN_VALUE);
        verify(query).bindValue("expirationDate", Date.from(EXPIRATION_DATE));
        verify(query).bindValue("maxVersions", 5L);
    }

    @Test
    void countExpiredWithOneLimit() throws Exception
    {
        Query maxVersionsQuery = mockQuery(pagesQuery(COUNT_COLUMNS, HAS_TOO_MANY_VERSIONS));
        when(maxVersionsQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { 1L, 7L }));
        Query ageQuery = mockQuery(pagesQuery(COUNT_COLUMNS + OLD_VERSION_COUNT, HAS_OLD_VERSIONS));
        when(ageQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { 1L, 3L, 3L }));

        assertEquals(2, this.handler.countExpired(WIKI_ID,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 5), NOW));
        assertEquals(2, this.handler.countExpired(WIKI_ID, AGE_LIMIT, NOW));

        verify(maxVersionsQuery, never()).bindValue("expirationDate", Date.from(EXPIRATION_DATE));
        verify(ageQuery, never()).bindValue("maxVersions", 5L);
    }

    @Test
    void countExpiredInBatches() throws Exception
    {
        Query query = mockQuery(pagesQuery(COUNT_COLUMNS + OLD_VERSION_COUNT, HAS_OLD_VERSIONS));
        List<Object[]> fullBatch = new ArrayList<>();
        for (long id = 1; id <= 1000; id++) {
            fullBatch.add(new Object[] { id, 2L, 1L });
        }
        when(query.<Object[]>execute()).thenReturn(fullBatch, List.<Object[]>of(new Object[] { 1001L, 2L, 1L }));

        assertEquals(1001, this.handler.countExpired(WIKI_ID, AGE_LIMIT, NOW));

        verify(query, times(2)).setLimit(1000);
        verify(query).bindValue(LAST_ID, 1000L);
    }

    @Test
    void invalidLimit()
    {
        PurgeTypeSettings halfAgeLimit = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, null, 5);
        assertEquals("The age limit of the purge of [pageHistory] needs both an interval and a unit.",
            assertThrows(IllegalArgumentException.class, () -> this.handler.countExpired(WIKI_ID, halfAgeLimit, NOW))
                .getMessage());

        PurgeTypeSettings noLimit = new PurgeTypeSettings(PurgeMode.ENABLED, null, null);
        assertEquals("The purge of [pageHistory] needs an age limit, a number of versions, or both.",
            assertThrows(IllegalArgumentException.class, () -> this.handler.purge(WIKI_ID, noLimit, NOW, USER))
                .getMessage());
        // The purge fails before going through the pages.
        verify(this.wikiContext, never()).setWikiId(anyString());
    }

    @Test
    void purgeOldVersions() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        mockName("Space.Page", "fr");
        mockVersions(OLD, OLD, RECENT, RECENT);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        assertEquals(new PurgeHistoryEntry(PurgeType.PAGE_HISTORY, NOW.toInstant(), USER, PurgeStatus.SUCCESS, 2, 0,
            List.of()), entry);
        // Each translation has its own history.
        verify(this.wiki).getDocument(new DocumentReference(PAGE, Locale.FRENCH), this.wikiContext);
        verify(this.wiki).deleteDocumentVersions(this.document, "1.1", "1.2", false, this.wikiContext);
    }

    @Test
    void purgeWithBothLimits() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS + " or " + HAS_TOO_MANY_VERSIONS), 1L);
        mockName("Space.Page", "");
        mockVersions(OLD, RECENT, RECENT, RECENT);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, 2), NOW, USER);

        // The versions beyond the 2 newest ones are purged, although only the first one is too old.
        assertEquals(2, entry.getRemovedCount());
        verify(this.wiki).deleteDocumentVersions(this.document, "1.1", "1.2", false, this.wikiContext);
    }

    @Test
    void purgeKeepsTheCurrentVersion() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        mockName("Space.Page", "");
        mockVersions(OLD, OLD, OLD);

        assertEquals(2, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        verify(this.wiki).deleteDocumentVersions(this.document, "1.1", "1.2", false, this.wikiContext);
    }

    @Test
    void purgeStopsAtTheFirstRecentVersion() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        mockName("Space.Page", "");
        // The dates don't follow the order of the versions, e.g. after an import: the purge removes one range from
        // the first version, so an old version after a recent one is kept.
        mockVersions(OLD, RECENT, OLD, RECENT);

        assertEquals(1, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        verify(this.wiki).deleteDocumentVersions(this.document, "1.1", "1.1", false, this.wikiContext);
    }

    @Test
    void purgeWithoutVersionsToPurge() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        mockName("Space.Page", "");
        // The page was edited since its versions were selected.
        mockVersions(RECENT, OLD);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        assertEquals(PurgeStatus.SUCCESS, entry.getStatus());
        assertEquals(0, entry.getRemovedCount());
        verify(this.wiki, never()).deleteDocumentVersions(any(), anyString(), anyString(), anyBoolean(), any());
    }

    @Test
    void purgeHistoryOfMissingPage() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        when(this.nameQuery.execute()).thenReturn(List.of());

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // The history left behind by a deleted page is skipped.
        assertEquals(PurgeStatus.SUCCESS, entry.getStatus());
        assertEquals(0, entry.getRemovedCount());
        verify(this.wiki, never()).getDocument(any(DocumentReference.class), any(XWikiContext.class));
    }

    @Test
    void purgeWhenTheCurrentVersionIsNotTheNewest() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L);
        mockName("Space.Page", "fr");
        mockVersions(OLD, OLD, RECENT, RECENT);
        when(this.document.getRCSVersion()).thenReturn(new Version("2.1"));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // Deleting the versions would roll the page back, so the page is listed as failed instead.
        assertEquals(new PurgeHistoryEntry(PurgeType.PAGE_HISTORY, NOW.toInstant(), USER, PurgeStatus.FAILED, 0, 1,
            List.of("Space.Page (fr)\tIllegalStateException: The current version [2.1] of the page is not the newest "
                + "version [3.1] of its history.")), entry);
        verify(this.wiki, never()).deleteDocumentVersions(any(), anyString(), anyString(), anyBoolean(), any());
        assertEquals("Failed to purge the [pageHistory] item [Space.Page (fr)] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeFailureNames() throws Exception
    {
        mockBatch(pagesQuery(PAGE_ID, HAS_OLD_VERSIONS), 1L, 2L);
        // The page is found when it is purged, but not when its failure is named; the name of the second page can't
        // be loaded at all.
        when(this.nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { "Space.Page", "" }),
            List.of()).thenThrow(new QueryException("Broken", this.nameQuery, null));
        mockVersions(OLD, RECENT);
        doThrow(new XWikiException(0, 0, "Failed to delete")).when(this.wiki)
            .deleteDocumentVersions(this.document, "1.1", "1.1", false, this.wikiContext);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // A page whose name can't be loaded is listed with its identifier.
        assertEquals(List.of("1\tXWikiException: Error number 0 in 0: Failed to delete",
            "2\tQueryException: Broken. Query statement = [null]"), entry.getFailures());
        assertEquals(PurgeStatus.FAILED, entry.getStatus());
        assertEquals("Failed to purge the [pageHistory] item [1] on wiki [subwiki].", this.logCapture.getMessage(0));
        assertEquals("Failed to purge the [pageHistory] item [2] on wiki [subwiki].", this.logCapture.getMessage(1));
    }

    /**
     * @param columns the selected columns
     * @param conditions the conditions of the limit
     * @return the statement of the query on the page translations with versions past the limit
     */
    private String pagesQuery(String columns, String conditions)
    {
        return String.format("select %s from XWikiRCSNodeInfo as rcs where rcs.id.docId > :lastId "
            + "group by rcs.id.docId having count(rcs.id.docId) > 1 and (%s) order by rcs.id.docId", columns,
            conditions);
    }

    /**
     * @param statement the statement of the query
     * @return the query created for the statement, whose setters return the query itself
     * @throws QueryException never
     */
    private Query mockQuery(String statement) throws QueryException
    {
        Query query = mock(Query.class, RETURNS_SELF);
        when(this.queryManager.createQuery(statement, Query.HQL)).thenReturn(query);
        return query;
    }

    /**
     * @param statement the statement of the query selecting the page translations to purge
     * @param ids the identifiers of the page translations to purge, as one batch
     * @throws QueryException never
     */
    private void mockBatch(String statement, Long... ids) throws QueryException
    {
        when(mockQuery(statement).<Long>execute()).thenReturn(List.of(ids));
    }

    /**
     * @param fullName the full name of the page translation
     * @param language the language of the page translation
     * @throws QueryException never
     */
    private void mockName(String fullName, String language) throws QueryException
    {
        when(this.nameQuery.<Object[]>execute()).thenReturn(List.<Object[]>of(new Object[] { fullName, language }));
    }

    /**
     * Set the history of the page, whose versions are numbered 1.1, 1.2, 2.1, 3.1 and so on. The newest version is the
     * current version of the page.
     *
     * @param dates the dates of the versions, oldest first
     */
    private void mockVersions(Date... dates)
    {
        List<XWikiRCSNodeInfo> versions = new ArrayList<>();
        for (int i = 0; i < dates.length; i++) {
            XWikiRCSNodeInfo version =
                new XWikiRCSNodeInfo(new XWikiRCSNodeId(1, new Version(i < 2 ? "1." + (i + 1) : i + ".1")));
            version.setDate(dates[i]);
            versions.add(version);
        }
        Version latestVersion = versions.get(versions.size() - 1).getVersion();
        // The archive lists the newest version first.
        Collections.reverse(versions);
        when(this.archive.getNodes()).thenReturn(versions);
        when(this.archive.getLatestVersion()).thenReturn(latestVersion);
        when(this.document.getRCSVersion()).thenReturn(latestVersion);
    }
}
