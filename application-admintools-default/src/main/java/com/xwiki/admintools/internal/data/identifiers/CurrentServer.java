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
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.internal.data.ServletContainerPing;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.PingProvider;

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

    private static final String SERVER_VERSION_KEY = "version";

    @Inject
    private Provider<List<ServerIdentifier>> supportedServers;

    private ServerIdentifier currentServerIdentifier;

    @Inject
    private PingProvider pingProvider;

    @Override
    public void initialize() throws InitializationException
    {
        updateCurrentServer();
    }

    /**
     * Get the used server identifier.
     *
     * @return {@link ServerIdentifier}
     */
    public ServerIdentifier getCurrentServer()
    {
        return this.currentServerIdentifier;
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
        for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
            supportedServerList.add(serverIdentifier.getComponentHint());
        }
        return supportedServerList;
    }

    /**
     * Access a JSON containing the server metadata.
     *
     * @return the server metadata.
     */
    public Map<String, String> getServerMetadata()
    {
        ServletContainerPing servletPing = pingProvider.getServletPing();
        String serverName = servletPing.getName();
        String serverVersion = servletPing.getVersion();
        return Map.of(SERVER_NAME_KEY, serverName, SERVER_VERSION_KEY, serverVersion);
    }

    /**
     * Go through all supported servers and return the one that is used.
     */
    public void updateCurrentServer()
    {
        this.currentServerIdentifier = null;
        for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
            boolean matchingHint =
                getServerMetadata().get(SERVER_NAME_KEY).toLowerCase().contains(serverIdentifier.getComponentHint());
            if (matchingHint && serverIdentifier.foundServerPath()) {
                this.currentServerIdentifier = serverIdentifier;
                this.currentServerIdentifier.updatePossiblePaths();
                break;
            }
        }
    }
}
