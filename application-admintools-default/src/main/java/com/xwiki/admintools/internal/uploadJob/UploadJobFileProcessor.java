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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;
import com.xwiki.admintools.jobs.PackageUploadJobStatus;
import com.xwiki.admintools.uploadPackageJob.UploadPackageJobResource;

/**
 * Processes the {@link XWikiAttachment} and the given archive file content, while also creating a backup if needed.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = UploadJobFileProcessor.class)
@Singleton
@Unstable
public class UploadJobFileProcessor
{
    /**
     * Path format for new files from a given parent.
     */
    public static final String NEW_FILE_PATH_FORMAT = "%s/%s";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    /**
     * Checks if a file with the same name or if a JAR file with a similar name exists at the processed target location
     * and creates a backup if it does.
     *
     * @param targetFolderPath the path of the folder for which to check for existing files.
     * @param filePath the path for the file within the folder.
     * @param status the job status where to log the backup result.
     * @return the {@link UploadPackageJobResource} containing the processed target file, file name and backup file.
     */
    public UploadPackageJobResource maybeBackupFile(String targetFolderPath, String filePath,
        PackageUploadJobStatus status)
    {
        File targetFile = new File(targetFolderPath + filePath);
        UploadPackageJobResource uploadPackageJobResource = new UploadPackageJobResource();
        String fileName = targetFile.getName();
        uploadPackageJobResource.setNewFileName(fileName);
        if (targetFile.exists()) {
            createBackupFile(targetFile.getAbsolutePath() + ".bak", targetFile, uploadPackageJobResource, fileName,
                status);
        } else {
            String extension = filePath.substring(filePath.lastIndexOf("."));
            if (extension.equals(".jar")) {
                String simpleFileName = extractNameBeforeVersion(fileName);
                String directoryPath = targetFile.getParent();
                String[] filesList = new File(directoryPath).list(
                    ((dir, name) -> name.startsWith(simpleFileName) && name.endsWith(extension)));
                if (filesList == null || filesList.length > 1) {
                    JobResult log =
                        new JobResult("adminTools.jobs.upload.assessoriginal.fail", JobResultLevel.ERROR, fileName);
                    status.addLog(log);
                    throw new RuntimeException(
                        String.format("Unable to correctly assess the original file for %s.", fileName));
                } else if (filesList.length == 1) {
                    String oldFilePath = String.format(NEW_FILE_PATH_FORMAT, directoryPath, filesList[0]);
                    targetFile = new File(oldFilePath);
                    createBackupFile(String.format("%s.bak", oldFilePath), targetFile, uploadPackageJobResource,
                        fileName, status);
                }
            }
        }
        uploadPackageJobResource.setTargetFile(targetFile);
        return uploadPackageJobResource;
    }

    /**
     * Get the content of an {@link XWikiAttachment} as an {@link InputStream}.
     *
     * @param fileRef the reference of the attachment.
     * @return the {@link InputStream} of the attachment.
     * @throws XWikiException if any errors occur during the retrieval of the attachment.
     */
    public InputStream getArchiveInputStream(String fileRef) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        AttachmentReference attachmentReference = attachmentReferenceResolver.resolve(fileRef);
        XWikiDocument parentDoc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        return parentDoc.getAttachment(attachmentReference.getName()).getContentInputStream(xcontext);
    }

    /**
     * Process and store the content of a {@link ZipInputStream}.
     *
     * @param zis the zip content to be processed.
     * @param jobResource the resource used for storing the result.
     * @throws IOException if any errors occur during the reading of the content.
     */
    public void processFileContent(ZipInputStream zis, UploadPackageJobResource jobResource) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        jobResource.setNewFileContent(baos);
    }

    private void createBackupFile(String backupFilePath, File targetFile,
        UploadPackageJobResource uploadPackageJobResource, String fileName, PackageUploadJobStatus status)
    {
        File backupFile = new File(backupFilePath);
        try {
            Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JobResult log =
                new JobResult("adminTools.jobs.upload.backup.fail", JobResultLevel.ERROR, backupFile.getName(),
                    fileName);
            status.addLog(log);
            throw new RuntimeException(e);
        }
        uploadPackageJobResource.setBackupFile(backupFile);
        JobResult log =
            new JobResult("adminTools.jobs.upload.backup.success", JobResultLevel.INFO, backupFile.getName(), fileName);
        status.addLog(log);
    }

    private String extractNameBeforeVersion(String input)
    {
        String regex = "^([\\w-]+(?:-[\\w-]+)*)(?=(-\\d+|\\.[^.]+$))";
        Matcher matcher = Pattern.compile(regex).matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input;
    }
}
