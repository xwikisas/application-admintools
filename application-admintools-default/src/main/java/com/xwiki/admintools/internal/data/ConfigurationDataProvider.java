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
import org.xwiki.component.phase.InitializationException;

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

    private Map<String, String> supportedDB;

    @Inject
    private DefaultFileOperations fileOperations;

    @Inject
    private CurrentServer currentServer;

    @Override
    public void initialize() throws InitializationException
    {
        supportedDB = new HashMap<>();
        supportedDB.put("mysql", "MySQL");
        supportedDB.put("hsqldb", "HSQLDB");
        supportedDB.put("mariadb", "MariaDB");
        supportedDB.put("postgresql", "PostgreSQL");
        supportedDB.put("oracle", "Oracle");
    }

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
            logger.warn(ExceptionUtils.getRootCauseMessage(e));
            systemInfo.put(serverFound, null);
            systemInfo.put("supportedServers", currentServer.getSupportedServers().toString());
        }
        return renderTemplate(template, systemInfo, HINT);
    }

    @Override
    public Map<String, String> provideJson() throws Exception
    {
        try {
            currentServer.updateCurrentServer();
            Map<String, String> systemInfo = new HashMap<>();
            systemInfo.put("xwikiCfgPath", getCurrentServer().getXwikiCfgFolderPath());
            systemInfo.put("tomcatConfPath", this.getCurrentServer().getServerCfgPath());
            systemInfo.put("javaVersion", this.getJavaVersion());
            systemInfo.putAll(this.getOSInfo());
            systemInfo.put("database", this.identifyDB());
            systemInfo.put("usedServer", this.getCurrentServer().getComponentHint());
            return systemInfo;
        } catch (Exception e) {
            throw new Exception(
                "Failed to generate the configuration json. Error info : " + ExceptionUtils.getRootCauseMessage(e));
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
     * @throws Exception
     */
    String identifyDB() throws Exception
    {
        try {
            String databaseCfgPath = getCurrentServer().getXwikiCfgFolderPath() + "hibernate.cfg.xml";
            String patternString = "<property name=\"connection.url\">jdbc:(.*?)://";
            Pattern pattern = Pattern.compile(patternString);
            fileOperations.readFile(databaseCfgPath);
            fileOperations.initializeScanner();
            String errorMessage = "Database not supported.";
            String usedDB = errorMessage;
            while (fileOperations.hasNextLine()) {
                String line = fileOperations.nextLine();

                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String foundDB = matcher.group(1);
                    usedDB = supportedDB.getOrDefault(foundDB, errorMessage);
                    break;
                }
            }
            fileOperations.closeScanner();
            if (usedDB.equals("Database not supported.")) {
                logger.warn("Failed to find database");
            }
            return usedDB;
        } catch (Exception e) {
            throw new Exception(
                "Failed to identify used Database. Root cause is: " + ExceptionUtils.getRootCauseMessage(e));
        }
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

    private ServerIdentifier getCurrentServer() throws Exception
    {
        try {
            return this.currentServer.getCurrentServer();
        } catch (Exception e) {
            throw new Exception(
                "Failed to retrieve used server. Root cause is: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
