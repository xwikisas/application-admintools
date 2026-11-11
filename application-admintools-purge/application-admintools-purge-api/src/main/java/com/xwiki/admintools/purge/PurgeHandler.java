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

import java.time.ZonedDateTime;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Purges the items of one type from a wiki. There is one implementation per {@link PurgeType}, whose hint is the
 * identifier of the type. The limit of a purge is given as item type settings, from the stored settings or chosen for
 * a manual purge, and is applied relative to a given time; its mode is ignored. No rights are checked: the callers
 * must do it.
 *
 * @version $Id$
 * @since 1.5
 */
@Role
@Unstable
public interface PurgeHandler
{
    /**
     * Count the items of a wiki that are past a limit, which a purge with the same limit and time would remove.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at, whose time zone is used for the calendar units
     * @return the number of items past the limit
     * @throws QueryException if the items can't be counted
     */
    long countExpired(String wikiId, PurgeTypeSettings limit, ZonedDateTime now) throws QueryException;

    /**
     * Permanently delete the items of a wiki that are past a limit. An item that can't be deleted is skipped and
     * reported in the returned entry, and the purge goes on with the next items. If the next items can't be loaded,
     * the purge stops, and the returned entry has the {@link PurgeStatus#INTERRUPTED} status.
     *
     * @param wikiId the identifier of the wiki
     * @param limit the limit of the purge
     * @param now the time the limit is applied at, whose time zone is used for the calendar units; it is also the
     *     date stored in the returned entry
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge; it is only
     *     stored in the returned entry
     * @return the history entry describing the purge
     */
    PurgeHistoryEntry purge(String wikiId, PurgeTypeSettings limit, ZonedDateTime now, DocumentReference triggeredBy);
}
