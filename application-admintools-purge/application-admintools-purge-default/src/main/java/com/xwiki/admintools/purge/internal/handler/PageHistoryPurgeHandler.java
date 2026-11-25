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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the history of the pages of a wiki, with the limits described by {@link AbstractHistoryPurgeHandler}. Each
 * page translation has its own history, and is an item of the purge: its old versions are deleted at once, as one
 * range starting at its first version, and each deleted version is counted as removed.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(PageHistoryPurgeHandler.HINT)
@Singleton
public class PageHistoryPurgeHandler extends AbstractHistoryPurgeHandler
{
    /**
     * The hint for the component, which is the identifier of {@link PurgeType#PAGE_HISTORY}.
     */
    public static final String HINT = "pageHistory";

    private static final String EXPIRATION_DATE = "expirationDate";

    private static final String MAX_VERSIONS = "maxVersions";

    private static final String LAST_ID = "lastId";

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

    /**
     * Count the versions past the limit, up to {@link #MAX_COUNT}. The versions are counted from their metadata,
     * assuming that their dates follow their order, which is the case unless the history was edited: otherwise, the
     * count can be higher than the number of versions purged, since the purge stops at the first version that isn't
     * old enough.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at, whose time zone is used for the calendar units
     * @return the number of versions past the limit, at most {@link #MAX_COUNT}
     * @throws QueryException if the versions can't be counted
     * @throws IllegalArgumentException if the limit is incomplete
     */
    @Override
    public long countExpired(String wikiId, PurgeTypeSettings limit, ZonedDateTime now) throws QueryException
    {
        checkLimit(limit);
        Instant expirationDate = getExpirationDate(limit, now);
        Integer maxVersions = limit.getMaxVersions().orElse(null);
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
                Long oldVersions = expirationDate != null ? ((Number) page[2]).longValue() : null;
                count += getVersionsToPurge(((Number) page[1]).longValue(), oldVersions, maxVersions);
            }
        } while (pages.size() == COUNT_BATCH_SIZE && count < MAX_COUNT);
        return Math.min(count, MAX_COUNT);
    }

    @Override
    protected PurgeType getType()
    {
        return PurgeType.PAGE_HISTORY;
    }

    @Override
    protected List<Long> getBatch(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, long lastId)
        throws QueryException
    {
        return createPagesQuery(PAGE_ID, wikiId, getExpirationDate(limit, now), limit.getMaxVersions().orElse(null),
            lastId).setLimit(BATCH_SIZE).execute();
    }

    /**
     * Delete the versions of a page translation that are past the limit, as one range starting at its first version.
     * The age limit stops at the first version that isn't old enough, and the current version is never deleted. The
     * history of a page that no longer exists is skipped. The history is loaded again from the store, since the one
     * cached by the page may be outdated, and the versions are deleted from it.
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
        // Without a cached history, the page loads it from the store; the platform then deletes the versions from it.
        document.setDocumentArchive((XWikiDocumentArchive) null);
        XWikiDocumentArchive archive = document.getDocumentArchive(wikiContext);
        // The archive lists the newest version first.
        List<XWikiRCSNodeInfo> versions = new ArrayList<>(archive.getNodes());
        Collections.reverse(versions);
        List<Instant> dates = versions.stream().map(version -> toInstant(version.getDate()))
            .collect(Collectors.toList());
        int versionsToPurge = (int) getVersionsToPurge(versions.size(),
            getLeadingOldVersions(dates, getExpirationDate(limit, now)), limit.getMaxVersions().orElse(null));
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
    protected Optional<String> loadItemName(String wikiId, long id) throws QueryException
    {
        List<Object[]> names = createNameQuery(wikiId, id).execute();
        return names.stream().findFirst().map(name -> getPageName((String) name[0], (String) name[1]));
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
            String.format(PAGES_QUERY, columns, String.join(" and ", conditions)), Query.HQL).setWiki(wikiId)
            .bindValue(LAST_ID, lastId);
        if (expirationDate != null) {
            query.bindValue(EXPIRATION_DATE, toDate(expirationDate));
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
}
