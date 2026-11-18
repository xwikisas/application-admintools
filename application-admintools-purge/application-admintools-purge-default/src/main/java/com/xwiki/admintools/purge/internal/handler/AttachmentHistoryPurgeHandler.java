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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the history of the attachments of a wiki, with the limits described by {@link AbstractHistoryPurgeHandler}.
 * The items of the purge are the pages, so that each page is loaded at most once: the history of each attachment of a
 * page is purged on its own, each deleted version is counted as removed, and a page is listed as failed with the
 * attachments whose history couldn't be purged. Only the histories stored on the file system are purged: the number of
 * other attachments is listed first in the failures of the history entry, without being counted as failed.
 * <p>
 * The history of an attachment is read without loading its page, which is loaded only if versions have to be deleted.
 * The platform has no API to delete some versions of an attachment, so the file store is used in three steps that only
 * write the list of versions, never their content: save a history listing only the versions to delete, delete the
 * history, which deletes the files of the listed versions, then save a history listing the versions kept, whose files
 * are still there. The stored history and the current version of the attachment are checked again before each step:
 * if an upload changed them, the attachment is left for the next purge, and its history is repaired so that it lists
 * the versions whose files exist. If a step fails, the history is repaired the same way. An upload saved between the
 * last check and the deletion of the history can still lose the file of its version from the history: the current
 * version is added back by the next purge.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(AttachmentHistoryPurgeHandler.HINT)
@Singleton
public class AttachmentHistoryPurgeHandler extends AbstractHistoryPurgeHandler
{
    /**
     * The hint for the component, which is the identifier of {@link PurgeType#ATTACHMENT_HISTORY}.
     */
    public static final String HINT = "attachmentHistory";

    /**
     * The item of the failure line listing the number of attachments skipped since their history is not stored on the
     * file system, followed by the separator and that number. The failures page of the user interface translates it.
     */
    private static final String SKIPPED_ATTACHMENTS = "skippedAttachments";

    /**
     * The hint of the file store of the attachment histories, the only one that is purged.
     */
    private static final String FILE_STORE = "file";

    /**
     * The version of an attachment that was uploaded once, which has no old version.
     */
    private static final String SINGLE_VERSION = "1.1";

    /**
     * Orders the versions oldest first. The natural order of the versions puts the newest first.
     */
    private static final Comparator<Version> OLDEST_FIRST = (first, second) -> second.compareTo(first);

    private static final String STORE = "store";

    private static final String FIRST_VERSION = "firstVersion";

    private static final String LAST_ID = "lastId";

    private static final String PAGE_ID = "pageId";

    /**
     * Selects the attachments whose history is stored on the file system and has old versions. An attachment saved
     * without the file store has an empty store, which is the store of the database.
     */
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

    @Inject
    @Named(FILE_STORE)
    private Provider<AttachmentVersioningStore> fileStoreProvider;

    /**
     * Purge the history of the attachments, and list the number of attachments skipped since their history is not
     * stored on the file system first in the failures, without counting them as failed.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param triggeredBy the user who started the purge, or {@code null} for a scheduled purge
     * @return the history entry of the purge
     */
    @Override
    public PurgeHistoryEntry purge(String wikiId, PurgeTypeSettings limit, ZonedDateTime now,
        DocumentReference triggeredBy)
    {
        PurgeHistoryEntry entry = super.purge(wikiId, limit, now, triggeredBy);
        long skipped = countSkipped(wikiId);
        if (skipped == 0) {
            return entry;
        }
        List<String> failures = new ArrayList<>();
        failures.add(SKIPPED_ATTACHMENTS + FAILURE_SEPARATOR + skipped);
        failures.addAll(entry.getFailures());
        return new PurgeHistoryEntry(entry.getType(), entry.getDate(), entry.getTriggeredBy().orElse(null),
            entry.getStatus(), entry.getRemovedCount(), entry.getFailedCount(), failures);
    }

    /**
     * Count the attachments that may have versions past the limit, up to {@link #MAX_COUNT}, from their version
     * numbers only, without loading their histories: an attachment at version {@code 1.N} has at most N versions, and
     * the dates of its versions are unknown. The count is thus an upper bound, and the purge deletes only the versions
     * that are really past the limit. The version number doesn't decrease after a purge, so the count rarely goes back
     * to 0.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @return the maximum number of attachments with versions past the limit, at most {@link #MAX_COUNT}
     * @throws QueryException if the attachments can't be loaded
     * @throws IllegalArgumentException if the limit is incomplete
     */
    @Override
    public long countExpired(String wikiId, PurgeTypeSettings limit, ZonedDateTime now) throws QueryException
    {
        checkLimit(limit);
        // Without a number of versions kept, only the current version is always kept.
        long keptVersions = limit.getMaxVersions().orElse(1);
        long count = 0;
        long lastId = Long.MIN_VALUE;
        List<Object[]> attachments;
        do {
            attachments = createCandidatesQuery(COUNT_QUERY, wikiId).bindValue(LAST_ID, lastId)
                .setLimit(COUNT_BATCH_SIZE).execute();
            for (Object[] attachment : attachments) {
                lastId = (Long) attachment[0];
                if (getVersionNumber((String) attachment[1]) > keptVersions) {
                    count++;
                }
            }
        } while (attachments.size() == COUNT_BATCH_SIZE && count < MAX_COUNT);
        return Math.min(count, MAX_COUNT);
    }

    @Override
    protected PurgeType getType()
    {
        return PurgeType.ATTACHMENT_HISTORY;
    }

    @Override
    protected List<Long> getBatch(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, long lastId)
        throws QueryException
    {
        return createCandidatesQuery(PAGES_QUERY, wikiId).bindValue(LAST_ID, lastId).setLimit(BATCH_SIZE).execute();
    }

    /**
     * Delete the versions past the limit of the attachments of a page, oldest first. The current version of an
     * attachment is never deleted. An attachment that no longer exists, or that was changed during the purge, is
     * skipped.
     *
     * @param id the identifier of the page
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param wikiContext the context, whose wiki is the wiki of the page
     * @return the number of deleted versions
     * @throws QueryException if the attachments of the page can't be loaded
     * @throws PartialPurgeException if the history of some attachments couldn't be purged
     */
    @Override
    protected long purgeItem(long id, PurgeTypeSettings limit, ZonedDateTime now, XWikiContext wikiContext)
        throws QueryException, PartialPurgeException
    {
        String wikiId = wikiContext.getWikiId();
        List<Object[]> attachments =
            createCandidatesQuery(PAGE_ATTACHMENTS_QUERY, wikiId).bindValue(PAGE_ID, id).execute();
        if (attachments.isEmpty()) {
            this.logger.debug("Skipped the page with identifier [{}] on wiki [{}], since it no longer has attachments "
                + "with old versions.", id, wikiId);
            return 0;
        }
        DocumentReference reference =
            this.referenceResolver.resolve((String) attachments.get(0)[0], new WikiReference(wikiId));
        Integer maxVersions = limit.getMaxVersions().orElse(null);
        long removedCount = 0;
        List<String> failures = new ArrayList<>();
        for (Object[] attachment : attachments) {
            String fileName = (String) attachment[1];
            String version = (String) attachment[2];
            // An attachment can't have more versions than its version number, so its history isn't loaded.
            if (maxVersions == null || getVersionNumber(version) > maxVersions) {
                try {
                    removedCount += purgeAttachment(reference, fileName, version, limit, now, wikiContext);
                } catch (XWikiException | RuntimeException e) {
                    this.logger.warn("Failed to purge the history of the attachment [{}] of the page [{}].", fileName,
                        reference, e);
                    failures.add(fileName + ": " + ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new PartialPurgeException(String.join("; ", failures), removedCount);
        }
        return removedCount;
    }

    @Override
    protected Optional<String> loadItemName(String wikiId, long id) throws QueryException
    {
        List<String> names =
            this.queryManager.createQuery(PAGE_NAME_QUERY, Query.HQL).setWiki(wikiId).bindValue("id", id).execute();
        return names.stream().findFirst();
    }

    /**
     * Delete the versions of an attachment that are past the limit. Its history is first read without loading its
     * page, which is loaded only if versions have to be deleted, or if the history doesn't list the current version,
     * e.g. after a purge stopped by a crash: the current version is then added back.
     *
     * @param reference the page of the attachment
     * @param fileName the name of the attachment
     * @param version the current version of the attachment, as stored in the database
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param context the context, whose wiki is the wiki of the attachment
     * @return the number of deleted versions
     * @throws XWikiException if the page or the history can't be loaded, saved or deleted
     */
    private long purgeAttachment(DocumentReference reference, String fileName, String version,
        PurgeTypeSettings limit, ZonedDateTime now, XWikiContext context) throws XWikiException
    {
        AttachmentVersioningStore store = this.fileStoreProvider.get();
        XWikiAttachment detachedAttachment = new XWikiAttachment(new XWikiDocument(reference), fileName);
        detachedAttachment.setVersion(version);
        List<XWikiAttachment> revisions = loadRevisions(store, detachedAttachment, context);
        Version currentVersion = detachedAttachment.getRCSVersion();
        if (revisions.stream().anyMatch(revision -> revision.getRCSVersion().isGreaterThan(currentVersion))) {
            this.logger.debug("Skipped the attachment [{}], since a newer version is being uploaded.",
                detachedAttachment.getReference());
            return 0;
        }
        boolean complete =
            !revisions.isEmpty() && revisions.get(revisions.size() - 1).getRCSVersion().equals(currentVersion);
        if (complete && getVersionsToPurge(revisions, limit, now) == 0) {
            return 0;
        }
        XWikiAttachment attachment = context.getWiki().getDocument(reference, context).getExactAttachment(fileName);
        if (attachment == null || !FILE_STORE.equals(attachment.getArchiveStore())
            || !version.equals(attachment.getVersion()))
        {
            this.logger.debug("Skipped the attachment [{}], since it was changed.", detachedAttachment.getReference());
            return 0;
        }
        try {
            if (!complete) {
                revisions.add(attachment);
                // Only the list of versions is written, unless the file of the current version is missing too.
                store.saveArchive(new RevisionsArchive(attachment, revisions), context, true);
                this.logger.info("Added the current version [{}] of the attachment [{}] back to its history.", version,
                    attachment.getReference());
            }
            int versionsToPurge = getVersionsToPurge(revisions, limit, now);
            return versionsToPurge == 0 ? 0 : deleteVersions(store, attachment, revisions, versionsToPurge, context);
        } finally {
            // The history loaded by the attachment of the cached page may list deleted versions: it is reloaded from
            // the store when needed.
            attachment.setAttachment_archive(null);
        }
    }

    /**
     * @param revisions the versions of an attachment, oldest first
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @return the number of oldest versions past the limit
     */
    private int getVersionsToPurge(List<XWikiAttachment> revisions, PurgeTypeSettings limit, ZonedDateTime now)
    {
        // A version without a date is never old enough.
        List<Instant> dates = revisions.stream()
            .map(revision -> Optional.ofNullable(toInstant(revision.getDate())).orElse(Instant.MAX))
            .collect(Collectors.toList());
        return (int) getVersionsToPurge(revisions.size(), getLeadingOldVersions(dates, getExpirationDate(limit, now)),
            limit.getMaxVersions().orElse(null));
    }

    /**
     * Delete the oldest versions of an attachment in three steps that only write the list of versions, checking the
     * attachment again before each step.
     *
     * @param store the file store of the attachment histories
     * @param attachment the attachment
     * @param revisions the versions of the attachment, oldest first, the current one last
     * @param versionsToPurge the number of oldest versions to delete, lower than the number of versions
     * @param context the context, whose wiki is the wiki of the attachment
     * @return the number of deleted versions, or 0 if the attachment was changed during the purge
     * @throws XWikiException if the history can't be loaded, saved or deleted
     */
    private long deleteVersions(AttachmentVersioningStore store, XWikiAttachment attachment,
        List<XWikiAttachment> revisions, int versionsToPurge, XWikiContext context) throws XWikiException
    {
        List<XWikiAttachment> toDelete = revisions.subList(0, versionsToPurge);
        List<XWikiAttachment> toKeep = revisions.subList(versionsToPurge, revisions.size());
        // The versions the stored history should list, the versions whose files were deleted, and the versions whose
        // files exist.
        List<XWikiAttachment> expected = revisions;
        List<XWikiAttachment> deleted = List.of();
        List<XWikiAttachment> existing = revisions;
        try {
            if (!canWrite(store, attachment, expected, deleted, existing, context)) {
                return 0;
            }
            store.saveArchive(new RevisionsArchive(attachment, toDelete), context, true);
            expected = toDelete;
            if (!canWrite(store, attachment, expected, deleted, existing, context)) {
                return 0;
            }
            store.deleteArchive(attachment, context, true);
            expected = List.of();
            deleted = toDelete;
            existing = toKeep;
            if (!canWrite(store, attachment, expected, deleted, existing, context)) {
                return 0;
            }
            store.saveArchive(new RevisionsArchive(attachment, toKeep), context, true);
            return versionsToPurge;
        } catch (XWikiException | RuntimeException e) {
            repair(store, attachment, deleted, existing, context);
            throw e;
        }
    }

    /**
     * Check that the stored history and the current version of an attachment didn't change since the last step. If
     * they changed, e.g. because of an upload, the history is repaired and the attachment is left for the next purge.
     *
     * @param store the file store of the attachment histories
     * @param attachment the attachment
     * @param expected the versions the stored history should list
     * @param deleted the versions whose files were deleted by the purge
     * @param existing the versions whose files exist, as known by the purge
     * @param context the context, whose wiki is the wiki of the attachment
     * @return {@code true} if the next step can be done, {@code false} if the attachment was changed
     * @throws XWikiException if the stored history or the page of the attachment can't be loaded
     */
    private boolean canWrite(AttachmentVersioningStore store, XWikiAttachment attachment,
        List<XWikiAttachment> expected, List<XWikiAttachment> deleted, List<XWikiAttachment> existing,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachment current = context.getWiki()
            .getDocument(attachment.getReference().getDocumentReference(), context)
            .getExactAttachment(attachment.getFilename());
        if (getVersions(loadRevisions(store, attachment, context)).equals(getVersions(expected)) && current != null
            && attachment.getVersion().equals(current.getVersion()))
        {
            return true;
        }
        this.logger.debug("Skipped the attachment [{}], since it was changed during the purge.",
            attachment.getReference());
        repair(store, attachment, deleted, existing, context);
        return false;
    }

    /**
     * Repair the stored history of an attachment after a step failed or the attachment was changed: it lists the
     * versions whose files exist, i.e. the versions known by the purge and the versions saved by others, except the
     * versions whose files were deleted. Only the list of versions is written, and only if it changes. A failure is
     * logged with the versions, so that the history can be fixed.
     *
     * @param store the file store of the attachment histories
     * @param attachment the attachment
     * @param deleted the versions whose files were deleted by the purge
     * @param existing the versions whose files exist, as known by the purge
     * @param context the context, whose wiki is the wiki of the attachment
     */
    private void repair(AttachmentVersioningStore store, XWikiAttachment attachment, List<XWikiAttachment> deleted,
        List<XWikiAttachment> existing, XWikiContext context)
    {
        Map<String, XWikiAttachment> listed = new LinkedHashMap<>();
        existing.forEach(revision -> listed.put(revision.getVersion(), revision));
        try {
            List<XWikiAttachment> stored = loadRevisions(store, attachment, context);
            Set<String> deletedVersions = Set.copyOf(getVersions(deleted));
            stored.stream().filter(revision -> !deletedVersions.contains(revision.getVersion()))
                .forEach(revision -> listed.putIfAbsent(revision.getVersion(), revision));
            List<XWikiAttachment> revisions = new ArrayList<>(listed.values());
            if (!getVersions(revisions).equals(getVersions(stored))) {
                store.saveArchive(new RevisionsArchive(attachment, revisions), context, true);
                this.logger.info("Repaired the history of the attachment [{}], which now lists the versions {}.",
                    attachment.getReference(), getVersions(revisions));
            }
        } catch (Exception e) {
            this.logger.error("Failed to repair the history of the attachment [{}], which should list the versions {}.",
                attachment.getReference(), listed.keySet(), e);
        }
    }

    /**
     * Load the stored history of an attachment, ignoring the history cached by the attachment.
     *
     * @param store the file store of the attachment histories
     * @param attachment the attachment
     * @param context the context, whose wiki is the wiki of the attachment
     * @return the versions of the attachment, oldest first, whose content is read from their files when needed
     * @throws XWikiException if the history can't be loaded
     */
    private List<XWikiAttachment> loadRevisions(AttachmentVersioningStore store, XWikiAttachment attachment,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachmentArchive archive = store.loadArchive(attachment, context, true);
        List<Version> versions = new ArrayList<>(Arrays.asList(archive.getVersions()));
        versions.sort(OLDEST_FIRST);
        List<XWikiAttachment> revisions = new ArrayList<>();
        for (Version version : versions) {
            XWikiAttachment revision = archive.getRevision(attachment, version.toString(), context);
            if (revision != null) {
                revisions.add(revision);
            }
        }
        return revisions;
    }

    /**
     * @param revisions the versions of an attachment
     * @return their version numbers, sorted
     */
    private List<String> getVersions(List<XWikiAttachment> revisions)
    {
        return revisions.stream().map(XWikiAttachment::getVersion).sorted().collect(Collectors.toList());
    }

    /**
     * @param version the version of an attachment, {@code 1.N} for its N-th upload
     * @return N, which is the maximum number of versions of the attachment, or 0 if the version is not a number
     */
    private long getVersionNumber(String version)
    {
        return NumberUtils.toLong(StringUtils.substringAfterLast(version, "."));
    }

    /**
     * @param wikiId the identifier of the wiki
     * @return the number of attachments with old versions whose history is not stored on the file system, or 0 if
     *     they can't be counted
     */
    private long countSkipped(String wikiId)
    {
        try {
            List<Long> counts = this.queryManager.createQuery(SKIPPED_QUERY, Query.HQL).setWiki(wikiId)
                .bindValue(STORE, FILE_STORE).bindValue(FIRST_VERSION, SINGLE_VERSION).execute();
            return counts.isEmpty() || counts.get(0) == null ? 0 : counts.get(0);
        } catch (QueryException e) {
            this.logger.warn("Failed to count the attachments skipped by the purge of [{}] on wiki [{}].",
                getType().getId(), wikiId, e);
            return 0;
        }
    }

    /**
     * @param statement a query on the attachments selected by {@link #CANDIDATES}
     * @param wikiId the identifier of the wiki
     * @return the query, with the store and the first version bound
     * @throws QueryException if the query can't be created
     */
    private Query createCandidatesQuery(String statement, String wikiId) throws QueryException
    {
        return this.queryManager.createQuery(statement, Query.HQL).setWiki(wikiId).bindValue(STORE, FILE_STORE)
            .bindValue(FIRST_VERSION, SINGLE_VERSION);
    }

    /**
     * History of an attachment listing the given versions only, so that the file store writes only them. The content
     * of a version is written only if its file is missing, so saving existing versions writes only the list.
     */
    private static final class RevisionsArchive extends XWikiAttachmentArchive
    {
        private final List<XWikiAttachment> revisions;

        /**
         * Create the history of an attachment listing the given versions.
         *
         * @param attachment the attachment
         * @param revisions the versions of the attachment listed by the history
         */
        RevisionsArchive(XWikiAttachment attachment, List<XWikiAttachment> revisions)
        {
            setAttachment(attachment);
            this.revisions = List.copyOf(revisions);
        }

        @Override
        public Version[] getVersions()
        {
            // The histories list the newest version first.
            return this.revisions.stream().map(XWikiAttachment::getRCSVersion).sorted(OLDEST_FIRST.reversed())
                .toArray(Version[]::new);
        }

        @Override
        public XWikiAttachment getRevision(XWikiAttachment attachment, String rev, XWikiContext context)
        {
            return this.revisions.stream().filter(revision -> revision.getVersion().equals(rev)).findFirst()
                .orElse(null);
        }
    }
}
