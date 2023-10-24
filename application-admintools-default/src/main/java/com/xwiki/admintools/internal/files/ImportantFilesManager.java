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
import com.xwiki.admintools.internal.files.resources.LogsDataResource;

/**
 * Endpoints used for accessing important server files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = ImportantFilesManager.class)
@Singleton
public class ImportantFilesManager
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
     */
    public byte[] getFile(String hint, String input) throws Exception
    {
        try {
            DataResource fileViewerProvider = findDataResource(hint);
            return fileViewerProvider.getByteData(input);
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
            for (String dataResourceHint : params.get("files")) {
                Map<String, String> filters = null;
                if (dataResourceHint.equals(LogsDataResource.HINT)) {
                    filters = new HashMap<>();
                    filters.put(FROM, !Objects.equals(params.get(FROM)[0], "") ? params.get(FROM)[0] : null);
                    filters.put(TO, !Objects.equals(params.get(TO)[0], "") ? params.get(TO)[0] : null);
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
            logger.warn("Error while generating the file archive. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new Exception("Error while generating the file archive.", e);
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
            return this.templateManager.render("filesSectionTemplate.vm");
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
