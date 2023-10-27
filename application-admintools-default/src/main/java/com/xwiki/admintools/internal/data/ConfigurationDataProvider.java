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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    private static final String TEMPLATE_NAME = "configurationTemplate.vm";

    @Inject
    private CurrentServer currentServer;

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public String getRenderedData()
    {
        Map<String, String> systemInfo = new HashMap<>();
        try {
            systemInfo = getDataAsJSON();
            systemInfo.put(SERVER_FOUND, "true");
        } catch (Exception e) {
            systemInfo.put(SERVER_FOUND, "false");
        }
        return renderTemplate(TEMPLATE_NAME, systemInfo, HINT);
    }

    @Override
    public Map<String, String> getDataAsJSON() throws Exception
    {
        try {
            Map<String, String> systemInfo = new HashMap<>();
            systemInfo.put("database", this.identifyDB());
            systemInfo.put("xwikiCfgPath", getCurrentServer().getXwikiCfgFolderPath());
            systemInfo.put("tomcatConfPath", this.getCurrentServer().getServerCfgPath());
            systemInfo.put("javaVersion", this.getJavaVersion());
            systemInfo.put("usedServer", this.getCurrentServer().getComponentHint());
            systemInfo.put("xwikiVersion", getXWikiVersion());
            systemInfo.putAll(this.getOSInfo());
            return systemInfo;
        } catch (Exception e) {
            logger.warn("Failed to generate the instance configuration data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new Exception("Failed to generate the instance configuration data.", e);
        }
    }

    /**
     * Get the version of Java used on the server.
     *
     * @return the used Java version.
     */
    private String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    /**
     * Identify the used database for XWiki by verifying the configration files.
     *
     * @return the name of the used database or {@code null} in case an error occurred or the used DB is not supported.
     */
    private String identifyDB()
    {
        String usedDB = null;
        try {
            ServerIdentifier server = getCurrentServer();
            String databaseCfgPath = server.getXwikiCfgFolderPath() + "hibernate.cfg.xml";
            String patternString = "<property name=\"connection.url\">jdbc:(.*?)://";
            Pattern pattern = Pattern.compile(patternString);
            File file = new File(databaseCfgPath);
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String foundDB = matcher.group(1);
                    usedDB = this.currentServer.getSupportedDBs().getOrDefault(foundDB, null);
                    break;
                }
            }
            bufferedReader.close();
            if (usedDB == null) {
                this.logger.warn("Failed to find database. Used database may not be supported!");
            }
        } catch (IOException exception) {
            this.logger.warn("Error while handling database configuration file. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(exception));
        }

        return usedDB;
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
            throw new NullPointerException("Failed to retrieve the used server. Server not found.");
        }
        return serverIdentifier;
    }

    private String getXWikiVersion()
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWiki xWiki = wikiContext.getWiki();
        return xWiki.getVersion();
    }
}
