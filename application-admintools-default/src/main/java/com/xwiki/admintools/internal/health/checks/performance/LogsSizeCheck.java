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
package com.xwiki.admintools.internal.health.checks.performance;

import java.io.File;
import java.text.DecimalFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

@Component
@Named(LogsSizeCheck.HINT)
@Singleton
public class LogsSizeCheck implements HealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "logSize";

    @Inject
    private CurrentServer currentServer;

    @Override
    public HealthCheckResult check()
    {
        ServerIdentifier server = currentServer.getCurrentServer();

        long logsSize = getFolderSize(server.getLogsFolderPath());

        String[] units = new String[] { "B", "KB", "MB", "GB" };
        int unitIndex = (int) (Math.log10(logsSize) / 3);
        double unitValue = 1 << (unitIndex * 10);

        String readableSize = new DecimalFormat("#,##0.#").format(logsSize / unitValue) + " " + units[unitIndex];

        // NEED TO SET SOME METRICS. WHAT LOG SIZE IS TOO BIG?
        if (logsSize / (1024 * 1024) > 500) {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.performance.cpu.info", "info");
        } else {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.performance.cpu.warn",
                "adminTools.dashboard.healthcheck.performance.cpu.recommendation", "error", "cpuSpecifications");
        }
    }

    public static long getFolderSize(String folderPath)
    {
        File folder = new File(folderPath);
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += getFolderSize(file.getAbsolutePath());
                }
            }
        }
        return length;
    }
}
