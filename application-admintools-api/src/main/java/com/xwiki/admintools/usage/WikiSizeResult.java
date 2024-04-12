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

import java.text.DecimalFormat;
import java.util.List;

import org.xwiki.stability.Unstable;

/**
 * Stores info about the size of a wiki.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class WikiSizeResult
{
    private String name;

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

    /**
     * Get the name of the wiki.
     *
     * @return the name of the wiki.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Set the name of the wiki.
     *
     * @param name representing the name of the wiki.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the number of users registered in the wiki.
     *
     * @return {@link Long} representing the number of users in the wiki.
     */
    public Long getUserCount()
    {
        return userCount;
    }

    /**
     * Set the number of users registered in the wiki.
     *
     * @param userCount the number of users in the wiki.
     */
    public void setUserCount(Long userCount)
    {
        this.userCount = userCount;
    }

    /**
     * Get the total size of the attachments in the wiki.
     *
     * @return formatted {@link String} with the size of the attachments in the wiki and corresponding size unit.
     */
    public Long getAttachmentsSize()
    {
        return attachmentsSize;
    }

    /**
     * Set the total size of the attachments in the wiki.
     *
     * @param attachmentsSize the size of the attachments in the wiki and corresponding size unit.
     */
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
