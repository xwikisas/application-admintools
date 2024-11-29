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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;
import com.xwiki.admintools.jobs.PackageUploadJobRequest;
import com.xwiki.admintools.jobs.PackageUploadJobStatus;
import com.xwiki.admintools.uploadPackageJob.UploadPackageJobResource;

import static com.xwiki.admintools.internal.uploadJob.UploadJobFileProcessor.NEW_FILE_PATH_FORMAT;

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

    private static final String FILE_TYPE_BACKUP = "backup";

    private static final String FILE_TYPE_TARGET = "target";

    @Inject
    private CurrentServer currentServer;

    @Inject
    private UploadJobFileProcessor fileProcessor;

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
                InputStream fileInputStream = fileProcessor.getArchiveInputStream(request.getFileRef());
                try (ZipInputStream zis = new ZipInputStream(fileInputStream)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            progressManager.startStep(this);
                            UploadPackageJobResource jobResource =
                                fileProcessor.maybeBackupFile(path, entry.getName(), this.status);
                            fileProcessor.processFileContent(zis, jobResource);
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
            JobResult log = new JobResult("adminTools.jobs.upload.batch.backup.success", JobResultLevel.INFO,
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
                String filePath = targetFile.getParent();
                if (!targetFile.delete()) {
                    status.addLog(new JobResult("adminTools.jobs.upload.batch.save.fail", JobResultLevel.ERROR,
                        targetFile.getName()));
                    throw new RuntimeException(
                        String.format("Failed to delete original file [%s].", targetFile.getName()));
                }
                File newFile = new File(String.format(NEW_FILE_PATH_FORMAT, filePath, jobResource.getNewFileName()));
                Files.write(newFile.toPath(), jobResource.getNewFileContent().toByteArray());
            } else {
                Files.write(targetFile.toPath(), jobResource.getNewFileContent().toByteArray());
            }
            jobResource.setUploaded();
            status.addLog(new JobResult("adminTools.jobs.upload.batch.save.success", JobResultLevel.INFO,
                jobResource.getNewFileName()));
            progressManager.endStep(this);
            Thread.yield();
        }
    }
}
