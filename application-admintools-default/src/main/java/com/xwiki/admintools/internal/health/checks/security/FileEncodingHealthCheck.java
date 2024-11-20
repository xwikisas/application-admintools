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
package com.xwiki.admintools.internal.health.checks.security;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.jobs.JobResult;
import com.xwiki.admintools.jobs.JobResultLevel;

/**
 * Implementation of {@link HealthCheck} for checking system file encoding.
 *
 * @version $Id$
 */
@Component
@Named(FileEncodingHealthCheck.HINT)
@Singleton
public class FileEncodingHealthCheck extends AbstractSecurityHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "fileEncoding";

    @Override
    public JobResult check()
    {
        String fileEnc = getSecurityProviderJSON().get(HINT);
        if (fileEnc == null) {
            logger.warn("File encoding could not be detected!");
            return new JobResult("adminTools.dashboard.healthcheck.security.system.file.notFound",
                JobResultLevel.WARN);
        }
        boolean isSafeFileEnc = isSafeEncoding(fileEnc, "System file");
        if (!isSafeFileEnc) {
            return new JobResult("adminTools.dashboard.healthcheck.security.system.file.warn",
                JobResultLevel.WARN, fileEnc);
        }
        return new JobResult("adminTools.dashboard.healthcheck.security.system.file.info",
            JobResultLevel.INFO);
    }
}
