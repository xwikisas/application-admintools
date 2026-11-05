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
 * The outcome of a purge run.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public enum PurgeStatus
{
    /**
     * No item failed to be purged.
     */
    SUCCESS("success"),

    /**
     * Some items were purged and some failed.
     */
    PARTIAL("partial"),

    /**
     * All the items that should have been purged failed.
     */
    FAILED("failed"),

    /**
     * The purge stopped before going through all the items to purge, for example because the next items couldn't be
     * loaded. The items removed until then stay removed, and the remaining ones are purged by the next run.
     */
    INTERRUPTED("interrupted");

    private final String id;

    PurgeStatus(String id)
    {
        this.id = id;
    }

    /**
     * @return the identifier of the status, as stored in the wiki pages and used in requests
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier to look for
     * @return the status with the given identifier, or an empty {@link Optional} if there is none
     */
    public static Optional<PurgeStatus> fromId(String id)
    {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
