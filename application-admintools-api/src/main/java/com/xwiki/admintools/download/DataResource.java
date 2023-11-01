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
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.xwiki.component.annotation.Role;

/**
 * Access server files.
 *
 * @version $Id$
 */
@Role
public interface DataResource
{
    /**
     * Retrieves the content of a system file and adds it as an entry inside a {@link ZipOutputStream}.
     *
     * @param zipOutputStream {@link ZipOutputStream} represents the zip archive in which the entry is written.
     * @param filters store filters that can be used for file selection.
     */
    void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String[]> filters);

    /**
     * Retrieve the content of a system file.
     *
     * @param params Can be used to send additional info to the component.
     * @return the content of the file as an {@link Byte} array.
     * @throws IOException when there are errors while handling searched files.
     * @throws NumberFormatException when there is an invalid numeric input.
     */
    byte[] getByteData(Map<String, String[]> params) throws Exception;

    /**
     * Get the hint of a component.
     *
     * @return the component hint.
     */
    String getIdentifier();
}
