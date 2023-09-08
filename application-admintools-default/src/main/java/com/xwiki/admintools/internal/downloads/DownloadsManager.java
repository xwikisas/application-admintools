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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.FilesResourceProvider;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Encapsulates functions used for downloading important server files.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = DownloadsManager.class)
@Singleton
public class DownloadsManager implements Initializable
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * A list of all supported server file downloaders.
     */
    @Inject
    private Provider<List<FilesResourceProvider>> filesResourceProviderProvider;

    private FilesResourceProvider usedFilesResourceProvider;

    @Inject
    @Named(ConfigurationDataProvider.HINT)
    private DataProvider configurationDataProvider;

    private String serverPath;

    @Inject
    private Logger logger;

    /**
     * Initializes variables with the server type and the path to the server.
     *
     * @throws InitializationException
     */
    @Override
    public void initialize() throws InitializationException
    {
        Map<String, String> identifiers = currentServer.getServerIdentifiers();
        serverPath = identifiers.get("serverPath");
        String usedServerLogsHint = identifiers.get("serverType") + "Logs";
        findUsedServer(usedServerLogsHint);
    }

    /**
     * Initiates the download process for the xwiki files. It removes the sensitive content from the file.
     *
     * @param fileType {@link String} representing the file type.
     * @return filtered file content as a {@link Byte} array
     * @throws IOException
     */
    public byte[] getXWikiFile(String fileType) throws IOException
    {
        return usedFilesResourceProvider.getConfigurationFileContent(fileType, currentServer.retrieveXwikiCfgPath());
    }

    /**
     * Checks the admin rights of the calling user.
     *
     * @return {@link Boolean} true if the user is admin, false otherwise
     */
    public boolean isAdmin()
    {
        XWikiContext wikiContext = xcontextProvider.get();
        DocumentReference user = wikiContext.getUserReference();
        WikiReference wikiReference = wikiContext.getWikiReference();
        return this.authorizationManager.hasAccess(Right.ADMIN, user, wikiReference);
    }

    /**
     * Retrieve the selected files from the request and create an archive containing them.
     *
     * @return {@link Byte} array representing the files archive.
     */
    public byte[] downloadMultipleFiles()
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWikiRequest xWikiRequest = wikiContext.getRequest();
        Map<String, String[]> files = xWikiRequest.getParameterMap();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            createArchiveEntries(files, zipOutputStream);
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
     * Select the right server downloader and call the logs retrieval function.
     *
     * @return {@link Byte} array representing the last "n" logs.
     * @throws IOException
     */
    public byte[] callLogsRetriever() throws IOException
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWikiRequest xWikiRequest = wikiContext.getRequest();
        long noLines = Long.parseLong(xWikiRequest.getParameter("noLines"));
        return usedFilesResourceProvider.retrieveLastLogs(serverPath, noLines);
    }

    /**
     * Verify which files are selected, prepare the zip entry and add it to the zip stream.
     *
     * @param files {@link Map} representing the files that are to be processed.
     * @param zipOutputStream {@link ZipOutputStream} where the zip entries are stored.
     * @throws IOException
     */
    private void createArchiveEntries(Map<String, String[]> files, ZipOutputStream zipOutputStream) throws IOException
    {
        ZipEntry zipEntry;
        byte[] buffer;
        for (String fileType : files.get("files")) {
            switch (fileType) {
                case "cfg_file":
                    zipEntry = new ZipEntry("xwiki.cfg");
                    zipOutputStream.putNextEntry(zipEntry);
                    buffer = usedFilesResourceProvider.getConfigurationFileContent("config",
                        currentServer.retrieveXwikiCfgPath());
                    zipOutputStream.write(buffer, 0, buffer.length);
                    zipOutputStream.closeEntry();
                    break;
                case "properties_file":
                    zipEntry = new ZipEntry("xwiki.properties");
                    zipOutputStream.putNextEntry(zipEntry);
                    buffer = usedFilesResourceProvider.getConfigurationFileContent("properties",
                        currentServer.retrieveXwikiCfgPath());
                    zipOutputStream.write(buffer, 0, buffer.length);
                    zipOutputStream.closeEntry();
                    break;
                case "logs":
                    zipEntry = new ZipEntry("logs.zip");
                    zipOutputStream.putNextEntry(zipEntry);
                    Map<String, String> filters = new HashMap<>();
                    String from = "from";
                    String to = "to";
                    filters.put(from, !Objects.equals(files.get(from)[0], "") ? files.get(from)[0] : null);
                    filters.put(to, !Objects.equals(files.get(to)[0], "") ? files.get(to)[0] : null);
                    buffer = getLogs(filters);
                    zipOutputStream.write(buffer, 0, buffer.length);
                    zipOutputStream.closeEntry();
                    break;
                case "configuration_info":
                    zipEntry = new ZipEntry("configuration_json.txt");
                    zipOutputStream.putNextEntry(zipEntry);
                    buffer = configurationDataProvider.generateJson().toString().getBytes();
                    zipOutputStream.write(buffer, 0, buffer.length);
                    zipOutputStream.closeEntry();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Initiates the download process for the server logs. Taking into consideration the used server, it identifies the
     * right downloader. It returns null if none is corresponding.
     *
     * @param filters {@link Map} that can contain the start and end date of the search. It can also be empty.
     * @return {@link Byte} array representing the logs archive.
     */
    private byte[] getLogs(Map<String, String> filters)
    {
        return usedFilesResourceProvider.generateLogsArchive(filters, serverPath);
    }

    /**
     * Calls the logs generator for the used server.
     *
     * @param hint {@link String} represents the used server hint.
     */
    private void findUsedServer(String hint)
    {
        for (FilesResourceProvider specificFilesResourceProvider : this.filesResourceProviderProvider.get()) {
            if (specificFilesResourceProvider.getIdentifier().equals(hint)) {
                usedFilesResourceProvider = specificFilesResourceProvider;
            }
        }
    }
}
