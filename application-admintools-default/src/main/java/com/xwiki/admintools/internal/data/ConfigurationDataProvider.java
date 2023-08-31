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

    /**
     * Collection of functions used to retrieve info about the current server paths and type.
     */
    @Inject
    private CurrentServer usedServer;

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    /**
     * Get the configuration info template.
     *
     * @return xwiki configuration info json.
     */
    @Override
    public String provideData()
    {
        usedServer.updatePaths();
        Map<String, String> systemInfo = new HashMap<>();
        systemInfo.put("xwikiCfgPath", usedServer.getXwikiCfgPath());
        systemInfo.put("tomcatConfPath", this.usedServer.getServerCfgPath());
        systemInfo.put("javaVersion", this.getJavaVersion());
        systemInfo.put("osInfo", this.getOSInfo());

        return getRenderedTemplate("data/configurationTemplate.vm", systemInfo, HINT);
    }

    /**
     * Get the used version of Java.
     *
     * @return the used Java version.
     */
    private String getJavaVersion()
    {
        return System.getProperty("java.version");
    }

    /**
     * Get info about the OS XWiki is running on.
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
