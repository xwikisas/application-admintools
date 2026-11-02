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
 * How the automatic purge is configured for an item type.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public enum PurgeMode
{
    /**
     * Use the global default configuration. Only meaningful for a wiki configuration.
     */
    INHERIT("inherit"),

    /**
     * The automatic purge is enabled, with its own interval.
     */
    ENABLED("enabled"),

    /**
     * The automatic purge is disabled.
     */
    DISABLED("disabled");

    private final String id;

    PurgeMode(String id)
    {
        this.id = id;
    }

    /**
     * @return the identifier of the mode, as stored in the wiki pages and used in requests
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier to look for
     * @return the mode with the given identifier, or an empty {@link Optional} if there is none
     */
    public static Optional<PurgeMode> fromId(String id)
    {
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }
}
