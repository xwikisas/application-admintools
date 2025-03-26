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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.stability.Unstable;

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

    private static final String FILE_TYPE_BACKUP = "backup";

    private static final String FILE_TYPE_TARGET = "target";

    @Inject
    private UploadJobFileProcessor fileProcessor;

    private List<UploadPackageJobResource> jobResourceList = new ArrayList<>();

    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return new JobGroupPath(List.of("adminTools", "upload"));
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
            if (!status.isCanceled()) {
                logger.info("Started upload job with ID: [{}]", this.status.getJobID());
                this.progressManager.pushLevelProgress(this);
                InputStream fileInputStream = fileProcessor.getArchiveInputStream(request.getFileRef());
                try (ZipInputStream zis = new ZipInputStream(fileInputStream)) {
                    fileProcessor.initializeBackupFolder(this.status.getJobID());
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            progressManager.startStep(this);
                            UploadPackageJobResource jobResource =
                                fileProcessor.maybeBackupFile(entry.getName(), this.status);
                            jobResourceList.add(jobResource);
                            fileProcessor.processFileContent(zis, jobResource, this.status);
                            progressManager.endStep(this);
                        }
                        zis.closeEntry();
                    }
                }
                status.addLog(new JobResult("adminTools.jobs.upload.success", JobResultLevel.INFO));
            }
        } catch (Exception e) {
            logger.error("Error during the file upload job.", e);
            status.addLog(new JobResult("adminTools.jobs.upload.fail", JobResultLevel.ERROR));
            progressManager.endStep(this);
            batchRestore();
        } finally {
            this.progressManager.popLevelProgress(this);
            logger.info("Finished upload job with ID: [{}]", this.status.getJobID());
        }
    }

    private void batchRestore()
    {
        boolean successful = true;
        for (UploadPackageJobResource jobResource : jobResourceList) {
            progressManager.startStep(this);
            File backupFile = jobResource.getBackupFile();
            File targetFile = jobResource.getTargetFile();
            if (backupFile == null) {
                if (!handleFileDeletion(targetFile, FILE_TYPE_TARGET)) {
                    successful = false;
                    continue;
                }
            } else {
                try {
                    File newFile = Paths.get(targetFile.getParent()).resolve(jobResource.getNewFilename()).toFile();
                    boolean newFileDeleteFailure = !handleFileDeletion(newFile, FILE_TYPE_TARGET);
                    Files.copy(backupFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    boolean backupFileDeleteFailure = !handleFileDeletion(backupFile, FILE_TYPE_BACKUP);
                    if (newFileDeleteFailure || backupFileDeleteFailure) {
                        successful = false;
                        continue;
                    }
                } catch (Exception e) {
                    backupFailureMessage("copy", backupFile.getName(), targetFile.getName(),
                        ExceptionUtils.getRootCauseMessage(e));
                    successful = false;
                    continue;
                }
            }
            logger.info("Successfully restored backup and removed new file [{}].", jobResource.getNewFilename());
            JobResult log = new JobResult("adminTools.jobs.upload.batch.restore.file.success", JobResultLevel.INFO,
                jobResource.getNewFilename());
            status.addLog(log);
            progressManager.endStep(this);
        }
        JobResult log;
        if (successful) {
            logger.info("Backup restored with success.");
            log = new JobResult("adminTools.jobs.upload.batch.restore.success", JobResultLevel.INFO);
        } else {
            logger.info("There were issues while trying to restore the backup. Please consult the log.");
            log = new JobResult("adminTools.jobs.upload.batch.restore.fail", JobResultLevel.ERROR);
        }
        status.addLog(log);
    }

    private boolean handleFileDeletion(File file, String fileType)
    {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                backupFailureMessage(fileType, file.getName(), ExceptionUtils.getRootCauseMessage(e));
                progressManager.endStep(this);
                return false;
            }
        }
        return true;
    }

    private void backupFailureMessage(String translation, String... parameters)
    {
        String translationHint = String.format("adminTools.jobs.upload.batch.restore.%s.fail", translation);
        logger.warn(contextLocalization.getTranslationPlain(translationHint, parameters));
        JobResult log = new JobResult(translationHint, JobResultLevel.ERROR, parameters);
        status.addLog(log);
    }
}
