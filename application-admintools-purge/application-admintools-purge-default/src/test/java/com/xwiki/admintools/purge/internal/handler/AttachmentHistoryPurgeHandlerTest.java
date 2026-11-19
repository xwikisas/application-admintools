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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
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
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachmentHistoryPurgeHandler}. The file store of the attachment histories is simulated by
 * {@link FakeFileStore}, so that the tests check the histories and the version files left by a purge.
 *
 * @version $Id$
 */
@ComponentTest
class AttachmentHistoryPurgeHandlerTest
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

    private static final String PAGE_NAME = "Space.Page";

    private static final String FILE_NAME = "file.txt";

    private static final String CANDIDATES = "attach.archiveStore = :store and attach.version <> :firstVersion";

    private static final String PAGES_QUERY = "select distinct attach.docId from XWikiAttachment as attach where "
        + CANDIDATES + " and attach.docId > :lastId order by attach.docId";

    private static final String COUNT_QUERY = "select attach.id, attach.version from XWikiAttachment as attach where "
        + CANDIDATES + " and attach.id > :lastId order by attach.id";

    private static final String PAGE_ATTACHMENTS_QUERY = "select doc.fullName, attach.filename, attach.version "
        + "from XWikiAttachment as attach, XWikiDocument as doc where doc.id = attach.docId and " + CANDIDATES
        + " and attach.docId = :pageId order by attach.filename";

    private static final String SKIPPED_QUERY = "select count(attach.id) from XWikiAttachment as attach "
        + "where (attach.archiveStore is null or attach.archiveStore <> :store) and attach.version <> :firstVersion";

    private static final String PAGE_NAME_QUERY = "select doc.fullName from XWikiDocument as doc where doc.id = :id";

    private static final String PAGE_FAILURE = "Failed to purge the [attachmentHistory] item [Space.Page] on wiki "
        + "[subwiki].";

    private static final String WRITE_FAILED = "XWikiException: Error number 0 in 0: Write failed";

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private AttachmentHistoryPurgeHandler handler;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private RetentionCalculator retentionCalculator;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    @Named("file")
    private Provider<AttachmentVersioningStore> fileStoreProvider;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    private final FakeFileStore store = new FakeFileStore();

    /**
     * The attachments of the page selected by the purge: the full name of the page, their name and their version.
     */
    private final List<Object[]> pageAttachments = new ArrayList<>();

    private Query pageNameQuery;

    private Query skippedQuery;

    @BeforeEach
    void setUp() throws Exception
    {
        when(mockQuery(PAGES_QUERY).<Long>execute()).thenReturn(List.of(1L));
        when(mockQuery(PAGE_ATTACHMENTS_QUERY).<Object[]>execute()).thenAnswer(invocation -> this.pageAttachments);
        this.pageNameQuery = mockQuery(PAGE_NAME_QUERY);
        when(this.pageNameQuery.<String>execute()).thenReturn(List.of(PAGE_NAME));
        this.skippedQuery = mockQuery(SKIPPED_QUERY);
        when(this.wikiContextProvider.get()).thenReturn(this.wikiContext);
        when(this.fileStoreProvider.get()).thenReturn(this.store);
        when(this.wikiContext.getWikiId()).thenReturn(WIKI_ID);
        when(this.wikiContext.getWiki()).thenReturn(this.wiki);
        when(this.retentionCalculator.getExpirationDate(NOW, 1, IntervalUnit.MONTHS)).thenReturn(EXPIRATION_DATE);
        when(this.referenceResolver.resolve(PAGE_NAME, new WikiReference(WIKI_ID))).thenReturn(PAGE);
        when(this.wiki.getDocument(PAGE, this.wikiContext)).thenReturn(this.document);
        when(this.document.getDocumentReference()).thenReturn(PAGE);
    }

    @Test
    void hint()
    {
        assertEquals(PurgeType.ATTACHMENT_HISTORY.getId(), AttachmentHistoryPurgeHandler.HINT);
    }

    @Test
    void countExpired() throws Exception
    {
        Query query = mockQuery(COUNT_QUERY);
        when(query.<Object[]>execute()).thenReturn(List.of(new Object[] { 1L, "1.5" }, new Object[] { 2L, "1.2" },
            new Object[] { 3L, "1.3" }, new Object[] { 4L, "broken" }));

        // The attachments are counted, not their versions. The version number is the most versions an attachment can
        // have: only the attachment with 5 versions can have more than 3 versions.
        assertEquals(1,
            this.handler.countExpired(WIKI_ID, new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 3), NOW));
        // The dates of the versions are unknown, so an age limit may purge the old versions of every attachment.
        assertEquals(3, this.handler.countExpired(WIKI_ID, AGE_LIMIT, NOW));
        // With both limits, a version is purged only if it is beyond the versions kept.
        assertEquals(2, this.handler.countExpired(WIKI_ID,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, 2), NOW));

        verify(query, times(3)).setWiki(WIKI_ID);
        verify(query, times(3)).bindValue("store", "file");
        verify(query, times(3)).bindValue("firstVersion", "1.1");
        verify(query, times(3)).bindValue("lastId", Long.MIN_VALUE);
        verify(query, times(3)).setLimit(1000);
    }

    @Test
    void countExpiredStopsAtTheMaximum() throws Exception
    {
        Query query = mockQuery(COUNT_QUERY);
        List<Object[]> fullBatch = new ArrayList<>();
        for (long id = 1; id <= 1000; id++) {
            fullBatch.add(new Object[] { id, "1.2" });
        }
        when(query.<Object[]>execute()).thenReturn(fullBatch);

        // Counting can be slow, so it stops at 1000 attachments, without loading the next batch.
        assertEquals(1000, this.handler.countExpired(WIKI_ID, AGE_LIMIT, NOW));

        verify(query).setLimit(1000);
    }

    @Test
    void invalidLimit()
    {
        PurgeTypeSettings noLimit = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, null);

        assertEquals("The purge of [attachmentHistory] needs an age limit, a number of versions, or both.",
            assertThrows(IllegalArgumentException.class, () -> this.handler.countExpired(WIKI_ID, noLimit, NOW))
                .getMessage());
    }

    @Test
    void purgeOldVersions() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT, RECENT);

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        assertEquals(new PurgeHistoryEntry(PurgeType.ATTACHMENT_HISTORY, NOW.toInstant(), USER, PurgeStatus.SUCCESS,
            2, 0, List.of()), entry);
        assertEquals(List.of("1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(Set.of("1.3", "1.4"), this.store.getFiles(FILE_NAME));
        // Only the list of versions was written, never their content.
        assertEquals(List.of(), this.store.writtenContents);
    }

    @Test
    void purgeWithBothLimits() throws Exception
    {
        PurgeTypeSettings limit = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, 2);
        mockAttachment(FILE_NAME, OLD, OLD, OLD, RECENT);

        // The 2 newest versions are kept, although one of them is too old.
        assertEquals(2, this.handler.purge(WIKI_ID, limit, NOW, USER).getRemovedCount());
        assertEquals(List.of("1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));

        this.pageAttachments.clear();
        mockAttachment(FILE_NAME, OLD, RECENT, RECENT, RECENT);

        // Only the first version is too old, although there are 2 versions beyond the 2 newest ones.
        assertEquals(1, this.handler.purge(WIKI_ID, limit, NOW, USER).getRemovedCount());
        assertEquals(List.of("1.2", "1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));
    }

    @Test
    void purgeKeepsTheCurrentVersion() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, OLD);

        assertEquals(2, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        assertEquals(List.of("1.3"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(Set.of("1.3"), this.store.getFiles(FILE_NAME));
    }

    @Test
    void purgeStopsAtTheFirstRecentVersion() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, RECENT, OLD, RECENT);

        assertEquals(1, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        assertEquals(List.of("1.2", "1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));
    }

    @Test
    void purgeWithoutVersionsToPurgeDoesntLoadThePage() throws Exception
    {
        mockAttachment(FILE_NAME, RECENT, RECENT, RECENT);

        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        // The history is read without the page, which is loaded only when versions have to be deleted.
        assertEquals(1, this.store.loads);
        verify(this.wiki, never()).getDocument(any(DocumentReference.class), any(XWikiContext.class));
    }

    @Test
    void purgeSkipsAttachmentsWithFewVersions() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, OLD);

        // An attachment at version 1.3 has at most 3 versions, so its history isn't even loaded.
        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 3), NOW, USER);

        assertEquals(0, entry.getRemovedCount());
        assertEquals(0, this.store.loads);
    }

    @Test
    void purgeSkipsChangedAttachments() throws Exception
    {
        // The page no longer has attachments with old versions.
        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        // The attachment was deleted from the page after its history was read.
        when(this.document.getExactAttachment(FILE_NAME)).thenReturn(null);
        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());
        // The attachment was uploaded again after its history was read.
        mockCurrentVersion("1.4");
        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());
        // The history of the attachment is now stored in the database.
        XWikiAttachment attachment = mockCurrentVersion("1.3");
        attachment.setArchiveStore("hibernate");
        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        assertEquals(List.of("1.1", "1.2", "1.3"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(0, this.store.writes);
    }

    @Test
    void purgeHistoryWithoutTheCurrentVersion() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, OLD);
        // A purge stopped by a crash left a history without the current version, whose content is stored apart.
        this.store.getHistory(FILE_NAME).metadata.remove(2);

        // The newest version listed by the history is kept.
        assertEquals(1, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        assertEquals(List.of("1.2"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(List.of(), this.store.writtenContents);
    }

    @Test
    void purgeRepairsTheHistoryWhenTheAttachmentIsUploadedBeforeTheHistoryIsDeleted() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        // An upload saves the history listing only the old versions with its own, after the first step.
        this.store.beforeLoad.put(3, () -> uploadVersion(List.of("1.1", "1.2")));

        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        // Nothing is deleted, and the history lists every version again.
        assertEquals(List.of("1.1", "1.2", "1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(Set.of("1.1", "1.2", "1.3", "1.4"), this.store.getFiles(FILE_NAME));
    }

    @Test
    void purgeRepairsTheHistoryWhenItIsSavedAfterItIsDeleted() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        // An upload saves the history it loaded before the old versions were deleted, with its own version.
        this.store.beforeLoad.put(4, () -> uploadVersion(List.of("1.1", "1.2", "1.3")));

        assertEquals(0, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getRemovedCount());

        // The history no longer lists the deleted versions.
        assertEquals(List.of("1.3", "1.4"), this.store.getStoredVersions(FILE_NAME));
        assertEquals(Set.of("1.3", "1.4"), this.store.getFiles(FILE_NAME));
    }

    @Test
    void purgeRepairsTheHistoryWhenAStepFails() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        // The deletion of the history fails.
        this.store.failingWrite = 2;

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        assertEquals(PurgeStatus.FAILED, entry.getStatus());
        assertEquals(List.of(PAGE_NAME + "\tfile.txt: " + WRITE_FAILED), entry.getFailures());
        assertEquals(List.of("1.1", "1.2", "1.3"), this.store.getStoredVersions(FILE_NAME));
        assertEquals("Failed to purge the history of the attachment [file.txt] of the page [subwiki:Space.Page].",
            this.logCapture.getMessage(0));
        assertEquals(PAGE_FAILURE, this.logCapture.getMessage(1));
    }

    @Test
    void purgeLogsTheVersionsWhenTheHistoryCantBeRepaired() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        // The last step fails, and repairing the history fails too.
        this.store.failingWrite = 3;
        this.store.failingWrites = 2;

        assertEquals(1, this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getFailedCount());

        assertEquals(Set.of("1.3"), this.store.getFiles(FILE_NAME));
        assertEquals("Failed to repair the history of the attachment [Attachment subwiki:Space.Page@file.txt], which "
            + "should list the versions [1.3].", this.logCapture.getMessage(0));
        assertEquals("Failed to purge the history of the attachment [file.txt] of the page [subwiki:Space.Page].",
            this.logCapture.getMessage(1));
        assertEquals(PAGE_FAILURE, this.logCapture.getMessage(2));
    }

    @Test
    void purgeCountsThePartialPurgeOfAPage() throws Exception
    {
        mockAttachment("a.txt", OLD, OLD, RECENT);
        mockAttachment("b.txt", OLD, OLD, RECENT);
        this.store.failingFile = "b.txt";
        this.store.failingWrite = 1;

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // The page is listed as failed with its failed attachment, and the versions deleted from the other one count.
        assertEquals(new PurgeHistoryEntry(PurgeType.ATTACHMENT_HISTORY, NOW.toInstant(), USER, PurgeStatus.PARTIAL,
            2, 1, List.of(PAGE_NAME + "\tb.txt: " + WRITE_FAILED)), entry);
        assertEquals(List.of("1.3"), this.store.getStoredVersions("a.txt"));
        assertEquals(List.of("1.1", "1.2", "1.3"), this.store.getStoredVersions("b.txt"));
        assertEquals("Failed to purge the history of the attachment [b.txt] of the page [subwiki:Space.Page].",
            this.logCapture.getMessage(0));
        assertEquals(PAGE_FAILURE, this.logCapture.getMessage(1));
    }

    @Test
    void purgeListsTheSkippedAttachments() throws Exception
    {
        mockAttachment(FILE_NAME, OLD, OLD, RECENT);
        when(this.skippedQuery.<Long>execute()).thenReturn(List.of(4L));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // The skipped attachments are listed first, with a marker translated by the user interface, without being
        // counted as failed.
        assertEquals(new PurgeHistoryEntry(PurgeType.ATTACHMENT_HISTORY, NOW.toInstant(), USER, PurgeStatus.SUCCESS,
            2, 0, List.of("skippedAttachments\t4")), entry);
        verify(this.skippedQuery).setWiki(WIKI_ID);

        when(this.skippedQuery.execute()).thenThrow(new QueryException("Broken", this.skippedQuery, null));
        assertEquals(List.of(), this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER).getFailures());
        assertEquals("Failed to count the attachments skipped by the purge of [attachmentHistory] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeFailureNames() throws Exception
    {
        // The attachments of the page can't be loaded, and neither can the name of the page.
        when(this.queryManager.createQuery(PAGE_ATTACHMENTS_QUERY, Query.HQL))
            .thenThrow(new QueryException("Broken", null, null));
        when(this.pageNameQuery.execute()).thenThrow(new QueryException("Broken", this.pageNameQuery, null));

        PurgeHistoryEntry entry = this.handler.purge(WIKI_ID, AGE_LIMIT, NOW, USER);

        // A page whose name can't be loaded is listed with its identifier.
        assertEquals(List.of("1\tQueryException: Broken"), entry.getFailures());
        assertEquals("Failed to purge the [attachmentHistory] item [1] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    /**
     * Add an attachment with old versions to the page selected by the purge, with one version per date, numbered 1.1,
     * 1.2 and so on, the newest version being its current version. Its history is stored with a file per version.
     *
     * @param fileName the name of the attachment
     * @param dates the dates of the versions, oldest first
     */
    private void mockAttachment(String fileName, Date... dates)
    {
        String currentVersion = "1." + dates.length;
        this.pageAttachments.add(new Object[] { PAGE_NAME, fileName, currentVersion });
        XWikiAttachment attachment = new XWikiAttachment(this.document, fileName);
        attachment.setVersion(currentVersion);
        attachment.setDate(dates[dates.length - 1]);
        attachment.setArchiveStore("file");
        when(this.document.getExactAttachment(fileName)).thenReturn(attachment);
        StoredHistory history = this.store.getHistory(fileName);
        history.metadata = new ArrayList<>();
        history.files.clear();
        for (int i = 0; i < dates.length; i++) {
            history.metadata.add(revision(fileName, "1." + (i + 1), dates[i]));
            history.files.add("1." + (i + 1));
        }
    }

    /**
     * Set the attachment {@link #FILE_NAME} of the page to a new instance at the given version.
     *
     * @param version the current version of the attachment
     * @return the attachment
     */
    private XWikiAttachment mockCurrentVersion(String version)
    {
        XWikiAttachment attachment = new XWikiAttachment(this.document, FILE_NAME);
        attachment.setVersion(version);
        attachment.setArchiveStore("file");
        when(this.document.getExactAttachment(FILE_NAME)).thenReturn(attachment);
        return attachment;
    }

    /**
     * Simulate an upload of {@link #FILE_NAME} at version 1.4, which saves the given versions with its own.
     *
     * @param versions the versions listed by the history loaded by the upload
     */
    private void uploadVersion(List<String> versions)
    {
        StoredHistory history = this.store.getHistory(FILE_NAME);
        history.metadata = versions.stream().map(version -> revision(version, OLD)).collect(Collectors.toList());
        history.metadata.add(revision("1.4", RECENT));
        history.files.add("1.4");
        mockCurrentVersion("1.4");
    }

    /**
     * @param version the version of the revision
     * @param date the date of the revision
     * @return a revision of {@link #FILE_NAME}
     */
    private XWikiAttachment revision(String version, Date date)
    {
        return revision(FILE_NAME, version, date);
    }

    /**
     * @param fileName the name of the attachment
     * @param version the version of the revision
     * @param date the date of the revision
     * @return a revision of the attachment
     */
    private XWikiAttachment revision(String fileName, String version, Date date)
    {
        XWikiAttachment revision = new XWikiAttachment(this.document, fileName);
        revision.setVersion(version);
        revision.setDate(date);
        return revision;
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
     * The stored history of an attachment: a metadata file listing the versions, and one file per version.
     */
    private static final class StoredHistory
    {
        /**
         * The versions listed by the metadata file, or {@code null} if there is no metadata file.
         */
        private List<XWikiAttachment> metadata;

        private final Set<String> files = new TreeSet<>();
    }

    /**
     * Simulates the file store of the attachment histories. Like the real store, saving a history writes the content
     * of a version only if its file is missing, and deleting the history deletes the files of the versions listed by
     * the stored metadata.
     */
    private static final class FakeFileStore implements AttachmentVersioningStore
    {
        private final Map<String, StoredHistory> histories = new HashMap<>();

        private final List<String> writtenContents = new ArrayList<>();

        /**
         * Actions run before the n-th load of a history, counted from 1.
         */
        private final Map<Integer, Runnable> beforeLoad = new HashMap<>();

        private int loads;

        private int writes;

        /**
         * The attachment whose writes fail, or {@code null} for every attachment.
         */
        private String failingFile;

        /**
         * The number of the first write of the failing attachment that fails, counted from 1, or 0 if no write fails.
         */
        private int failingWrite;

        private int failingWrites = 1;

        private int failingFileWrites;

        @Override
        public String getHint()
        {
            return "file";
        }

        @Override
        public XWikiAttachmentArchive loadArchive(XWikiAttachment attachment, XWikiContext context,
            boolean bTransaction)
        {
            this.loads++;
            this.beforeLoad.getOrDefault(this.loads, () -> { }).run();
            List<XWikiAttachment> metadata = getHistory(attachment.getFilename()).metadata;
            return new ListArchive(attachment, metadata != null ? metadata : List.of());
        }

        @Override
        public void saveArchive(XWikiAttachmentArchive archive, XWikiContext context, boolean bTransaction)
            throws XWikiException
        {
            StoredHistory history = getHistory(archive.getAttachment().getFilename());
            write(archive.getAttachment().getFilename());
            List<XWikiAttachment> revisions = new ArrayList<>();
            for (Version version : archive.getVersions()) {
                XWikiAttachment revision = archive.getRevision(archive.getAttachment(), version.toString(), context);
                if (history.files.add(revision.getVersion())) {
                    this.writtenContents.add(revision.getVersion());
                }
                revisions.add(revision);
            }
            history.metadata = revisions;
        }

        @Override
        public void deleteArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
            throws XWikiException
        {
            StoredHistory history = getHistory(attachment.getFilename());
            write(attachment.getFilename());
            if (history.metadata != null) {
                history.metadata.forEach(revision -> history.files.remove(revision.getVersion()));
            }
            history.metadata = null;
        }

        private StoredHistory getHistory(String fileName)
        {
            return this.histories.computeIfAbsent(fileName, key -> new StoredHistory());
        }

        private List<String> getStoredVersions(String fileName)
        {
            // The natural order of the versions puts the newest first.
            return getHistory(fileName).metadata.stream().map(XWikiAttachment::getRCSVersion)
                .sorted((first, second) -> second.compareTo(first)).map(Version::toString)
                .collect(Collectors.toList());
        }

        private Set<String> getFiles(String fileName)
        {
            return getHistory(fileName).files;
        }

        private void write(String fileName) throws XWikiException
        {
            this.writes++;
            if (this.failingFile == null || this.failingFile.equals(fileName)) {
                this.failingFileWrites++;
                if (this.failingWrite > 0 && this.failingFileWrites >= this.failingWrite
                    && this.failingFileWrites < this.failingWrite + this.failingWrites)
                {
                    throw new XWikiException(0, 0, "Write failed");
                }
            }
        }
    }

    /**
     * A stored history listing the given versions.
     */
    private static final class ListArchive extends XWikiAttachmentArchive
    {
        private final List<XWikiAttachment> revisions;

        /**
         * Create a stored history listing the given versions.
         *
         * @param attachment the attachment
         * @param revisions the versions listed by the history
         */
        ListArchive(XWikiAttachment attachment, List<XWikiAttachment> revisions)
        {
            setAttachment(attachment);
            this.revisions = List.copyOf(revisions);
        }

        @Override
        public Version[] getVersions()
        {
            return this.revisions.stream().map(XWikiAttachment::getRCSVersion).toArray(Version[]::new);
        }

        @Override
        public XWikiAttachment getRevision(XWikiAttachment attachment, String rev, XWikiContext context)
        {
            return this.revisions.stream().filter(revision -> revision.getVersion().equals(rev)).findFirst()
                .orElse(null);
        }
    }
}
