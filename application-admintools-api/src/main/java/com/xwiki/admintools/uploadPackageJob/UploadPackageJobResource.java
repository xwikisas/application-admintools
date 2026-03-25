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

    private String newFilename;

    /**
     * Default constructor.
     */
    public UploadPackageJobResource()
    {
    }

    /**
     * The original file backup created before the uploading process began. Useful when restoring a failed upload
     * process. Will be null if the target file does not replace any files.
     *
     * @return the original file backup.
     */
    public File getBackupFile()
    {
        return backupFile;
    }

    /**
     * See {@link #getBackupFile()}.
     *
     * @param backupFile the original file backup.
     */
    public void setBackupFile(File backupFile)
    {
        this.backupFile = backupFile;
    }

    /**
     * Retrieves the file that serves as the target for the update process. If a file already exists at the intended
     * location, this will return the original file. If no file exists at the location, this will return the new file
     * being uploaded.
     *
     * @return the target file for the update process.
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
    public String getNewFilename()
    {
        return newFilename;
    }

    /**
     * See {@link #getNewFilename()}.
     *
     * @param newFilename the name of the new file.
     */
    public void setNewFilename(String newFilename)
    {
        this.newFilename = newFilename;
    }
}
