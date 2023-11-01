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
package com.xwiki.admintools.internal.files;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

/**
 * Endpoints used for accessing important server files.
 *
 * @version $Id$
 */
@Component(roles = ImportantFilesManager.class)
@Singleton
public class ImportantFilesManager
{
    private static final String TEMPLATE_NAME = "filesSectionTemplate.vm";

    private static final String REQUESTED_FILES_KEY = "files";

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
     * @param params {@link Map} containing the needed filters.
     * @return filtered file content as a {@link Byte} array
     */
    public byte[] getFile(String hint, Map<String, String[]> params) throws Exception
    {
        DataResource fileViewerProvider = findDataResource(hint);
        if (fileViewerProvider == null) {
            throw new NullPointerException(
                String.format("Could not find a DataResource implementation for [%s].", hint));
        }
        try {
            return fileViewerProvider.getByteData(params);
        } catch (IOException e) {
            throw new IOException("Error while managing file.", e);
        } catch (Exception e) {
            throw new Exception("Error while processing file content.", e);
        }
    }

    /**
     * Get an archive that contains specific files. For some of these files, a period filtering might also be applied.
     *
     * @param params parameters needed for filtering the requested files
     * @return an archive with files
     */
    public byte[] getFilesArchive(Map<String, String[]> params) throws Exception
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            for (String dataResourceHint : params.get(REQUESTED_FILES_KEY)) {
                DataResource archiver = findDataResource(dataResourceHint);
                // Get only the filters and exclude the requested files.
                Map<String, String[]> filteredParams =
                    params.entrySet().stream().filter(entry -> !entry.getKey().equals(REQUESTED_FILES_KEY))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if (archiver != null) {
                    archiver.addZipEntry(zipOutputStream, filteredParams);
                }
            }

            zipOutputStream.flush();
            byteArrayOutputStream.flush();
            zipOutputStream.close();
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new Exception("Error while generating the files archive.", e);
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
            boolean found = currentServer.getCurrentServer() != null;
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            scriptContext.setAttribute("found", found, ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render(TEMPLATE_NAME);
        } catch (Exception e) {
            this.logger.warn("Failed to render [{}] template. Root cause is: [{}]", TEMPLATE_NAME,
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
