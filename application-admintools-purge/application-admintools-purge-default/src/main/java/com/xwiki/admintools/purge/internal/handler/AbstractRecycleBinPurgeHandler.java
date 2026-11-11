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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.policy.RetentionCalculator;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the items of one recycle bin: the items deleted before the expiration date computed from the interval of the
 * limit. Each item is deleted as a whole.
 *
 * @version $Id$
 * @since 1.5
 */
public abstract class AbstractRecycleBinPurgeHandler extends AbstractPurgeHandler
{
    private static final String EXPIRATION_DATE = "expirationDate";

    private static final String COUNT_QUERY = "select count(item.id) from %s as item where item.date < :expirationDate";

    /**
     * Loads the identifiers of the next batch of items, after the last item of the previous batch. Only the identifiers
     * are selected, so that the database can answer from its indexes.
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
    private RetentionCalculator retentionCalculator;

    @Override
    public long countExpired(String wikiId, PurgeTypeSettings limit, ZonedDateTime now) throws QueryException
    {
        List<Long> result = createExpiredItemsQuery(COUNT_QUERY, wikiId, limit, now).execute();
        return result.get(0);
    }

    @Override
    protected void checkLimit(PurgeTypeSettings limit)
    {
        if (limit.getInterval().isEmpty() || limit.getUnit().isEmpty()) {
            throw missingLimit();
        }
    }

    @Override
    protected List<Long> getBatch(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, long lastId)
        throws QueryException
    {
        return createExpiredItemsQuery(BATCH_QUERY, wikiId, limit, now).bindValue("lastId", lastId)
            .setLimit(BATCH_SIZE).execute();
    }

    @Override
    protected long purgeItem(long id, PurgeTypeSettings limit, ZonedDateTime now, XWikiContext wikiContext)
        throws XWikiException
    {
        delete(id, wikiContext);
        return 1;
    }

    @Override
    protected String getItemName(String wikiId, long id)
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
     * @param statement a query on the expired items, where {@code %s} is replaced by the entity of the items
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge, which must have an interval and a unit
     * @param now the time the limit is applied at
     * @return the query on the items of the wiki deleted before the expiration date
     * @throws QueryException if the query can't be created
     */
    private Query createExpiredItemsQuery(String statement, String wikiId, PurgeTypeSettings limit,
        ZonedDateTime now) throws QueryException
    {
        // The limit is checked before the query is created.
        Date expirationDate = Date.from(getExpirationDate(limit, now));
        return this.queryManager.createQuery(String.format(statement, getEntityName()), Query.HQL).setWiki(wikiId)
            .bindValue(EXPIRATION_DATE, expirationDate);
    }

    /**
     * @param limit the limit of the purge, which must have an interval and a unit
     * @param now the time the limit is applied at
     * @return the date before which the deleted items are purged
     * @throws IllegalArgumentException if the limit has no interval or no unit
     */
    private Instant getExpirationDate(PurgeTypeSettings limit, ZonedDateTime now)
    {
        return this.retentionCalculator.getExpirationDate(now, limit.getInterval().orElseThrow(this::missingLimit),
            limit.getUnit().orElseThrow(this::missingLimit));
    }

    /**
     * @return the error thrown when the limit of a purge has no interval or no unit
     */
    private IllegalArgumentException missingLimit()
    {
        return new IllegalArgumentException(
            String.format("The limit of the purge of [%s] has no interval or no unit.", getType().getId()));
    }
}
