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
 * Result of a health check. May store the error message, severity level, recommendation and the current value of the
 * checked resource. The severity level is used as "info", for informative result, "warn" for warnings and "error" for
 * critical issues.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WikiRecycleBinResult
{
    String wikiId;

    String wikiName;

    Long pageSize;

    Long attachmentSize;

    public WikiRecycleBinResult()
    {
    }

    public Long getAttachmentSize()
    {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize)
    {
        this.attachmentSize = attachmentSize;
    }

    public Long getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Long pageSize)
    {
        this.pageSize = pageSize;
    }

    public Long getTotal()
    {
        return attachmentSize + pageSize;
    }

    public String getWikiName()
    {
        return wikiName;
    }

    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    public String getWikiId()
    {
        return wikiId;
    }

    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }
}
