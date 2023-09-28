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
package com.xwiki.admintools.internal.data;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.util.DefaultFileOperations;

/**
 * Extension of {@link AbstractDataProvider} for retrieving configuration data.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(ConfigurationDataProvider.HINT)
@Singleton
public class ConfigurationDataProvider extends AbstractDataProvider
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "configuration";

    private final String template = "configurationTemplate.vm";

    @Inject
    private DefaultFileOperations fileOperations;

    @Inject
    private CurrentServer currentServer;

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public String provideData()
    {
        Map<String, String> systemInfo = new HashMap<>();
        try {
            systemInfo = provideJson();
            systemInfo.put(serverFound, "found");
        } catch (Exception e) {
            systemInfo.put(serverFound, null);
        }
        return renderTemplate(template, systemInfo, HINT);
    }

    @Override
    public Map<String, String> provideJson() throws Exception
    {
        try {
            Map<String, String> systemInfo = new HashMap<>();
            systemInfo.put("xwikiCfgPath", getCurrentServer().getXwikiCfgFolderPath());
            systemInfo.put("tomcatConfPath", this.getCurrentServer().getServerCfgPath());
            systemInfo.put("usedServer", this.getCurrentServer().getComponentHint());
            systemInfo.put("javaVersion", this.getJavaVersion());
            systemInfo.put("xwikiVersion", this.getXWikiInstallationVersion());
            systemInfo.putAll(this.getOSInfo());
            systemInfo.put("database", this.identifyDB());
            return systemInfo;
        } catch (Exception e) {
            throw new Exception(String.format("Failed to generate the configuration json. Error info: %s",
                ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    /**
     * Get the version of Java used on the server.
     *
     * @return the used Java version.
     */
    String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    /**
     * Identify the used database for XWiki by verifying the configration files.
     *
     * @return the name of the used database.
     */
    String identifyDB()
    {
        String usedDB = null;
        try {
            ServerIdentifier server = getCurrentServer();
            String databaseCfgPath = server.getXwikiCfgFolderPath() + "hibernate.cfg.xml";
            String patternString = "<property name=\"connection.url\">jdbc:(.*?)://";
            Pattern pattern = Pattern.compile(patternString);
            fileOperations.openFile(databaseCfgPath);
            fileOperations.initializeScanner();

            while (fileOperations.hasNextLine()) {
                String line = fileOperations.nextLine();
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String foundDB = matcher.group(1);
                    usedDB = currentServer.getSupportedDB().getOrDefault(foundDB, null);
                    break;
                }
            }
            fileOperations.closeScanner();
            if (usedDB == null) {
                logger.warn("Failed to find database. Used database may not be supported!");
            }
        } catch (NullPointerException e) {
            throw new NullPointerException(String.format("Failed to identify used Database. Root cause is: [%s]",
                ExceptionUtils.getRootCauseMessage(e)));
        } catch (Exception exception) {
            logger.warn("Failed to open database configuration file. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(exception));
        }

        return usedDB;
    }

    private String getXWikiInstallationVersion()
    {
        XWikiContext xWikiContext = xcontextProvider.get();
        XWiki xWiki = xWikiContext.getWiki();
        return xWiki.getVersion();
    }

    /**
     * Get info about the OS that XWiki is running on.
     *
     * @return info about the OS structured in a {@link Map}.
     */
    private Map<String, String> getOSInfo()
    {
        Map<String, String> result = new HashMap<>();
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("osArch", System.getProperty("os.arch"));

        return result;
    }

    private ServerIdentifier getCurrentServer()
    {
        ServerIdentifier serverIdentifier = currentServer.getCurrentServer();
        if (serverIdentifier == null) {
            logger.warn("Failed to retrieve used server. Server not found.");
            throw new NullPointerException("Failed to retrieve the used server. Server not found.");
        }
        return serverIdentifier;
    }
}
