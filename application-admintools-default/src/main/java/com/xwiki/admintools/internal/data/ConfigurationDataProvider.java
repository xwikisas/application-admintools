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
    private CurrentServer usedServer;

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

    /**
     * Provides the info structured in a json.
     *
     * @return Map containing the generated info.
     */
    @Override
    public Map<String, String> generateJson()
    {
        usedServer.updatePaths();
        Map<String, String> systemInfo = new HashMap<>();
        systemInfo.put("xwikiCfgPath", usedServer.getXwikiCfgPath());
        systemInfo.put("tomcatConfPath", this.usedServer.getServerCfgPath());
        systemInfo.put("javaVersion", this.getJavaVersion());
        systemInfo.put("osInfo", this.getOSInfo());

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
    private String getOSInfo()
    {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");

        return String.format("Operating System: %s %s (%s)", osName, osVersion, osArch);
    }
}
