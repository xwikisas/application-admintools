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
package com.xwiki.admintools.purge;

import java.time.Instant;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Purges one item type from the recycle bin of a wiki. There is one implementation per {@link PurgeType}, whose hint
 * is the identifier of the type. No rights are checked: the callers must do it.
 *
 * @version $Id$
 * @since 1.5
 */
@Role
@Unstable
public interface PurgeHandler
{
    /**
     * Count the items of a wiki that were deleted before the expiration date, which a purge with the same expiration
     * date would remove.
     *
     * @param wikiId the identifier of the wiki
     * @param expirationDate the items deleted before this date are counted
     * @return the number of items deleted before the expiration date
     * @throws QueryException if the items can't be counted
     */
    long countExpired(String wikiId, Instant expirationDate) throws QueryException;

    /**
     * Permanently delete the items of a wiki that were deleted before the expiration date. An item that can't be
     * deleted is skipped and reported in the returned entry, and the purge goes on with the next items. If the next
     * items can't be loaded, the purge stops, and the returned entry has the {@link PurgeStatus#INTERRUPTED} status.
     *
     * @param wikiId the identifier of the wiki
     * @param expirationDate the items deleted before this date are purged
     * @param date when the purge runs, stored in the returned entry
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge; it is only
     *     stored in the returned entry
     * @return the history entry describing the purge
     */
    PurgeHistoryEntry purge(String wikiId, Instant expirationDate, Instant date, DocumentReference triggeredBy);
}
