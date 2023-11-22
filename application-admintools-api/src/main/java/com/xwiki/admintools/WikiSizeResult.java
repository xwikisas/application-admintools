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
package com.xwiki.admintools;

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
public class WikiSizeResult
{
    private String wikiName;

    private Long numberOfUsers;

    private Long attachmentSize;

    private Long numberOfAttachments;

    private Long numberOfDocuments;

    public WikiSizeResult()
    {
    }

    public String getWikiName()
    {
        return wikiName;
    }

    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    public Long getNumberOfUsers()
    {
        return numberOfUsers;
    }

    public void setNumberOfUsers(Long numberOfUsers)
    {
        this.numberOfUsers = numberOfUsers;
    }

    public float getAttachmentSize()
    {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize)
    {
        this.attachmentSize = attachmentSize;
    }

    public Long getNumberOfAttachments()
    {
        return numberOfAttachments;
    }

    public void setNumberOfAttachments(Long numberOfAttachments)
    {
        this.numberOfAttachments = numberOfAttachments;
    }

    public Long getNumberOfDocuments()
    {
        return numberOfDocuments;
    }

    public void setNumberOfDocuments(Long numberOfDocuments)
    {
        this.numberOfDocuments = numberOfDocuments;
    }
}
