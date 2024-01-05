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
 * Stores info about the size of a Wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WikiSizeResult
{
    private String wikiName;

    private Long numberOfUsers;

    private String attachmentSize;

    private Long numberOfAttachments;

    private Long numberOfDocuments;

    /**
     * Null constructor to initialize a {@link WikiSizeResult} object.
     */
    public WikiSizeResult()
    {
    }

    /**
     * Get the name of the Wiki.
     *
     * @return the name of the Wiki.
     */
    public String getWikiName()
    {
        return wikiName;
    }

    /**
     * Set the name of the Wiki.
     *
     * @param wikiName representing the name of the Wiki.
     */
    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    /**
     * Get the number of users registered in the Wiki.
     *
     * @return {@link Long} representing the number of users in the Wiki.
     */
    public Long getNumberOfUsers()
    {
        return numberOfUsers;
    }

    /**
     * Set the number of users registered in the Wiki.
     *
     * @param numberOfUsers the number of users in the Wiki.
     */
    public void setNumberOfUsers(Long numberOfUsers)
    {
        this.numberOfUsers = numberOfUsers;
    }

    /**
     * Get the total size of the attachments in the Wiki.
     *
     * @return formatted {@link String} with the size of the attachments in the Wiki and corresponding size unit.
     */
    public String getAttachmentSize()
    {
        return attachmentSize;
    }

    /**
     * Set the total size of the attachments in the Wiki.
     *
     * @param attachmentSize the size of the attachments in the Wiki and corresponding size unit.
     */
    public void setAttachmentSize(String attachmentSize)
    {
        this.attachmentSize = attachmentSize;
    }

    /**
     * Get the total number of the attachments in Wiki.
     *
     * @return the total number of the attachments in Wiki.
     */
    public Long getNumberOfAttachments()
    {
        return numberOfAttachments;
    }

    /**
     * Set the total number of the attachments in Wiki.
     *
     * @param numberOfAttachments the total number of the attachments in Wiki.
     */
    public void setNumberOfAttachments(Long numberOfAttachments)
    {
        this.numberOfAttachments = numberOfAttachments;
    }

    /**
     * Get the total number of documents in Wiki.
     *
     * @return the total number of documents in Wiki.
     */
    public Long getNumberOfDocuments()
    {
        return numberOfDocuments;
    }

    /**
     * Set the total number of documents in Wiki.
     *
     * @param numberOfDocuments the total number of documents in Wiki.
     */
    public void setNumberOfDocuments(Long numberOfDocuments)
    {
        this.numberOfDocuments = numberOfDocuments;
    }
}
