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
package com.xwiki.admintools.internal.usage.wikiResult;

import org.xwiki.stability.Unstable;

import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Store info about deleted attachments and documents from a wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WikiRecycleBins implements WikiUsageResult
{
    private String wikiId;

    private String wikiName;

    private Long documentsCount;

    private Long attachmentsCount;

    /**
     * Initialise an empty {@link WikiRecycleBins}.
     */
    public WikiRecycleBins()
    {
    }

    /**
     * Get the number of deleted attachments in this wiki.
     *
     * @return number of deleted attachments in wiki.
     */
    @Override
    public Long getAttachmentsCount()
    {
        return attachmentsCount;
    }

    /**
     * Set the number of deleted attachments in this wiki.
     *
     * @param attachmentsCount number of deleted attachments for wiki.
     */
    @Override
    public void setAttachmentsCount(Long attachmentsCount)
    {
        this.attachmentsCount = attachmentsCount;
    }

    /**
     * Get the number of deleted documents in this wiki.
     *
     * @return number of deleted documents in wiki.
     */
    @Override
    public Long getDocumentsCount()
    {
        return documentsCount;
    }

    /**
     * Set the number of deleted documents in this wiki.
     *
     * @param documentsCount number of deleted documents for wiki.
     */
    @Override
    public void setDocumentsCount(Long documentsCount)
    {
        this.documentsCount = documentsCount;
    }

    @Override
    public String getWikiName()
    {
        return wikiName;
    }

    @Override
    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    @Override
    public Long getTotal()
    {
        return documentsCount + attachmentsCount;
    }

    /**
     * Get the wiki id.
     *
     * @return the wiki id.
     */
    public String getWikiId()
    {
        return wikiId;
    }

    /**
     * Set the wiki id.
     *
     * @param wikiId the id of the wiki.
     */
    public void setWikiId(String wikiId)
    {
        this.wikiId = wikiId;
    }
}
