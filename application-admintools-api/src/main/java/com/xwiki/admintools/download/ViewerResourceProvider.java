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
package com.xwiki.admintools.download;

import java.io.IOException;

import org.xwiki.component.annotation.Role;

/**
 * Endpoint used to create zip archive entries.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface ViewerResourceProvider
{
    /**
     * Get the requested data from the server.
     *
     * @param input {@link T} data received from user input.
     * @return {@link Byte} array representing the transfer data.
     * @throws IOException
     */
    byte[] getByteData(String input) throws IOException;

    /**
     * Extract the hint of a component.
     *
     * @return the component hint.
     */
    String getIdentifier();
}
