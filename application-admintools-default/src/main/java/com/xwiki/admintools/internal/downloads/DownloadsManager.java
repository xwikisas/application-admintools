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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.LogsDownloader;
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
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * A list of all supported server file downloaders.
     */
    @Inject
    private Provider<List<LogsDownloader>> filesDownloader;

    private String serverPath;

    private String serverType;

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
    public byte[] downloadXWikiFile(String fileType) throws IOException
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
        XWiki wiki = wikiContext.getWiki();
        DocumentReference a = wikiContext.getUserReference();
        WikiReference b = wikiContext.getWikiReference();
        boolean c = this.authorizationManager.hasAccess(Right.ADMIN);
        String d = "";
        return c;
    }

    /**
     * Initiates the download process for the server logs. Taking into consideration the used server, it identifies the
     * right downloader. It returns null if none is corresponding.
     *
     * @param filters Map that can contain the start and end date of the search. It can also be empty.
     * @return byte array representing the logs archive.
     */
    public byte[] downloadLogs(Map<String, String> filters)
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
        if (Objects.equals(type, "properties")) {
            filePath += "/webapps/xwiki/WEB-INF/xwiki.properties";
        } else {
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
