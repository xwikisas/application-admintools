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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.xwiki.admintools.LogsDownloader;
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
    private final String config = "config";

    private final String properties = "properties";

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
    private Provider<List<LogsDownloader>> filesDownloader;

    @Inject
    @Named(ConfigurationDataProvider.HINT)
    private DataProvider configurationDataProvider;

    private String serverPath;

    private String serverType;

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
        serverType = identifiers.get("serverType");
    }

    /**
     * Initiates the download process for the xwiki files. It removes the sensitive content from the file.
     *
     * @param fileType properties of configuration file.
     * @return filtered file content as a byte array
     * @throws IOException
     */
    public byte[] getXWikiFile(String fileType) throws IOException
    {
        return prepareFile(fileType);
    }

    /**
     * Checks the admin rights of the calling user.
     *
     * @return true if the user is admin, false otherwise
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
     * @return byte array representing the files archive.
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

    private void createArchiveEntries(Map<String, String[]> files, ZipOutputStream zipOutputStream) throws IOException
    {
        ZipEntry zipEntry;
        byte[] buffer;
        for (String fileType : files.get("files")) {
            switch (fileType) {
                case "cfg_file":
                    zipEntry = new ZipEntry("xwiki.cfg");
                    zipOutputStream.putNextEntry(zipEntry);
                    buffer = prepareFile(config);
                    zipOutputStream.write(buffer, 0, buffer.length);
                    zipOutputStream.closeEntry();
                    break;
                case "properties_file":
                    zipEntry = new ZipEntry("xwiki.properties");
                    zipOutputStream.putNextEntry(zipEntry);
                    buffer = prepareFile(properties);
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
     * @param filters Map that can contain the start and end date of the search. It can also be empty.
     * @return byte array representing the logs archive.
     */
    private byte[] getLogs(Map<String, String> filters)
    {
        return callLogsDownloader(serverType + "Logs", filters);
    }

    /**
     * Identifies the searched file and filters the sensitive info from it. The searched As all servers types have the
     * same path to the xwiki properties configuration files, it is not needed to call this function in a server
     * specific class.
     *
     * @param type identifies the searched file.
     * @return byte array representing the filtered file content.
     * @throws IOException
     */
    private byte[] prepareFile(String type) throws IOException
    {
        String filePath = serverPath;
        if (Objects.equals(type, properties)) {
            filePath += "/webapps/xwiki/WEB-INF/xwiki.properties";
        } else if (Objects.equals(type, config)) {
            filePath += "/webapps/xwiki/WEB-INF/xwiki.cfg";
        }
        File inputFile = new File(filePath);

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        StringBuilder stringBuilder = new StringBuilder();

        String currentLine;
        List<String> wordsList = new ArrayList<>(
            Arrays.asList("xwiki.authentication.validationKey", "xwiki.authentication.encryptionKey",
                "xwiki.superadminpassword", "extension.repositories.privatemavenid.auth", "mail.sender.password"));

        // Read line by line and do not add it if it contains sensitive info.
        while ((currentLine = reader.readLine()) != null) {
            String trimmedLine = currentLine.trim();
            if (wordsList.stream().anyMatch(trimmedLine::contains)) {
                continue;
            }
            stringBuilder.append(currentLine).append(System.getProperty("line.separator"));
        }
        reader.close();
        return stringBuilder.toString().getBytes();
    }

    /**
     * Calls the logs generator for the used server.
     *
     * @param hint represents the used server hint.
     * @param filter Map representing the filters that can be applied to the search.
     * @return byte array representing the logs archive.
     */
    private byte[] callLogsDownloader(String hint, Map<String, String> filter)
    {
        for (LogsDownloader specificLogsDownloader : this.filesDownloader.get()) {
            if (specificLogsDownloader.getIdentifier().equals(hint)) {
                return specificLogsDownloader.generateLogsArchive(filter, serverPath);
            }
        }
        return null;
    }
}
