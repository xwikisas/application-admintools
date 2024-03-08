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
package com.xwiki.admintools.health;

import org.xwiki.stability.Unstable;

/**
 * Main XWiki versions.
 *
 * @version $Id$
 * @since 1.0-rc-1
 */
@Unstable
public enum XWikiVersions
{
    /**
     * XWiki version 8.1.0.
     */
    XWIKI_8_1("8.1.0"),
    /**
     * XWiki version 11.3.0.
     */
    XWIKI_11_3("11.3.0"),
    /**
     * XWiki version 14.0.0.
     */
    XWIKI_14_0("14.0.0"),
    /**
     * XWiki version 14.10.9.
     */
    XWIKI_14_10_9("14.10.9"),
    /**
     * XWiki version 15.3.0.
     */
    XWIKI_15_3("15.3.0"),
    /**
     * XWiki version 16.0.0.
     */
    XWIKI_16_0("16.0.0"),
    /**
     * XWiki version 17.0.0.
     */
    XWIKI_17_0("17.0.0");

    private final String version;

    XWikiVersions(String version)
    {
        this.version = version;
    }

    /**
     * Get the XWiki version represented by the enum value.
     *
     * @return the version represented by the enum value.
     */
    public String getVersion()
    {
        return version;
    }
}
