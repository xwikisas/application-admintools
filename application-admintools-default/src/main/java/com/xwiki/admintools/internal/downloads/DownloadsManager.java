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
import com.xwiki.admintools.FilesDownloader;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Encapsulates functions used for downloading configuration files.
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

    /**
     * TBC.
     */
    @Inject
    private CurrentServer currentServer;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * A list of all the data providers for Admin Tools.
     */
    @Inject
    private Provider<List<FilesDownloader>> filesDownloader;

    private String serverPath;

    private String serverType;

    /**
     * TBC.
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
     * TBC.
     *
     * @param fileType TBC.
     * @return TBC.
     * @throws IOException
     */
    public byte[] downloadXWikiFile(String fileType) throws IOException
    {
        return prepareFile(fileType);
    }

    /**
     * @return
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
     * TBC.
     *
     * @param filters TBC
     * @return TBC
     */
    public byte[] downloadLogs(Map<String, String> filters)
    {
        return callLogsDownloader(serverType + "Logs", filters);
    }

    /**
     * As all servers have the same path to the xwiki properties and configuration files, it is not needed to call this
     * function in a server specific class.
     *
     * @param type
     * @return
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

    private byte[] callLogsDownloader(String hint, Map<String, String> filter)
    {
        for (FilesDownloader specificFilesDownloader : this.filesDownloader.get()) {
            if (specificFilesDownloader.getIdentifier().equals(hint)) {
                return specificFilesDownloader.generateLogsArchive(filter, serverPath);
            }
        }
        return null;
    }
}
