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
import java.util.Scanner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Encapsulates functions used for retrieving configuration data.
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
        return getRenderedTemplate("data/configurationTemplate.vm", generateJson(), HINT);
    }

    @Override
    public Map<String, String> generateJson()
    {
        currentServer.updatePaths();
        Map<String, String> systemInfo = new HashMap<>();
        systemInfo.put("xwikiCfgPath", currentServer.getXwikiCfgPath());
        systemInfo.put("tomcatConfPath", this.currentServer.getServerCfgPath());
        systemInfo.put("javaVersion", this.getJavaVersion());
        systemInfo.putAll(this.getOSInfo());
        systemInfo.put("database", this.identifyDB());

        return systemInfo;
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
     * Get info about the OS that XWiki is running on.
     *
     * @return info about the OS.
     */
    private Map<String, String> getOSInfo()
    {
        Map<String, String> result = new HashMap<>();
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        result.put("osArch", System.getProperty("os.arch"));

        return result;
    }

    /**
     * Identify the used database for XWiki by verifying the configration files.
     *
     * @return the name of the used database as a String.
     */
    private String identifyDB()
    {
        String databaseCfgPath = currentServer.getXwikiCfgPath() + "hibernate.cfg.xml";
        File file = new File(databaseCfgPath);

        try (Scanner scanner = new Scanner(file)) {
            String usedDB = null;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("<property name=\"connection.url\">jdbc:")) {
                    if (line.contains("mysql")) {
                        usedDB = "MySQL";
                    } else if (line.contains("hsqldb")) {
                        usedDB = "HSQLDB";
                    } else if (line.contains("mariadb")) {
                        usedDB = "MariaDB";
                    } else if (line.contains("postgresql")) {
                        usedDB = "PostgreSQL";
                    } else if (line.contains("oracle")) {
                        usedDB = "Oracle";
                    }
                    break;
                }
            }
            scanner.close();
            return usedDB;
        } catch (FileNotFoundException e) {
            logger.warn("Failed to open database configuration file. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
