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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xwiki.admintools.purge.internal.policy.RetentionCalculator;
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the history of the pages of a wiki: the versions older than the age limit, and the oldest versions beyond the
 * number of versions kept. A version is purged as soon as it is past either limit, and the current version is always
 * kept. Each page translation has its own history, and is an item of the purge: its old versions are deleted at once,
 * as one range starting at its first version, and each deleted version is counted as removed.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(PageHistoryPurgeHandler.HINT)
@Singleton
public class PageHistoryPurgeHandler extends AbstractPurgeHandler
{
    /**
     * The hint for the component, which is the identifier of {@link PurgeType#PAGE_HISTORY}.
     */
    public static final String HINT = "pageHistory";

    /**
     * The number of page translations counted at once.
     */
    private static final int COUNT_BATCH_SIZE = 1000;

    private static final String EXPIRATION_DATE = "expirationDate";

    private static final String MAX_VERSIONS = "maxVersions";

    private static final String PAGE_ID = "rcs.id.docId";

    private static final String COLUMN_SEPARATOR = ", ";

    /**
     * Groups the versions by page translation, after the last page translation of the previous batch, and keeps the
     * ones with versions to purge. The first {@code %s} is replaced by the selected columns, the second by the
     * conditions of the limit. Only the version metadata is read, never the content.
     */
    private static final String PAGES_QUERY = "select %s from XWikiRCSNodeInfo as rcs where rcs.id.docId > :lastId "
        + "group by rcs.id.docId having count(rcs.id.docId) > 1 and (%s) order by rcs.id.docId";

    private static final String VERSION_COUNT = "count(rcs.id.docId)";

    private static final String OLD_VERSION_COUNT = "sum(case when rcs.date < :expirationDate then 1 else 0 end)";

    private static final String HAS_OLD_VERSIONS = "min(rcs.date) < :expirationDate";

    private static final String HAS_TOO_MANY_VERSIONS = "count(rcs.id.docId) > :maxVersions";

    private static final String NAME_QUERY =
        "select doc.fullName, doc.language from XWikiDocument as doc where doc.id = :id";

    @Inject
    private QueryManager queryManager;

    @Inject
    private RetentionCalculator retentionCalculator;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /**
     * Count the versions past the limit. The versions are counted from their metadata, assuming that their dates
     * follow their order, which is the case unless the history was edited: otherwise, the count can be higher than
     * the number of versions purged, since the purge stops at the first version that isn't old enough.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at, whose time zone is used for the calendar units
     * @return the number of versions past the limit
     * @throws QueryException if the versions can't be counted
     * @throws IllegalArgumentException if the limit is incomplete
     */
    @Override
    public long countExpired(String wikiId, PurgeTypeSettings limit, ZonedDateTime now) throws QueryException
    {
        checkLimit(limit);
        Instant expirationDate = getExpirationDate(limit, now);
        Integer maxVersions = getMaxVersions(limit);
        String columns = String.join(COLUMN_SEPARATOR, PAGE_ID, VERSION_COUNT);
        if (expirationDate != null) {
            columns = String.join(COLUMN_SEPARATOR, columns, OLD_VERSION_COUNT);
        }
        long count = 0;
        long lastId = Long.MIN_VALUE;
        List<Object[]> pages;
        do {
            pages = createPagesQuery(columns, wikiId, expirationDate, maxVersions, lastId)
                .setLimit(COUNT_BATCH_SIZE).execute();
            for (Object[] page : pages) {
                lastId = (Long) page[0];
                long oldVersions = expirationDate != null ? ((Number) page[2]).longValue() : 0;
                count += getVersionsToPurge(((Number) page[1]).longValue(), oldVersions, maxVersions);
            }
        } while (pages.size() == COUNT_BATCH_SIZE);
        return count;
    }

    @Override
    protected PurgeType getType()
    {
        return PurgeType.PAGE_HISTORY;
    }

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
            throw new IllegalArgumentException(
                String.format("The age limit of the purge of [%s] needs both an interval and a unit.", HINT));
        }
        if (limit.getInterval().isEmpty() && getMaxVersions(limit) == null) {
            throw new IllegalArgumentException(String.format(
                "The purge of [%s] needs an age limit, a number of versions, or both.", HINT));
        }
    }

    @Override
    protected List<Long> getBatch(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, long lastId)
        throws QueryException
    {
        return createPagesQuery(PAGE_ID, wikiId, getExpirationDate(limit, now), getMaxVersions(limit), lastId)
            .setLimit(BATCH_SIZE).execute();
    }

    /**
     * Delete the versions of a page translation that are past the limit, as one range starting at its first version.
     * The age limit stops at the first version that isn't old enough, and the current version is never deleted. The
     * history of a page that no longer exists is skipped.
     *
     * @param id the identifier of the page translation, as used by its history
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param wikiContext the context, whose wiki is the wiki of the page
     * @return the number of deleted versions
     * @throws XWikiException if the page or its history can't be loaded, or the versions can't be deleted
     * @throws QueryException if the name of the page can't be loaded
     * @throws IllegalStateException if the current version of the page is not the newest version of its history
     */
    @Override
    protected long purgeItem(long id, PurgeTypeSettings limit, ZonedDateTime now, XWikiContext wikiContext)
        throws XWikiException, QueryException
    {
        String wikiId = wikiContext.getWikiId();
        List<Object[]> names = createNameQuery(wikiId, id).execute();
        if (names.isEmpty()) {
            this.logger.debug("Skipped the history of the page with identifier [{}] on wiki [{}], since the page "
                + "doesn't exist.", id, wikiId);
            return 0;
        }
        DocumentReference reference =
            new DocumentReference(this.referenceResolver.resolve((String) names.get(0)[0], new WikiReference(wikiId)),
                LocaleUtils.toLocale((String) names.get(0)[1]));
        XWikiDocument document = wikiContext.getWiki().getDocument(reference, wikiContext);
        XWikiDocumentArchive archive = document.getDocumentArchive(wikiContext);
        // The archive lists the newest version first.
        List<XWikiRCSNodeInfo> versions = new ArrayList<>(archive.getNodes());
        Collections.reverse(versions);
        int versionsToPurge = (int) getVersionsToPurge(versions.size(),
            getLeadingOldVersions(versions, getExpirationDate(limit, now)), getMaxVersions(limit));
        if (versionsToPurge == 0) {
            return 0;
        }
        // Deleting versions up to the newest one makes the platform roll the page back to the version before it, so
        // the newest version of the history must be the current version of the page, which is never deleted.
        if (!document.getRCSVersion().equals(archive.getLatestVersion())) {
            throw new IllegalStateException(String.format(
                "The current version [%s] of the page is not the newest version [%s] of its history.",
                document.getRCSVersion(), archive.getLatestVersion()));
        }
        wikiContext.getWiki().deleteDocumentVersions(document, versions.get(0).getVersion().toString(),
            versions.get(versionsToPurge - 1).getVersion().toString(), false, wikiContext);
        return versionsToPurge;
    }

    @Override
    protected String getItemName(String wikiId, long id)
    {
        try {
            List<Object[]> names = createNameQuery(wikiId, id).execute();
            return names.isEmpty() ? String.valueOf(id)
                : getPageName((String) names.get(0)[0], (String) names.get(0)[1]);
        } catch (Exception e) {
            // The name only describes the failure, so failing to load it must not stop the purge.
            return String.valueOf(id);
        }
    }

    /**
     * Compute how many of the oldest versions of a page translation are past the limit. The current version is never
     * counted.
     *
     * @param versions the number of versions of the page translation, the current one included
     * @param oldVersions the number of versions older than the age limit, 0 if there is none
     * @param maxVersions the number of versions kept, or {@code null} if there is no such limit
     * @return the number of versions past the limit
     */
    private long getVersionsToPurge(long versions, long oldVersions, Integer maxVersions)
    {
        long tooOld = Math.min(oldVersions, versions - 1);
        long tooMany = maxVersions != null ? versions - maxVersions : 0;
        return Math.max(0, Math.max(tooOld, tooMany));
    }

    /**
     * @param versions the versions of a page translation, oldest first
     * @param expirationDate the versions before this date are past the age limit, or {@code null} if there is none
     * @return the number of versions older than the age limit before the first one that isn't
     */
    private long getLeadingOldVersions(List<XWikiRCSNodeInfo> versions, Instant expirationDate)
    {
        if (expirationDate == null) {
            return 0;
        }
        Date date = Date.from(expirationDate);
        return versions.stream().takeWhile(version -> version.getDate().before(date)).count();
    }

    /**
     * @param columns the selected columns
     * @param wikiId the identifier of the wiki
     * @param expirationDate the versions before this date are past the age limit, or {@code null} if there is none
     * @param maxVersions the number of versions kept, or {@code null} if there is no such limit
     * @param lastId the identifier of the last page translation of the previous batch
     * @return the query on the page translations with versions past the limit
     * @throws QueryException if the query can't be created
     */
    private Query createPagesQuery(String columns, String wikiId, Instant expirationDate, Integer maxVersions,
        long lastId) throws QueryException
    {
        List<String> conditions = new ArrayList<>();
        if (expirationDate != null) {
            conditions.add(HAS_OLD_VERSIONS);
        }
        if (maxVersions != null) {
            conditions.add(HAS_TOO_MANY_VERSIONS);
        }
        Query query = this.queryManager.createQuery(
            String.format(PAGES_QUERY, columns, String.join(" or ", conditions)), Query.HQL).setWiki(wikiId)
            .bindValue("lastId", lastId);
        if (expirationDate != null) {
            query.bindValue(EXPIRATION_DATE, Date.from(expirationDate));
        }
        if (maxVersions != null) {
            query.bindValue(MAX_VERSIONS, maxVersions.longValue());
        }
        return query;
    }

    /**
     * @param wikiId the identifier of the wiki
     * @param id the identifier of a page translation
     * @return the query loading the full name and the language of the page translation
     * @throws QueryException if the query can't be created
     */
    private Query createNameQuery(String wikiId, long id) throws QueryException
    {
        return this.queryManager.createQuery(NAME_QUERY, Query.HQL).setWiki(wikiId).bindValue("id", id);
    }

    /**
     * @param limit the limit of a purge
     * @param now the time the limit is applied at
     * @return the date before which the versions are past the age limit, or {@code null} if there is no age limit
     */
    private Instant getExpirationDate(PurgeTypeSettings limit, ZonedDateTime now)
    {
        return limit.getInterval().isPresent() && limit.getUnit().isPresent()
            ? this.retentionCalculator.getExpirationDate(now, limit.getInterval().get(), limit.getUnit().get()) : null;
    }

    /**
     * @param limit the limit of a purge
     * @return the number of versions kept, or {@code null} if the limit doesn't have one
     */
    private Integer getMaxVersions(PurgeTypeSettings limit)
    {
        return limit instanceof HistoryPurgeTypeSettings
            ? ((HistoryPurgeTypeSettings) limit).getMaxVersions().orElse(null) : null;
    }
}
