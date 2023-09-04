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
package com.xwiki.admintools.internal.downloads;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.logging.Logger;

/**
 * Encapsulates functions used for downloading configuration files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(TomcatLogsDownloader.HINT)
@Singleton
public class TomcatLogsDownloader extends AbstractLogsDownloader
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "tomcatLogs";



    /**
     * @param filter
     * @return
     */
    @Override
    public byte[] generateLogsArchive(Map<String, String> filter, String serverPath)
    {
        File logsFolder = new File(serverPath + "/logs");
        File[] listOfFiles = logsFolder.listFiles();
        Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

        return this.generateArchive(filter, listOfFiles, pattern);
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }
}
