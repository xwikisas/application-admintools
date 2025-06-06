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
package com.xwiki.admintools.configuration;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Admin Tools configurations.
 *
 * @version $Id$
 */
@Role
public interface AdminToolsConfiguration
{
    /**
     * Get the server installation path.
     *
     * @return {@link String} representing the server installation path.
     */
    String getServerPath();

    /**
     * Get the lines that are to be excluded from xwiki.cfg and xwiki.properties files.
     *
     * @return {@link List} with the lines to be excluded.
     */
    List<String> getExcludedLines();

    /**
     * Get the number of comments that are used for a page to be considered spam.
     *
     * @return {@link Integer} representing the spam size.
     */
    int getSpamSize();

    /**
     * Get the XWiki install path.
     *
     * @return {@link String} representing the XWiki install path.
     * @since 1.2.1
     */
    String getXWikiInstallLocation();
}
