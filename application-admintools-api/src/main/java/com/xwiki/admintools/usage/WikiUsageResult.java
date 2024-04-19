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
package com.xwiki.admintools.usage;

import org.xwiki.stability.Unstable;

/**
 * General interface for gathering info about a wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public interface WikiUsageResult
{
    /**
     * Get the name of the wiki.
     *
     * @return the name of the wiki.
     */
    String getWikiName();

    /**
     * Set the name of the wiki.
     *
     * @param wikiName representing the name of the wiki.
     */
    void setWikiName(String wikiName);

    /**
     * Get the number of documents in this wiki.
     *
     * @return number of documents in wiki.
     */
    Long getDocumentsCount();

    /**
     * Set the number of documents in this wiki.
     *
     * @param documentsCount number of documents for wiki.
     */
    void setDocumentsCount(Long documentsCount);

    /**
     * Get the number of attachments in this wiki.
     *
     * @return number of attachments in wiki.
     */
    Long getAttachmentsCount();

    /**
     * Set the number attachments in this wiki.
     *
     * @param attachmentsCount number of attachments for wiki.
     */
    void setAttachmentsCount(Long attachmentsCount);

    /**
     * Get the total number of documents and attachments.
     *
     * @return the total number of documents and attachments.
     */
    default Long getTotal()
    {
        return 0L;
    }

    /**
     * Get the number of users registered in the wiki.
     *
     * @return {@link Long} representing the number of users in the wiki.
     */
    default Long getUserCount()
    {
        return 0L;
    }

    /**
     * Set the number of users registered in the wiki.
     *
     * @param userCount the number of users in the wiki.
     */
    default void setUserCount(Long userCount)
    {
        // Default implementation.
    }

    /**
     * Get the total size of the attachments.
     *
     * @return {@link Long} with the size of the attachments in the wiki.
     */
    default Long getAttachmentsSize()
    {
        return 0L;
    }

    /**
     * Set the total size of the attachments.
     *
     * @param attachmentsSize the size of the attachments in the wiki.
     */
    default void setAttachmentsSize(Long attachmentsSize)
    {
        // Default implementation.
    }
}
