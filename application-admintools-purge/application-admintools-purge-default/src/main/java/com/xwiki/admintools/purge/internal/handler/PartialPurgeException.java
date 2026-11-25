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

/**
 * Thrown when only some parts of an item could be purged, e.g. some attachments of a page: the item is listed as
 * failed, with the message of this exception, and the units removed from its other parts are still counted.
 *
 * @version $Id$
 * @since 1.5
 */
final class PartialPurgeException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final long removedCount;

    /**
     * Create the exception of a partial purge.
     *
     * @param message the description of each part that couldn't be purged
     * @param removedCount the number of units removed from the other parts
     */
    PartialPurgeException(String message, long removedCount)
    {
        super(message);
        this.removedCount = removedCount;
    }

    /**
     * @return the number of units removed from the parts that could be purged
     */
    long getRemovedCount()
    {
        return this.removedCount;
    }
}
