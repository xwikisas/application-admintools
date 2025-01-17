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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.jobs.PackageUploadJobRequest;
import com.xwiki.admintools.jobs.PackageUploadJobStatus;
import com.xwiki.admintools.uploadPackageJob.UploadPackageJobResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UploadJob}.
 */
@ComponentTest
class UploadJobTest
{
    private final String PATH = "path/to/xwiki/install";

    @InjectMockComponents
    private UploadJob uploadJob;

    @Mock
    private UploadPackageJobResource jobResource;

    @Mock
    private UploadPackageJobResource jobResource2;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @Mock
    private PackageUploadJobRequest request;

    @MockComponent
    private UploadJobFileProcessor fileProcessor;

    @Mock
    private ZipOutputStream zipOutputStream;

    @Mock
    private AttachmentReference attachmentReference;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiAttachment wikiAttachment;

    @Mock
    private XWikiDocument wikiDocument;

    @Mock
    private DocumentReference documentReference;

    @MockComponent
    private ServerInfo serverInfo;

    @XWikiTempDir
    private File tmpDir;

    private File testFile;

    private File testFile2;

    private File archDir;

    private File zipFile;

    private File backupFile;

    private File backupFile2;

    private File targetFile;

    @Mock
    private File mockTargetFile2;

    @Mock
    private File mockTargetFile;

    private InputStream inputStream;

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
        when(mockTargetFile.getParent()).thenReturn(targetFile.getParent());
        when(mockTargetFile.toPath()).thenReturn(targetFile.toPath());

        when(mockTargetFile2.toPath()).thenReturn(testFile2.toPath());

        when(currentServer.getCurrentServer()).thenReturn(serverInfo);

        when(serverInfo.getXWikiInstallFolderPath()).thenReturn(PATH);
        when(fileProcessor.getArchiveInputStream(null)).thenReturn(inputStream);
        when(fileProcessor.maybeBackupFile(eq(PATH), eq("resource_file.txt"),
            any(PackageUploadJobStatus.class))).thenReturn(jobResource);
        when(fileProcessor.maybeBackupFile(eq(PATH), eq("resource_file2.txt"),
            any(PackageUploadJobStatus.class))).thenReturn(jobResource2);

        when(jobResource.getTargetFile()).thenReturn(mockTargetFile);
        when(jobResource.getNewFileName()).thenReturn("new_file_name.txt");
        when(jobResource.getNewFileContent()).thenReturn(new ByteArrayOutputStream());
        when(jobResource.getBackupFile()).thenReturn(backupFile);

        when(jobResource2.getTargetFile()).thenReturn(mockTargetFile2);
        when(jobResource2.getNewFileName()).thenReturn("new_file_name2.txt");
        when(jobResource2.getNewFileContent()).thenReturn(new ByteArrayOutputStream());
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
        when(mockTargetFile.exists()).thenReturn(true);
        when(mockTargetFile.delete()).thenReturn(true);

        when(mockTargetFile2.exists()).thenReturn(false);

        uploadJob.initialize(request);
        uploadJob.runInternal();

        PackageUploadJobStatus uploadJobStatus = uploadJob.getStatus();
        verify(jobResource, Mockito.times(1)).setUploaded();
        verify(jobResource2, Mockito.times(1)).setUploaded();
        verify(mockTargetFile, Mockito.times(1)).delete();
        verify(mockTargetFile2, Mockito.times(0)).delete();
        assertEquals(2, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.batch.save.success", uploadJobStatus.getJobResults().get(0).getMessage());
    }

    @Test
    void runInternalFailToDeleteTarget()
    {
        when(mockTargetFile.exists()).thenReturn(true);
        when(mockTargetFile.delete()).thenReturn(false);
        when(mockTargetFile.getName()).thenReturn(targetFile.getName());

        when(mockTargetFile2.exists()).thenReturn(false);

        uploadJob.initialize(request);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            uploadJob.runInternal();
        });

        PackageUploadJobStatus uploadJobStatus = uploadJob.getStatus();
        verify(jobResource, Mockito.times(0)).setUploaded();
        verify(jobResource2, Mockito.times(1)).setUploaded();
        assertEquals("java.lang.RuntimeException: Failed to delete original file [target].", exception.getMessage());
        assertEquals(4, uploadJobStatus.getJobResults().size());
        assertEquals("adminTools.jobs.upload.batch.save.success", uploadJobStatus.getJobResults().get(0).getMessage());
        assertEquals("adminTools.jobs.upload.batch.save.fail", uploadJobStatus.getJobResults().get(1).getMessage());
        assertEquals("adminTools.jobs.upload.batch.backup.success",
            uploadJobStatus.getJobResults().get(2).getMessage());
        assertEquals("adminTools.jobs.upload.batch.backup.success",
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
