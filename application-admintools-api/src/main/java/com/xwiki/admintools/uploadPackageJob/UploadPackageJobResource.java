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
package com.xwiki.admintools.uploadPackageJob;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.xwiki.stability.Unstable;

/**
 * Store data for package upload job process.
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class UploadPackageJobResource
{
    private File targetFile;

    private File backupFile;

    private ByteArrayOutputStream newFileContent;

    private String newFileName;

    private boolean uploaded;

    /**
     * Default constructor.
     */
    public UploadPackageJobResource()
    {
        uploaded = false;
    }

    /**
     * Get the content of the new file.
     *
     * @return the content of the new file.
     */
    public ByteArrayOutputStream getNewFileContent()
    {
        return newFileContent;
    }

    /**
     * See {@link #getNewFileContent()}.
     *
     * @param newFileContent the new file content.
     */
    public void setNewFileContent(ByteArrayOutputStream newFileContent)
    {
        this.newFileContent = newFileContent;
    }

    /**
     * Get the backup file.
     *
     * @return the backup file.
     */
    public File getBackupFile()
    {
        return backupFile;
    }

    /**
     * See {@link #getBackupFile()}.
     *
     * @param backupFile the backup file.
     */
    public void setBackupFile(File backupFile)
    {
        this.backupFile = backupFile;
    }

    /**
     * Get the file that will be replaced or saved.
     *
     * @return the update target file.
     */
    public File getTargetFile()
    {
        return targetFile;
    }

    /**
     * See {@link #getTargetFile()}.
     *
     * @param targetFile the update target file.
     */
    public void setTargetFile(File targetFile)
    {
        this.targetFile = targetFile;
    }

    /**
     * Get the new file name.
     *
     * @return the name of the new file.
     */
    public String getNewFileName()
    {
        return newFileName;
    }

    /**
     * See {@link #getNewFileName()}.
     *
     * @param newFileName the name of the new file.
     */
    public void setNewFileName(String newFileName)
    {
        this.newFileName = newFileName;
    }

    /**
     * Set the upload flag to {@code true}.
     */
    public void setUploaded()
    {
        this.uploaded = true;
    }

    /**
     * Get the upload flag.
     *
     * @return the upload flag.
     */
    public boolean isUploaded()
    {
        return uploaded;
    }
}
