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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Purges the items of one recycle bin in batches. Each item is deleted on its own, so that an item that can't be
 * deleted is only reported, and the purge goes on with the next items. If the next items can't be loaded, the purge is
 * interrupted, and the history entry still reports what was done.
 *
 * @version $Id$
 * @since 1.5
 */
public abstract class AbstractPurgeHandler implements PurgeHandler
{
    /**
     * The number of items loaded at once.
     */
    private static final int BATCH_SIZE = 100;

    /**
     * The number of failures listed in a history entry, and logged. The number of failed items is always exact.
     */
    private static final int MAX_FAILURES = 100;

    /**
     * Separates the item from the error in a failure line.
     */
    private static final String FAILURE_SEPARATOR = "\t";

    private static final String EXPIRATION_DATE = "expirationDate";

    private static final String COUNT_QUERY = "select count(item.id) from %s as item where item.date < :expirationDate";

    /**
     * Loads the identifiers of the next batch of items, after the last item of the previous batch. Paging by offset
     * can't be used, since the items that can't be deleted stay in the recycle bin while the others disappear. Only the
     * identifiers are selected, so that the database can answer from its indexes.
     */
    private static final String BATCH_QUERY =
        "select item.id from %s as item where item.date < :expirationDate and item.id > :lastId order by item.id";

    /**
     * Loads the columns naming an item, only for the items that can't be deleted.
     */
    private static final String NAME_QUERY = "select %s from %s as item where item.id = :id";

    @Inject
    private QueryManager queryManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private Logger logger;

    @Override
    public long countExpired(String wikiId, Instant expirationDate) throws QueryException
    {
        List<Long> result = createExpiredItemsQuery(COUNT_QUERY, wikiId, expirationDate).execute();
        return result.get(0);
    }

    @Override
    public PurgeHistoryEntry purge(String wikiId, Instant expirationDate, Instant date, DocumentReference triggeredBy)
    {
        long removedCount = 0;
        long failedCount = 0;
        boolean interrupted = false;
        List<String> failures = new ArrayList<>();
        XWikiContext wikiContext = this.wikiContextProvider.get();
        String currentWikiId = wikiContext.getWikiId();
        // The recycle bin stores delete the items from the database of the context wiki.
        wikiContext.setWikiId(wikiId);
        try {
            long lastId = Long.MIN_VALUE;
            List<Long> batch;
            do {
                batch = getBatch(wikiId, expirationDate, lastId);
                for (Long id : batch) {
                    lastId = id;
                    if (deleteItem(wikiId, id, wikiContext, failures)) {
                        removedCount++;
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
        return new PurgeHistoryEntry(getType(), date, triggeredBy, getStatus(interrupted, removedCount, failedCount),
            removedCount, failedCount, failures);
    }

    /**
     * @return the item type purged by this handler
     */
    protected abstract PurgeType getType();

    /**
     * @return the name of the Hibernate entity of the recycle bin items, as used in queries
     */
    protected abstract String getEntityName();

    /**
     * @return the columns that name an item, prefixed with {@code item.} and separated by commas, as used in queries
     */
    protected abstract String getItemNameColumns();

    /**
     * @param item the values of {@link #getItemNameColumns()} for an item
     * @return the name of the item, as listed in the failures of a history entry
     */
    protected abstract String getItemName(Object[] item);

    /**
     * Permanently delete an item from the recycle bin of the context wiki.
     *
     * @param id the identifier of the item
     * @param wikiContext the context, whose wiki is the wiki of the item
     * @throws XWikiException if the item can't be deleted
     */
    protected abstract void delete(long id, XWikiContext wikiContext) throws XWikiException;

    /**
     * Delete one item from the recycle bin. An item that can't be deleted is listed in the failures and logged, with
     * its error, as long as the list of failures is not full.
     *
     * @param wikiId the identifier of the wiki
     * @param id the identifier of the item
     * @param wikiContext the context, whose wiki is the wiki of the item
     * @param failures the failures listed so far, to which the failure of this item is added
     * @return {@code true} if the item was deleted
     */
    private boolean deleteItem(String wikiId, long id, XWikiContext wikiContext, List<String> failures)
    {
        try {
            delete(id, wikiContext);
            return true;
        } catch (Exception e) {
            // The failures listed in the history are also logged, with the same limit.
            if (failures.size() < MAX_FAILURES) {
                String itemName = getItemName(wikiId, id);
                this.logger.warn("Failed to purge the [{}] item [{}] on wiki [{}].", getType().getId(), itemName,
                    wikiId, e);
                failures.add(getFailure(itemName, e));
            }
            return false;
        }
    }

    /**
     * Load the identifiers of the next items to purge.
     *
     * @param wikiId the identifier of the wiki
     * @param expirationDate the items deleted before this date are purged
     * @param lastId the identifier of the last item of the previous batch
     * @return the identifiers of the next items, in increasing order
     * @throws QueryException if the items can't be loaded
     */
    private List<Long> getBatch(String wikiId, Instant expirationDate, long lastId) throws QueryException
    {
        return createExpiredItemsQuery(BATCH_QUERY, wikiId, expirationDate).bindValue("lastId", lastId)
            .setLimit(BATCH_SIZE).execute();
    }

    /**
     * @param statement a query on the expired items, where {@code %s} is replaced by the entity of the items
     * @param wikiId the identifier of the wiki
     * @param expirationDate the items deleted before this date are expired
     * @return the query on the expired items of the wiki
     * @throws QueryException if the query can't be created
     */
    private Query createExpiredItemsQuery(String statement, String wikiId, Instant expirationDate)
        throws QueryException
    {
        return this.queryManager.createQuery(String.format(statement, getEntityName()), Query.HQL).setWiki(wikiId)
            .bindValue(EXPIRATION_DATE, Date.from(expirationDate));
    }

    /**
     * @param wikiId the identifier of the wiki
     * @param id the identifier of an item that couldn't be deleted
     * @return the name of the item, or its identifier if the name can't be loaded
     */
    private String getItemName(String wikiId, long id)
    {
        try {
            List<Object[]> names =
                this.queryManager.createQuery(String.format(NAME_QUERY, getItemNameColumns(), getEntityName()),
                    Query.HQL).setWiki(wikiId).bindValue("id", id).execute();
            return names.isEmpty() ? String.valueOf(id) : getItemName(names.get(0));
        } catch (Exception e) {
            // The name only describes the failure, so failing to load it must not stop the purge.
            return String.valueOf(id);
        }
    }

    /**
     * @param item the name of the item that couldn't be deleted
     * @param error the error that occurred
     * @return the failure line
     */
    private String getFailure(String item, Exception error)
    {
        return item.replace(FAILURE_SEPARATOR, " ") + FAILURE_SEPARATOR + ExceptionUtils.getRootCauseMessage(error);
    }

    /**
     * @param interrupted {@code true} if the purge stopped before going through all the expired items
     * @param removedCount the number of removed items
     * @param failedCount the number of items that couldn't be removed
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
