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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.internal.wikiUsage.UsageDataProvider;

/**
 * Manages the server identifiers and offers endpoints to retrieve info about their paths.
 *
 * @version $Id$
 */
@Component(roles = CurrentServer.class)
@Singleton
public class CurrentServer implements Initializable
{
    private static final String SERVER_NAME_KEY = "name";

    @Inject
    private Provider<List<ServerInfo>> supportedServers;

    @Inject
    private UsageDataProvider usageDataProvider;

    private ServerInfo currentServerInfo;

    @Override
    public void initialize() throws InitializationException
    {
        updateCurrentServer();
    }

    /**
     * Get the used server identifier.
     *
     * @return {@link ServerInfo}
     */
    public ServerInfo getCurrentServer()
    {
        return this.currentServerInfo;
    }

    /**
     * Get a {@link List} with the supported databases.
     *
     * @return the supported databases.
     */
    public List<String> getSupportedDBs()
    {
        return List.of("MySQL", "HSQL", "MariaDB", "PostgreSQL", "Oracle");
    }

    /**
     * Returns a list of the supported servers.
     *
     * @return {@link List} with the supported servers.
     */
    public List<String> getSupportedServers()
    {
        List<String> supportedServerList = new ArrayList<>();
        for (ServerInfo serverInfo : this.supportedServers.get()) {
            supportedServerList.add(serverInfo.getComponentHint());
        }
        return supportedServerList;
    }

    /**
     * Go through all supported servers and return the one that is used.
     */
    public void updateCurrentServer()
    {
        this.currentServerInfo = null;
        for (ServerInfo serverInfo : this.supportedServers.get()) {
            boolean matchingHint = usageDataProvider.getServerMetadata().get(SERVER_NAME_KEY).toLowerCase()
                .contains(serverInfo.getComponentHint());
            if (matchingHint && serverInfo.isUsed()) {
                this.currentServerInfo = serverInfo;
                this.currentServerInfo.updatePossiblePaths();
                break;
            }
        }
    }
}
