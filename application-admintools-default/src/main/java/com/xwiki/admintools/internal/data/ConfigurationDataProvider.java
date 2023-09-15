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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;

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

    private final String template = "data/configurationTemplate.vm";

    private Map<String, String> supportedDB;

    @Inject
    private DefaultFileOperations fileOperations;

    @Inject
    private CurrentServer currentServer;

    @Override
    public void initialize() throws InitializationException
    {
        supportedDB = new HashMap<String, String>()
        {{
            put("mysql", "MySQL");
            put("hsqldb", "HSQLDB");
            put("mariadb", "MariaDB");
            put("postgresql", "PostgreSQL");
            put("oracle", "Oracle");
        }};
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public String provideData()
    {
        Map<String, String> systemInfo = generateJson();
        if (systemInfo != null) {
            systemInfo.put(serverFound, "found");
        } else {
            systemInfo = new HashMap<>();
            systemInfo.put(serverFound, null);
            systemInfo.put("supportedServers", currentServer.getSupportedServers().toString());
        }
        return renderTemplate(template, systemInfo, HINT);
    }

    @Override
    public Map<String, String> generateJson()
    {
        try {
            currentServer.updateCurrentServer();
            Map<String, String> systemInfo = new HashMap<>();
            systemInfo.put("xwikiCfgPath", currentServer.getCurrentServer().getXwikiCfgFolderPath());
            systemInfo.put("tomcatConfPath", this.currentServer.getCurrentServer().getServerCfgPath());
            systemInfo.put("javaVersion", this.getJavaVersion());
            systemInfo.putAll(this.getOSInfo());
            systemInfo.put("database", this.identifyDB());
            systemInfo.put("usedServer", this.currentServer.getCurrentServer().getComponentHint());
            return systemInfo;
        } catch (Exception e) {
            logger.warn("Failed to generate the configuration details. Error info : [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
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
        String databaseCfgPath = currentServer.getCurrentServer().getXwikiCfgFolderPath() + "hibernate.cfg.xml";
        File file = new File(databaseCfgPath);

        try {
            fileOperations.initializeScanner(file);
            String usedDB = "not found";
            while (fileOperations.hasNextLine()) {
                String line = fileOperations.nextLine();
                if (line.contains("<property name=\"connection.url\">jdbc:")) {
                    String patternString = "jdbc:(.*?)://";
                    Pattern pattern = Pattern.compile(patternString);
                    Matcher matcher = pattern.matcher(line);
                    String foundDB = "";
                    if (matcher.find()) {
                        foundDB = matcher.group(1);
                    } else {
                        logger.warn("Failed to find database");
                    }

                    usedDB = supportedDB.getOrDefault(foundDB, "not found");
                    break;
                }
            }
            fileOperations.closeScanner();
            return usedDB;
        } catch (FileNotFoundException e) {
            logger.warn("Failed to open database configuration file. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Get info about the OS that XWiki is running on.
     *
     * @return info about the OS structured in a {@link Map}.
     */
    Map<String, String> getOSInfo()
    {
        Map<String, String> result = new HashMap<>();
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("osArch", System.getProperty("os.arch"));

        return result;
    }
}
