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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.wikiUsage.UsageDataProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Extension of {@link AbstractDataProvider} for retrieving configuration data.
 *
 * @version $Id$
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

    private static final String METADATA_NAME = "name";

    private static final String METADATA_VERSION = "version";

    @Inject
    private CurrentServer currentServer;

    @Inject
    private UsageDataProvider usageDataProvider;

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
            logger.warn("Failed to generate the instance configuration data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            systemInfo.put(SERVER_FOUND, "false");
        }
        return renderTemplate(TEMPLATE_NAME, systemInfo, HINT);
    }

    @Override
    public Map<String, String> getDataAsJSON() throws Exception
    {
        try {
            Map<String, String> systemInfo = new HashMap<>();
            Map<String, String> dbMetadata = this.usageDataProvider.getDatabaseNetadata();
            systemInfo.put("databaseName", dbMetadata.get(METADATA_NAME));
            systemInfo.put("databaseVersion", dbMetadata.get(METADATA_VERSION));
            systemInfo.put("xwikiCfgPath", getCurrentServer().getXwikiCfgFolderPath());
            systemInfo.put("tomcatConfPath", this.getCurrentServer().getServerCfgPath());
            systemInfo.put("javaVersion", this.getJavaVersion());
            Map<String, String> serverMetadata = this.usageDataProvider.getServerMetadata();
            systemInfo.put("usedServerName", serverMetadata.get(METADATA_NAME));
            systemInfo.put("usedServerVersion", serverMetadata.get(METADATA_VERSION));
            systemInfo.put("xwikiVersion", getXWikiVersion());
            systemInfo.putAll(this.getOSInfo());
            return systemInfo;
        } catch (Exception e) {
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
            throw new NullPointerException("Failed to retrieve the current used server, check your configurations.");
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
