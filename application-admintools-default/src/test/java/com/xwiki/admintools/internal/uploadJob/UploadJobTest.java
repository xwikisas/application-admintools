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
package com.xwiki.admintools.internal.uploadJob;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.jobs.PackageUploadJobRequest;
import com.xwiki.admintools.jobs.PackageUploadJobStatus;
import com.xwiki.admintools.uploadPackageJob.UploadPackageJobResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UploadJob}.
 */
@ComponentTest
class UploadJobTest
{
    @InjectMockComponents
    private UploadJob uploadJob;

    @Mock
    private UploadPackageJobResource jobResource;

    @Mock
    private UploadPackageJobResource jobResource2;

    @Mock
    private PackageUploadJobRequest request;

    @MockComponent
    private UploadJobFileProcessor fileProcessor;

    @XWikiTempDir
    private File tmpDir;

    private File testFile;

    private File testFile2;

    private File archDir;

    private File zipFile;

    private File backupFile;

    private File backupFile2;

    private File targetFile;

    private InputStream inputStream;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeComponent
    void setUp() throws IOException
    {
        this.tmpDir.mkdir();
        this.tmpDir.deleteOnExit();
        this.archDir = new File(this.tmpDir, "resource_folder");
        this.archDir.mkdir();

        this.testFile = new File(this.archDir, "resource_file.txt");
        this.testFile2 = new File(this.archDir, "resource_file2.txt");
        this.zipFile = new File(this.tmpDir, "archive");
        this.backupFile = new File(this.tmpDir, "backup");
        this.backupFile2 = new File(this.tmpDir, "backup2");
        this.targetFile = new File(this.tmpDir, "target");

        this.testFile.createNewFile();
        this.testFile2.createNewFile();
        this.zipFile.createNewFile();
        this.backupFile.createNewFile();
        this.backupFile2.createNewFile();
        this.targetFile.createNewFile();
        zipFile(this.archDir, this.zipFile);
        this.inputStream = new DataInputStream(new FileInputStream(this.zipFile));
    }

    @BeforeEach
    void beforeEach() throws XWikiException
    {
        when(this.fileProcessor.getArchiveInputStream(null)).thenReturn(this.inputStream);
        when(this.fileProcessor.maybeBackupFile(eq("resource_file.txt"), any(PackageUploadJobStatus.class))).thenReturn(
            this.jobResource);

        when(this.fileProcessor.maybeBackupFile(eq("resource_file2.txt"), any(PackageUploadJobStatus.class))).thenReturn(
            this.jobResource2);

        when(this.jobResource.getNewFilename()).thenReturn("new_file_name.txt");
        when(this.jobResource.getBackupFile()).thenReturn(this.backupFile);

        when(this.jobResource2.getNewFilename()).thenReturn("new_file_name2.txt");
        when(this.jobResource2.getBackupFile()).thenReturn(this.backupFile2);
    }

    @Test
    void createNewStatus()
    {
        assertEquals(PackageUploadJobStatus.class, this.uploadJob.createNewStatus(new PackageUploadJobRequest()).getClass());
    }

    @Test
    void runInternal()
    {
        this.uploadJob.initialize(this.request);
        PackageUploadJobStatus uploadJobStatus = this.uploadJob.getStatus();

        this.uploadJob.runInternal();

        assertEquals(1, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.success", uploadJobStatus.getJobResults().get(0).getMessage());
    }

    @Test
    void runInternalFailToDeleteTarget()
    {
        when(this.fileProcessor.maybeBackupFile(eq(this.testFile2.getName()), any(PackageUploadJobStatus.class))).thenThrow(
            new RuntimeException("error"));
        when(this.jobResource.getTargetFile()).thenReturn(this.targetFile);
        this.uploadJob.initialize(this.request);
        this.uploadJob.runInternal();
        PackageUploadJobStatus uploadJobStatus = this.uploadJob.getStatus();

        assertEquals("Error during the file upload job.", this.logCapture.getMessage(0));
        assertEquals(4, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.fail", uploadJobStatus.getJobResults().get(0).getMessage());
        assertEquals("adminTools.jobs.upload.batch.restore.start",
            uploadJobStatus.getJobResults().get(1).getMessage());
        assertEquals("adminTools.jobs.upload.batch.restore.file.success",
            uploadJobStatus.getJobResults().get(2).getMessage());
        assertEquals("adminTools.jobs.upload.batch.restore.success",
            uploadJobStatus.getJobResults().get(3).getMessage());
    }

    private static void zipFile(File sourceFolder, File outputZip) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(outputZip); ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFiles(sourceFolder, sourceFolder, zos);
        }
    }

    private static void zipFiles(File rootFolder, File sourceFile, ZipOutputStream zos) throws IOException
    {
        if (sourceFile.isDirectory()) {
            // If directory, recursively call for each file in the directory
            for (File file : sourceFile.listFiles()) {
                zipFiles(rootFolder, file, zos);
            }
        } else {
            // If file, add it to the ZIP
            try (FileInputStream fis = new FileInputStream(sourceFile)) {
                // Create a relative path for the file within the ZIP
                String zipEntryName = sourceFile.getAbsolutePath().substring(rootFolder.getAbsolutePath().length() + 1);
                ZipEntry zipEntry =
                    new ZipEntry(zipEntryName.replace("\\", "/")); // Replace \ with / for cross-platform compatibility
                zos.putNextEntry(zipEntry);

                // Write file data to the ZIP entry
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }
}
