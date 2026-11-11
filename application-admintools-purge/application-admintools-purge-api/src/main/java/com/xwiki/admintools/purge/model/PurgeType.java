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
package com.xwiki.admintools.purge.model;

import java.util.Arrays;
import java.util.Optional;

import org.xwiki.stability.Unstable;

/**
 * The types of items that can be automatically purged.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public enum PurgeType
{
    /**
     * Pages (documents) that were deleted to the recycle bin.
     */
    DELETED_DOCUMENTS("deletedDocuments", false),

    /**
     * Attachments that were deleted to the recycle bin.
     */
    DELETED_ATTACHMENTS("deletedAttachments", false),

    /**
     * The versions of the pages (documents), except their current version. Its settings are
     * {@link HistoryPurgeTypeSettings}, which can also limit the number of versions kept.
     */
    PAGE_HISTORY("pageHistory", true);

    private final String id;

    private final boolean history;

    PurgeType(String id, boolean history)
    {
        this.id = id;
        this.history = history;
    }

    /**
     * @return the identifier of the type, as stored in the wiki pages and used in requests
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return {@code true} if the type purges the versions of a history, whose settings are
     *     {@link HistoryPurgeTypeSettings}, {@code false} if it purges the items of a recycle bin
     */
    public boolean isHistory()
    {
        return this.history;
    }

    /**
     * @param id the identifier to look for
     * @return the type with the given identifier, or an empty {@link Optional} if there is none
     */
    public static Optional<PurgeType> fromId(String id)
    {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
