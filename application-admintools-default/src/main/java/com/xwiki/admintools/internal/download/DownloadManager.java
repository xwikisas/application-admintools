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
package com.xwiki.admintools.internal.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.download.ArchiverResourceProvider;
import com.xwiki.admintools.download.ViewerResourceProvider;
import com.xwiki.admintools.internal.download.archiver.LogsArchiverResourceProvider;

/**
 * Encapsulates functions used for downloading important server files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = DownloadManager.class)
@Singleton
public class DownloadManager
{
    private final String from = "from";

    private final String to = "to";

    @Inject
    private Provider<List<ArchiverResourceProvider>> archiverResourceProviders;

    @Inject
    private Provider<List<ViewerResourceProvider>> viewerResourceProviders;

    @Inject
    private LogsArchiverResourceProvider logsArchiverResourceProvider;

    @Inject
    private Logger logger;

    /**
     * Initiates the download process for the xwiki files. It removes the sensitive content from the file.
     *
     * @param input {@link String} representing the file type.
     * @return filtered file content as a {@link Byte} array
     * @throws IOException
     */
    public byte[] getFileView(String input, String hint) throws IOException
    {
        ViewerResourceProvider fileViewerProvider = findViewerProvider(hint);
        if (fileViewerProvider != null) {
            return fileViewerProvider.getByteData(input);
        } else {
            return null;
        }
    }

    /**
     * Retrieve the selected files from the request and create an archive containing them.
     *
     * @return {@link Byte} array representing the files archive.
     */
    public byte[] downloadMultipleFiles(Map<String, String[]> files)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String archiverHint : files.get("files")) {
                if (archiverHint.equals("logs")) {
                    Map<String, String> filters = new HashMap<>();
                    filters.put(from, !Objects.equals(files.get(from)[0], "") ? files.get(from)[0] : null);
                    filters.put(to, !Objects.equals(files.get(to)[0], "") ? files.get(to)[0] : null);
                    logsArchiverResourceProvider.writeArchiveEntry(filters, zipOutputStream);
                } else {
                    ArchiverResourceProvider archiver = findArchiverProvider(archiverHint);
                    if (archiver != null) {
                        archiver.writeArchiveEntry(zipOutputStream);
                    }
                }
            }

            zipOutputStream.flush();
            byteArrayOutputStream.flush();
            zipOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            logger.warn("Failed to download logs. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private ViewerResourceProvider findViewerProvider(String hint)
    {
        for (ViewerResourceProvider viewerResourceProvider : viewerResourceProviders.get()) {
            if (viewerResourceProvider.getIdentifier().equals(hint)) {
                return viewerResourceProvider;
            }
        }
        return null;
    }

    private ArchiverResourceProvider findArchiverProvider(String hint)
    {
        for (ArchiverResourceProvider archiverResourceProvider : archiverResourceProviders.get()) {
            if (archiverResourceProvider.getIdentifier().equals(hint)) {
                return archiverResourceProvider;
            }
        }
        return null;
    }
}
