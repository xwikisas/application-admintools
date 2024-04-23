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

import java.text.DecimalFormat;
import java.util.List;

import org.xwiki.stability.Unstable;

import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Stores info about the size of a wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WikiSizeResult implements WikiUsageResult
{
    private String wikiName;

    private Long userCount;

    private Long attachmentsSize;

    private Long attachmentsCount;

    private Long documentsCount;

    /**
     * Null constructor to initialize a {@link WikiSizeResult} object.
     */
    public WikiSizeResult()
    {
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
    public Long getUserCount()
    {
        return userCount;
    }

    @Override
    public void setUserCount(Long userCount)
    {
        this.userCount = userCount;
    }

    @Override
    public Long getAttachmentsSize()
    {
        return attachmentsSize;
    }

    @Override
    public void setAttachmentsSize(Long attachmentsSize)
    {
        this.attachmentsSize = attachmentsSize;
    }

    /**
     * Get the total number of the attachments in wiki.
     *
     * @return the total number of the attachments in wiki.
     */
    public Long getAttachmentsCount()
    {
        return attachmentsCount;
    }

    /**
     * Set the total number of the attachments in wiki.
     *
     * @param attachmentsCount the total number of the attachments in wiki.
     */
    public void setAttachmentsCount(Long attachmentsCount)
    {
        this.attachmentsCount = attachmentsCount;
    }

    /**
     * Get the total number of documents in wiki.
     *
     * @return the total number of documents in wiki.
     */
    public Long getDocumentsCount()
    {
        return documentsCount;
    }

    /**
     * Set the total number of documents in wiki.
     *
     * @param documentsCount the total number of documents in wiki.
     */
    public void setDocumentsCount(Long documentsCount)
    {
        this.documentsCount = documentsCount;
    }

    /**
     * Get the size of the attachments in a readable format.
     *
     * @return a {@link String} with the size of the attachments converted to the corresponding unit of measurement.
     */
    public String getReadableAttachmentSize()
    {
        if (this.attachmentsSize == null || this.attachmentsSize <= 0) {
            return "0";
        }
        List<String> units = List.of("B", "KB", "MB", "GB");
        int digitGroup = (int) (Math.log10(this.attachmentsSize) / Math.log10(1024));
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        String resultedSize = decimalFormat.format(this.attachmentsSize / Math.pow(1024, digitGroup));

        return String.format("%s %s", resultedSize, units.get(digitGroup));
    }
}
