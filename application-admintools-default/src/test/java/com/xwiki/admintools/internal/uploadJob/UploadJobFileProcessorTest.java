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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.environment.Environment;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link UploadJobFileProcessor}.
 */
@ComponentTest
class UploadJobFileProcessorTest
{
    private final String SERVER_PATH = "path/to/xwiki/install";

    @InjectMockComponents
    private UploadJobFileProcessor fileProcessor;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private XWikiContext wikiContext;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @MockComponent
    private AttachmentReference attachmentReference;

    @Mock
    private XWikiDocument wikiDocument;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiAttachment wikiAttachment;

    @Mock
    private DocumentReference documentReference;

    @XWikiTempDir
    private File tmpDir;

    private File permDir;

    private File testFile;

    private File jarTestFile;

    private File jarTestFile2;

    private File archDir;

    private File jarArchDir;

    private File jarArchDirDuplicates;

    private File targetDir;

    private File targetFile;

    private File jarTargetFile;

    private File jarTargetFile1;

    private File jarTargetFile2;

    private File zipFile;

    private File jarZipFile;

    private File jarZipFile2;

    private ZipInputStream inputStream;

    private ZipInputStream jarInputStream;

    private ZipInputStream jarInputStream2;

    private ZipEntry zipEntry;

    private ZipEntry jarZipEntry;

    private ZipEntry jarZipEntry2;

    private PackageUploadJobStatus status;

    @Mock
    private UploadPackageJobResource resource;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private ServerInfo serverInfo;

    @MockComponent
    private Environment environment;

    @BeforeComponent
    void setUp() throws IOException
    {
        tmpDir.mkdir();
        tmpDir.deleteOnExit();

        targetDir = new File(tmpDir, "target_folder");
        archDir = new File(tmpDir, "resource_folder");
        jarArchDir = new File(tmpDir, "jar_target_folder");
        jarArchDirDuplicates = new File(tmpDir, "jar_target_folder2");
        permDir = new File(tmpDir, "permanent_directory");

        targetDir.mkdir();
        archDir.mkdir();
        jarArchDir.mkdir();
        jarArchDirDuplicates.mkdir();
        permDir.mkdir();

        testFile = new File(archDir, "resource_file.txt");
        jarTestFile = new File(jarArchDir, "ant-launcher-1.10.11-client-development-special.jar");
        jarTestFile2 = new File(jarArchDirDuplicates, "bcutil-jdk18on-1.72-client-development-special.jar");
        zipFile = new File(tmpDir, "archive");
        jarZipFile = new File(tmpDir, "jar_archive");
        jarZipFile2 = new File(tmpDir, "jar_archive2");
        targetFile = new File(targetDir, "resource_file.txt");
        jarTargetFile = new File(targetDir, "ant-launcher-1.10.11.jar");
        jarTargetFile1 = new File(targetDir, "bcutil-jdk18on-1.72.jar");
        jarTargetFile2 = new File(targetDir, "bcutil-jdk18on-1.72-something.jar");

        testFile.createNewFile();
        jarTestFile.createNewFile();
        jarTestFile2.createNewFile();
        zipFile.createNewFile();
        jarZipFile.createNewFile();
        targetFile.createNewFile();
        jarTargetFile.createNewFile();
        jarTargetFile1.createNewFile();
        jarTargetFile2.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(testFile.getAbsolutePath()));
        writer.append("test content");
        writer.close();

        zipFile(archDir, zipFile);
        inputStream = new ZipInputStream(new FileInputStream(zipFile.getAbsolutePath()));
        zipEntry = inputStream.getNextEntry();

        zipFile(jarArchDir, jarZipFile);
        jarInputStream = new ZipInputStream(new FileInputStream(jarZipFile.getAbsolutePath()));
        jarZipEntry = jarInputStream.getNextEntry();

        zipFile(jarArchDirDuplicates, jarZipFile2);
        jarInputStream2 = new ZipInputStream(new FileInputStream(jarZipFile2.getAbsolutePath()));
        jarZipEntry2 = jarInputStream2.getNextEntry();

        when(currentServer.getCurrentServer()).thenReturn(serverInfo);
        when(serverInfo.getXWikiInstallFolderPath()).thenReturn(targetDir.getPath());
        when(environment.getPermanentDirectory()).thenReturn(new File(tmpDir, "permanent_directory"));
    }

    @BeforeEach
    void beforeEach()
    {
        status = new PackageUploadJobStatus("admintools.uploadpackage", new PackageUploadJobRequest(), null, null);
    }

    @Test
    @Order(1)
    void initializeBackupFolder() throws IOException
    {
        fileProcessor.initializeBackupFolder(status.getJobID());
        assertTrue(environment.getPermanentDirectory().toPath().resolve("adminTools").resolve("backup")
            .resolve(String.join("_", status.getJobID())).toFile().exists());
    }

    @Test
    @Order(1)
    void getArchiveInputStream() throws XWikiException, IOException
    {
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        String fileRef = "fileRef";
        when(attachmentReferenceResolver.resolve(fileRef)).thenReturn(attachmentReference);
        when(attachmentReference.getDocumentReference()).thenReturn(documentReference);
        when(wiki.getDocument(documentReference, wikiContext)).thenReturn(wikiDocument);
        when(wikiDocument.getAttachment(null)).thenReturn(wikiAttachment);
        when(wikiAttachment.getContentInputStream(wikiContext)).thenReturn(
            new ByteArrayInputStream(fileRef.getBytes()));
        assertEquals(fileRef, new String(fileProcessor.getArchiveInputStream(fileRef).readAllBytes()));
    }

    @Test
    @Order(2)
    void processFileContent() throws IOException
    {
        when(resource.getTargetFile()).thenReturn(jarTargetFile);
        when(resource.getNewFilename()).thenReturn(jarTestFile.getName());
        fileProcessor.processFileContent(inputStream, resource, status);
        assertFalse(jarTargetFile.exists());
        File testResult = Paths.get(jarTargetFile.getParent()).resolve(jarTestFile.getName()).toFile();
        assertTrue(testResult.exists());
        testResult.deleteOnExit();
    }

    @Test
    @Order(3)
    void maybeBackupFile() throws IOException
    {
        fileProcessor.initializeBackupFolder(status.getJobID());
        fileProcessor.maybeBackupFile(zipEntry.getName(), status);
        assertEquals(1, status.getJobResults().size());

        File backupFile = environment.getPermanentDirectory().toPath().resolve("adminTools").resolve("backup")
            .resolve(String.join("_", status.getJobID()))
            .resolve(targetFile.getName() + ".bak").toFile();
        assertTrue(backupFile.exists());
        inputStream.closeEntry();
    }

    @Test
    @Order(4)
    void maybeBackupFileJar() throws IOException
    {
        fileProcessor.initializeBackupFolder(status.getJobID());
        fileProcessor.maybeBackupFile(jarZipEntry.getName(), status);
        File backupFile = environment.getPermanentDirectory().toPath().resolve("adminTools").resolve("backup")
            .resolve(String.join("_", status.getJobID()))
            .resolve(jarTargetFile.getName() + ".bak").toFile();
        assertEquals(1, status.getJobResults().size());
        assertEquals("adminTools.jobs.upload.backup.success", status.getJobResults().get(0).getMessage());
        assertTrue(backupFile.exists());
    }

    @Test
    @Order(5)
    void maybeBackupFileJarNotFound()
    {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileProcessor.maybeBackupFile(jarZipEntry2.getName(), status);
        });
        assertEquals(
            "Unable to correctly assess the original file for bcutil-jdk18on-1.72-client-development-special.jar.",
            exception.getMessage());
        assertEquals(1, status.getJobResults().size());
        assertEquals("adminTools.jobs.upload.assessoriginal.fail", status.getJobResults().get(0).getMessage());
        assertFalse(new File(jarTargetFile2.getAbsolutePath() + ".bak").exists());
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
            // If it is a file, add it to the ZIP
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
