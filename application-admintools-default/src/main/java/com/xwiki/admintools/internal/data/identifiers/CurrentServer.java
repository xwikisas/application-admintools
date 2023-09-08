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
package com.xwiki.admintools.internal.data.identifiers;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.internal.configuration.DefaultAdminToolsConfiguration;

/**
 * Manages the server identifiers and offers endpoints to retrieve info about their paths.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = CurrentServer.class)
@Singleton
public class CurrentServer implements Initializable
{
    @Inject
    private Provider<List<ServerIdentifier>> supportedServers;

    private ServerIdentifier usedServer;

    @Inject
    @Named(DefaultAdminToolsConfiguration.HINT)
    private AdminToolsConfiguration adminToolsConfig;

    /**
     * Method called by the Component Manager when the component is created for the first time (i.e. when it's looked up
     * for the first time or if the component is specified as being loaded on startup). If the component instantiation
     * strategy is singleton then this method is called only once during the lifecycle of the Component Manager.
     * Otherwise the component is created at each lookup and thus this method is called at each lookup too.
     *
     * @throws InitializationException if an error happens during a component's initialization
     */
    @Override
    public void initialize() throws InitializationException
    {
        String providedConfigServerPath = adminToolsConfig.getServerPath();
        findServer(providedConfigServerPath);
    }

    /**
     * Retrieves the XWiki configuration file path for the installation.
     *
     * @return a String representing the path to the XWiki configuration file.
     */
    public String getXwikiCfgPath()
    {
        return usedServer.getXwikiCfgPath();
    }

    /**
     * Retrieves the server configuration file path for the installation.
     *
     * @return a String representing the path to the server configuration file.
     */
    public String getServerCfgPath()
    {
        return usedServer.getServerCfgPath();
    }

    /**
     * Verifies if a server type was found and updates the paths.
     */
    public void updatePaths()
    {
        String providedConfigServerPath = adminToolsConfig.getServerPath();
        if (usedServer == null) {
            findServer(providedConfigServerPath);
            usedServer.updatePaths(providedConfigServerPath);
        } else {
            usedServer.updatePaths(providedConfigServerPath);
        }
    }

    /**
     * Calls the used server function to retrieve the server identifiers.
     *
     * @return a Map<String, String> with the info used to identify the server.
     */
    public Map<String, String> getServerIdentifiers()
    {
        return usedServer.getServerIdentifiers();
    }

    /**
     * Go through all supported servers and return the one that is used.
     *
     * @param providedConfigServerPath server path provided by XWiki configurations.
     */
    private void findServer(String providedConfigServerPath)
    {
        for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
            if (serverIdentifier.isUsed(providedConfigServerPath)) {
                usedServer = serverIdentifier;
                break;
            }
        }
    }
}
