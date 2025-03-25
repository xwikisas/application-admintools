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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
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
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @Inject
    private Environment environment;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Logger logger;

    /**
     * Checks if a file with the same name or if a JAR file with a similar name exists at the processed target location
     * and creates a backup if it does.
     *
     * @param filePath the path for the file within the folder.
     * @param status the job status where to log the backup result.
     * @return the {@link UploadPackageJobResource} containing the processed target file, file name and backup file.
     */
    public UploadPackageJobResource maybeBackupFile(String filePath, PackageUploadJobStatus status)
    {
        Path targetFolderPath = Paths.get(currentServer.getCurrentServer().getXWikiInstallFolderPath());
        File targetFile = targetFolderPath.resolve(filePath).toFile();
        UploadPackageJobResource uploadPackageJobResource = new UploadPackageJobResource();
        String fileName = targetFile.getName();
        uploadPackageJobResource.setNewFilename(fileName);
        if (targetFile.exists()) {
            createBackupFile(targetFile, uploadPackageJobResource, status);
        } else {
            int extensionIndex = filePath.lastIndexOf(".");
            String extension = extensionIndex != -1 ? filePath.substring(extensionIndex) : "";

            if (extension.equals(".jar")) {
                String simpleFileName = extractNameBeforeVersion(fileName);
                File parentFile = new File(targetFile.getParent());
                String[] filesList =
                    parentFile.list(((dir, name) -> name.startsWith(simpleFileName) && name.endsWith(extension)));
                if (filesList == null || filesList.length > 1) {
                    JobResult log =
                        new JobResult("adminTools.jobs.upload.assessoriginal.fail", JobResultLevel.ERROR, fileName);
                    status.addLog(log);
                    throw new RuntimeException(
                        String.format("Unable to correctly assess the original file for %s.", fileName));
                } else if (filesList.length == 1) {
                    targetFile = parentFile.toPath().resolve(filesList[0]).toFile();
                    createBackupFile(targetFile, uploadPackageJobResource, status);
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
     * Process the content of a {@link ZipInputStream} and replace the old file.
     *
     * @param zis the zip content to be processed.
     * @param jobResource the resource used for storing the result.
     * @param status the job status where to log the process result.
     * @throws IOException if any errors occur during the reading of the content.
     */
    public void processFileContent(ZipInputStream zis, UploadPackageJobResource jobResource,
        PackageUploadJobStatus status) throws IOException
    {
        File targetFile = jobResource.getTargetFile();
        try (OutputStream fos = new FileOutputStream(targetFile, false)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            if (!jobResource.getNewFilename().equals(targetFile.getName())) {
                String filePath = targetFile.getParent();
                File newFile = Paths.get(filePath).resolve(jobResource.getNewFilename()).toFile();
                if (!targetFile.renameTo(newFile)) {
                    status.addLog(
                        new JobResult("adminTools.jobs.upload.save.fail", JobResultLevel.ERROR, targetFile.getName()));
                    throw new RuntimeException(
                        String.format("Failed to rename original file [%s].", targetFile.getName()));
                }
            }
            logger.info("Successfully uploaded file [{}].", jobResource.getNewFilename());
            JobResult log =
                new JobResult("adminTools.jobs.upload.save.success", JobResultLevel.INFO, jobResource.getNewFilename());
            status.addLog(log);
        }
    }

    /**
     * Create a unique folder for backups based on the given ID, inside the permanent directory of XWiki.
     *
     * @param folderID the id of the backup folder.
     * @throws IOException if any error occurs during the creation of the directories.
     */
    public void initializeBackupFolder(List<String> folderID) throws IOException
    {
        Path backupFilePath = getBackupFolderPath(folderID);
        Files.createDirectories(backupFilePath);
    }

    private Path getBackupFolderPath(List<String> jobID)
    {
        return environment.getPermanentDirectory().toPath().resolve("adminTools").resolve("backup")
            .resolve(String.join("_", jobID));
    }

    private void createBackupFile(File targetFile, UploadPackageJobResource uploadPackageJobResource,
        PackageUploadJobStatus status)
    {
        Path backupFilePath = getBackupFolderPath(status.getJobID());

        File backupFile = backupFilePath.resolve(targetFile.getName() + ".bak").toFile();
        try {
            Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            JobResult log =
                new JobResult("adminTools.jobs.upload.backup.fail", JobResultLevel.ERROR, backupFile.getName(),
                    targetFile.getName());
            status.addLog(log);
            throw new RuntimeException(e);
        }
        logger.info("Backup file [{}] created for file [{}].", backupFile.getName(), targetFile.getName());
        uploadPackageJobResource.setBackupFile(backupFile);
        status.addLog(new JobResult("adminTools.jobs.upload.backup.success", JobResultLevel.INFO, backupFile.getName(),
            targetFile.getName()));
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
