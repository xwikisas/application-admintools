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
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import com.xwiki.admintools.download.DataResource;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.download.resources.LogsDataResource;

/**
 * Endpoints used for accessing important server files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = DownloadManager.class)
@Singleton
public class DownloadManager
{
    private static final String FROM = "from";

    private static final String TO = "to";

    @Inject
    private Provider<List<DataResource>> dataResources;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private Logger logger;

    /**
     * Access system file content.
     *
     * @param hint file type identifier.
     * @param input {@link String} representing the file type.
     * @return filtered file content as a {@link Byte} array
     * @throws IOException
     */
    public byte[] getFileView(String input, String hint) throws IOException
    {
        DataResource fileViewerProvider = findDataResource(hint);
        if (fileViewerProvider != null) {
            return fileViewerProvider.getByteData(input);
        } else {
            return null;
        }
    }

    /**
     * Retrieve files from the request and create an archive with the given entries.
     *
     * @param request {@link Map} With the
     * @return {@link Byte} array representing the request archive.
     */
    public byte[] downloadMultipleFiles(Map<String, String[]> request)
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String dataResourceHint : request.get("files")) {
                Map<String, String> filters = null;
                if (dataResourceHint.equals(LogsDataResource.HINT)) {
                    filters = new HashMap<>();
                    filters.put(FROM, !Objects.equals(request.get(FROM)[0], "") ? request.get(FROM)[0] : null);
                    filters.put(TO, !Objects.equals(request.get(TO)[0], "") ? request.get(TO)[0] : null);
                }
                DataResource archiver = findDataResource(dataResourceHint);
                if (archiver != null) {
                    archiver.addZipEntry(zipOutputStream, filters);
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

    /**
     * Get the data in a format given by the associated template.
     *
     * @return the rendered template as a {@link String}.
     */
    public String renderTemplate()
    {
        try {
            String found = "false";
            if (currentServer.getCurrentServer() != null) {
                found = "true";
            }
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            scriptContext.setAttribute("found", found, ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render("downloadTemplate.vm");
        } catch (Exception e) {
            this.logger.warn("Failed to render custom template. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private DataResource findDataResource(String hint)
    {
        for (DataResource archiverDataResource : dataResources.get()) {
            if (archiverDataResource.getIdentifier().equals(hint)) {
                return archiverDataResource;
            }
        }
        return null;
    }
}
