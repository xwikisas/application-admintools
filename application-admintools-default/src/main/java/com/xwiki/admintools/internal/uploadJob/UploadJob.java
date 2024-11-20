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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;
import com.xwiki.admintools.jobs.PackageUploadJobRequest;
import com.xwiki.admintools.jobs.PackageUploadJobStatus;
import com.xwiki.admintools.uploadPackageJob.UploadPackageJobResource;

/**
 * The Admin Tools package upload job.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named(UploadJob.JOB_TYPE)
@Unstable
public class UploadJob extends AbstractJob<PackageUploadJobRequest, PackageUploadJobStatus> implements GroupedJob
{
    /**
     * Admin Tools package upload job type.
     */
    public static final String JOB_TYPE = "admintools.uploadpackage";

    private static final String NEW_FILE_PATH_FORMAT = "%s/%s";

    private static final String FILE_TYPE_BACKUP = "backup";

    private static final String FILE_TYPE_TARGET = "target";

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    private List<UploadPackageJobResource> jobResourceList = new ArrayList<>();

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return new JobGroupPath(List.of("adminTools", "import", request.getFileRef()));
    }

    @Override
    protected PackageUploadJobStatus createNewStatus(PackageUploadJobRequest request)
    {
        return new PackageUploadJobStatus(JOB_TYPE, request, observationManager, loggerManager);
    }

    /**
     * Run the package upload job.
     */
    @Override
    protected void runInternal()
    {
        try {
            jobResourceList.clear();
            if (!status.isCanceled()) {
                this.progressManager.pushLevelProgress(this);
                String path = currentServer.getCurrentServer().getXWikiInstallFolderPath();
                InputStream fileInputStream = getInputStream();
                try (ZipInputStream zis = new ZipInputStream(fileInputStream)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            progressManager.startStep(this);
                            UploadPackageJobResource jobResource = maybeBackupFile(path, entry.getName());
                            processFileContent(zis, jobResource);
                            jobResourceList.add(jobResource);
                            progressManager.endStep(this);
                            Thread.yield();
                        }
                        zis.closeEntry();
                    }
                }
                batchSave();
            }
        } catch (Exception e) {
            logger.error(ExceptionUtils.getRootCauseMessage(e));
            progressManager.endStep(this);
            Thread.yield();
            batchRestore();
            throw new RuntimeException(e);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private InputStream getInputStream() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        AttachmentReference attachmentReference = attachmentReferenceResolver.resolve(request.getFileRef());
        XWikiDocument parentDoc = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
        return parentDoc.getAttachment(attachmentReference.getName()).getContentInputStream(xcontext);
    }

    private void processFileContent(ZipInputStream zis, UploadPackageJobResource jobResource) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            baos.write(buffer, 0, len);
        }
        jobResource.setNewFileContent(baos);
    }

    private void batchRestore()
    {
        for (UploadPackageJobResource jobResource : jobResourceList) {
            progressManager.startStep(this);
            File backupFile = jobResource.getBackupFile();
            File targetFile = jobResource.getTargetFile();

            if (!jobResource.isUploaded()) {
                if (!handleFileDeletion(backupFile, FILE_TYPE_BACKUP)) {
                    continue;
                }
            } else if (backupFile == null) {
                if (!handleFileDeletion(targetFile, FILE_TYPE_TARGET)) {
                    continue;
                }
            } else {
                try {
                    String newFilePath =
                        String.format(NEW_FILE_PATH_FORMAT, targetFile.getParent(), jobResource.getNewFileName());
                    File newFile = new File(newFilePath);
                    boolean newFileDeleteFailure = !handleFileDeletion(newFile, FILE_TYPE_TARGET);
                    Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    boolean backupFileDeleteFailure = !handleFileDeletion(backupFile, FILE_TYPE_BACKUP);
                    if (newFileDeleteFailure || backupFileDeleteFailure) {
                        continue;
                    }
                } catch (IOException e) {
                    backupFailureMessage("copy", backupFile.getName(), targetFile.getName());
                    continue;
                }
            }
            JobResult log =
                new JobResult("adminTools.jobs.upload.batch.backup.success", JobResultLevel.INFO,
                    jobResource.getNewFileName());
            status.addLog(log);
            progressManager.endStep(this);
            Thread.yield();
        }
    }

    private boolean handleFileDeletion(File file, String fileType)
    {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                backupFailureMessage(fileType, file.getName());
                progressManager.endStep(this);
                Thread.yield();
                return false;
            }
        }
        return true;
    }

    private void backupFailureMessage(String translation, String... parameters)
    {
        String translationHint = String.format("adminTools.jobs.upload.batch.backup.%s.fail", translation);
        JobResult log = new JobResult(translationHint, JobResultLevel.ERROR, parameters);
        status.addLog(log);
    }

    private void batchSave() throws IOException
    {
        for (UploadPackageJobResource jobResource : jobResourceList) {
            progressManager.startStep(this);
            File targetFile = jobResource.getTargetFile();
            if (targetFile.exists()) {
                String filePath = jobResource.getTargetFile().getParent();
                if (!targetFile.delete()) {
                    JobResult log =
                        new JobResult("adminTools.jobs.upload.batch.save.fail", JobResultLevel.ERROR,
                            targetFile.getName());
                    status.addLog(log);
                    throw new RuntimeException(
                        String.format("Failed to delete original file [%s].", targetFile.getName()));
                }
                File newFile = new File(String.format(NEW_FILE_PATH_FORMAT, filePath, jobResource.getNewFileName()));
                Files.write(newFile.toPath(), jobResource.getNewFileContent().toByteArray());
            } else {
                Files.write(targetFile.toPath(), jobResource.getNewFileContent().toByteArray());
            }
            jobResource.setUploaded();
            JobResult log =
                new JobResult("adminTools.jobs.upload.batch.save.success", JobResultLevel.INFO,
                    jobResource.getNewFileName());
            status.addLog(log);
            progressManager.endStep(this);
            Thread.yield();
        }
    }

    private UploadPackageJobResource maybeBackupFile(String xwikiInstallationPath, String archiveFilePath)
    {
        File targetFile = new File(xwikiInstallationPath + archiveFilePath);
        UploadPackageJobResource uploadPackageJobResource = new UploadPackageJobResource();
        String fileName = targetFile.getName();
        uploadPackageJobResource.setNewFileName(fileName);
        if (targetFile.exists()) {
            createBackupFile(targetFile.getAbsolutePath() + ".bak", targetFile, uploadPackageJobResource, fileName);
        } else {
            String extension = archiveFilePath.substring(archiveFilePath.lastIndexOf("."));
            if (extension.equals(".jar")) {
                String simpleFileName = extractNameBeforeVersion(fileName);
                String directoryPath = targetFile.getParent();
                String[] filesList = new File(directoryPath).list(
                    ((dir, name) -> name.startsWith(simpleFileName) && name.endsWith(extension)));
                if (filesList == null || filesList.length > 1) {
                    JobResult log =
                        new JobResult("adminTools.jobs.upload.assessoriginal.fail", JobResultLevel.ERROR,
                            fileName);
                    status.addLog(log);
                    throw new RuntimeException(
                        String.format("Unable to correctly assess the original file for %s.", fileName));
                } else if (filesList.length == 1) {
                    String oldFilePath = String.format(NEW_FILE_PATH_FORMAT, directoryPath, filesList[0]);
                    targetFile = new File(oldFilePath);
                    createBackupFile(String.format("%s.bak", oldFilePath), targetFile, uploadPackageJobResource,
                        fileName);
                }
            }
        }
        uploadPackageJobResource.setTargetFile(targetFile);
        return uploadPackageJobResource;
    }

    private void createBackupFile(String backupFilePath, File targetFile,
        UploadPackageJobResource uploadPackageJobResource, String fileName)
    {
        File backupFile = new File(backupFilePath);
        try {
            Files.copy(targetFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JobResult log = new JobResult("adminTools.jobs.upload.backup.fail", JobResultLevel.ERROR,
                backupFile.getName(), fileName);
            status.addLog(log);
            throw new RuntimeException(e);
        }
        uploadPackageJobResource.setBackupFile(backupFile);
        JobResult log = new JobResult("adminTools.jobs.upload.backup.success", JobResultLevel.INFO,
            backupFile.getName(), fileName);
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
