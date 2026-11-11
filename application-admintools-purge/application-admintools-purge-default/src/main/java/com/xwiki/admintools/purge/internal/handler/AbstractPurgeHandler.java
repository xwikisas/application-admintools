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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the items of one type in batches, in the context of the purged wiki. The limit is checked first, so that an
 * incomplete limit fails the purge instead of interrupting it. Each item is purged on its own, so that an item that
 * can't be purged is only reported, and the purge goes on with the next items. If the next items can't be loaded, the
 * purge is interrupted, and the history entry still reports what was done.
 *
 * @version $Id$
 * @since 1.5
 */
public abstract class AbstractPurgeHandler implements PurgeHandler
{
    /**
     * The number of items loaded at once: {@link #getBatch} loads at most this many items, and the purge stops after a
     * batch with fewer items.
     */
    protected static final int BATCH_SIZE = 100;

    /**
     * The number of failures listed in a history entry, and logged. The number of failed items is always exact.
     */
    private static final int MAX_FAILURES = 100;

    /**
     * Separates the item from the error in a failure line.
     */
    private static final String FAILURE_SEPARATOR = "\t";

    /**
     * The logger, shared with the subclasses: a subclass field with the same name would hide this one from the
     * component manager, which injects the fields by name.
     */
    @Inject
    protected Logger logger;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Override
    public PurgeHistoryEntry purge(String wikiId, PurgeTypeSettings limit, ZonedDateTime now,
        DocumentReference triggeredBy)
    {
        checkLimit(limit);
        long removedCount = 0;
        long failedCount = 0;
        boolean interrupted = false;
        List<String> failures = new ArrayList<>();
        XWikiContext wikiContext = this.wikiContextProvider.get();
        String currentWikiId = wikiContext.getWikiId();
        // The stores delete the items from the database of the context wiki.
        wikiContext.setWikiId(wikiId);
        try {
            long lastId = Long.MIN_VALUE;
            List<Long> batch;
            do {
                batch = getBatch(wikiId, limit, now, lastId);
                for (Long id : batch) {
                    lastId = id;
                    OptionalLong removed = attemptItemPurge(wikiId, id, limit, now, wikiContext, failures);
                    if (removed.isPresent()) {
                        removedCount += removed.getAsLong();
                    } else {
                        failedCount++;
                    }
                }
            } while (batch.size() == BATCH_SIZE);
        } catch (Exception e) {
            this.logger.error("Interrupted the purge of [{}] on wiki [{}], since the next items can't be loaded.",
                getType().getId(), wikiId, e);
            interrupted = true;
        } finally {
            wikiContext.setWikiId(currentWikiId);
        }
        return new PurgeHistoryEntry(getType(), now.toInstant(), triggeredBy,
            getStatus(interrupted, removedCount, failedCount), removedCount, failedCount, failures);
    }

    /**
     * @return the item type purged by this handler
     */
    protected abstract PurgeType getType();

    /**
     * Check that a limit has the values this item type needs.
     *
     * @param limit the limit of a purge
     * @throws IllegalArgumentException if the limit is incomplete
     */
    protected abstract void checkLimit(PurgeTypeSettings limit);

    /**
     * Load the identifiers of the next {@link #BATCH_SIZE} items to purge, after the last item of the previous batch.
     * Paging by offset can't be used, since the items that can't be purged stay while the others disappear.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param lastId the identifier of the last item of the previous batch
     * @return the identifiers of the next items, at most {@link #BATCH_SIZE}, in increasing order
     * @throws QueryException if the items can't be loaded
     */
    protected abstract List<Long> getBatch(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, long lastId)
        throws QueryException;

    /**
     * Permanently purge an item of the context wiki.
     *
     * @param id the identifier of the item
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param wikiContext the context, whose wiki is the wiki of the item
     * @return the number of removed units, counted in the history entry: 1 for an item that is deleted as a whole
     * @throws XWikiException if the item can't be purged
     * @throws QueryException if the data needed to purge the item can't be loaded
     */
    protected abstract long purgeItem(long id, PurgeTypeSettings limit, ZonedDateTime now, XWikiContext wikiContext)
        throws XWikiException, QueryException;

    /**
     * @param wikiId the identifier of the wiki
     * @param id the identifier of an item that couldn't be purged
     * @return the name of the item, as listed in the failures of a history entry, or its identifier if the name can't
     *     be loaded
     */
    protected abstract String getItemName(String wikiId, long id);

    /**
     * @param fullName the full name of a page
     * @param language the language of the page translation, empty for the default translation
     * @return the name of the page translation, as listed in the failures of a history entry
     */
    protected String getPageName(String fullName, String language)
    {
        return StringUtils.isEmpty(language) ? fullName : String.format("%s (%s)", fullName, language);
    }

    /**
     * Purge one item. An item that can't be purged is listed in the failures and logged, with its error, as long as the
     * list of failures is not full.
     *
     * @param wikiId the identifier of the wiki
     * @param id the identifier of the item
     * @param limit the limit of the purge
     * @param now the time the limit is applied at
     * @param wikiContext the context, whose wiki is the wiki of the item
     * @param failures the failures listed so far, to which the failure of this item is added
     * @return the number of removed units, or an empty value if the item couldn't be purged
     */
    private OptionalLong attemptItemPurge(String wikiId, long id, PurgeTypeSettings limit, ZonedDateTime now,
        XWikiContext wikiContext, List<String> failures)
    {
        try {
            return OptionalLong.of(purgeItem(id, limit, now, wikiContext));
        } catch (Exception e) {
            // The failures listed in the history are also logged, with the same limit.
            if (failures.size() < MAX_FAILURES) {
                String itemName = getItemName(wikiId, id);
                this.logger.warn("Failed to purge the [{}] item [{}] on wiki [{}].", getType().getId(), itemName,
                    wikiId, e);
                failures.add(getFailure(itemName, e));
            }
            return OptionalLong.empty();
        }
    }

    /**
     * @param item the name of the item that couldn't be purged
     * @param error the error that occurred
     * @return the failure line
     */
    private String getFailure(String item, Exception error)
    {
        return item.replace(FAILURE_SEPARATOR, " ") + FAILURE_SEPARATOR + ExceptionUtils.getRootCauseMessage(error);
    }

    /**
     * @param interrupted {@code true} if the purge stopped before going through all the items to purge
     * @param removedCount the number of removed units
     * @param failedCount the number of items that couldn't be purged
     * @return the outcome of the purge
     */
    private PurgeStatus getStatus(boolean interrupted, long removedCount, long failedCount)
    {
        if (interrupted) {
            return PurgeStatus.INTERRUPTED;
        }
        if (failedCount == 0) {
            return PurgeStatus.SUCCESS;
        }
        return removedCount > 0 ? PurgeStatus.PARTIAL : PurgeStatus.FAILED;
    }
}
