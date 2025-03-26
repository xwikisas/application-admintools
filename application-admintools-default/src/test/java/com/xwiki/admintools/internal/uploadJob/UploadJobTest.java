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
        tmpDir.mkdir();
        tmpDir.deleteOnExit();
        archDir = new File(tmpDir, "resource_folder");
        archDir.mkdir();

        testFile = new File(archDir, "resource_file.txt");
        testFile2 = new File(archDir, "resource_file2.txt");
        zipFile = new File(tmpDir, "archive");
        backupFile = new File(tmpDir, "backup");
        backupFile2 = new File(tmpDir, "backup2");
        targetFile = new File(tmpDir, "target");

        testFile.createNewFile();
        testFile2.createNewFile();
        zipFile.createNewFile();
        backupFile.createNewFile();
        backupFile2.createNewFile();
        targetFile.createNewFile();
        zipFile(archDir, zipFile);
        inputStream = new DataInputStream(new FileInputStream(zipFile));
    }

    @BeforeEach
    void beforeEach() throws XWikiException
    {
        when(fileProcessor.getArchiveInputStream(null)).thenReturn(inputStream);
        when(fileProcessor.maybeBackupFile(eq("resource_file.txt"), any(PackageUploadJobStatus.class))).thenReturn(
            jobResource);

        when(fileProcessor.maybeBackupFile(eq("resource_file2.txt"), any(PackageUploadJobStatus.class))).thenReturn(
            jobResource2);

        when(jobResource.getNewFilename()).thenReturn("new_file_name.txt");
        when(jobResource.getBackupFile()).thenReturn(backupFile);

        when(jobResource2.getNewFilename()).thenReturn("new_file_name2.txt");
        when(jobResource2.getBackupFile()).thenReturn(backupFile2);
    }

    @Test
    void createNewStatus()
    {
        assertEquals(PackageUploadJobStatus.class, uploadJob.createNewStatus(new PackageUploadJobRequest()).getClass());
    }

    @Test
    void runInternal()
    {
        uploadJob.initialize(request);
        PackageUploadJobStatus uploadJobStatus = uploadJob.getStatus();

        uploadJob.runInternal();

        assertEquals(1, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.success", uploadJobStatus.getJobResults().get(0).getMessage());
    }

    @Test
    void runInternalFailToDeleteTarget()
    {
        when(fileProcessor.maybeBackupFile(eq(testFile2.getName()), any(PackageUploadJobStatus.class))).thenThrow(
            new RuntimeException("error"));

        uploadJob.initialize(request);
        uploadJob.runInternal();
        PackageUploadJobStatus uploadJobStatus = uploadJob.getStatus();

        assertEquals("Error during the file upload job.", logCapture.getMessage(0));
        assertEquals(2, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.fail", uploadJobStatus.getJobResults().get(0).getMessage());
        assertEquals("adminTools.jobs.upload.batch.restore.success",
            uploadJobStatus.getJobResults().get(1).getMessage());
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
