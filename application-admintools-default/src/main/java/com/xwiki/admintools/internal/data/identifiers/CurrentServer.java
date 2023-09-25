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
import java.util.HashMap;
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

    private ServerIdentifier currentServerIdentifier;

    @Inject
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    private Map<String, String> supportedDB;

    @Override
    public void initialize() throws InitializationException
    {
        supportedDB = new HashMap<>();
        supportedDB.put("mysql", "MySQL");
        supportedDB.put("hsqldb", "HSQLDB");
        supportedDB.put("mariadb", "MariaDB");
        supportedDB.put("postgresql", "PostgreSQL");
        supportedDB.put("oracle", "Oracle");

        currentServerIdentifier = null;
        updateCurrentServer();
    }

    /**
     * Get the used server identifier.
     *
     * @return {@link ServerIdentifier}
     */
    public ServerIdentifier getCurrentServer()
    {
        return currentServerIdentifier;
    }

    /**
     * Get a {@link Map} with the supported databases.
     *
     * @return the supported databases.
     */
    public Map<String, String> getSupportedDB()
    {
        return supportedDB;
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
     * Go through all supported servers and return the one that is used.
     */
    public void updateCurrentServer()
    {
        String providedConfigServerPath = adminToolsConfig.getServerPath();
        currentServerIdentifier = null;
        for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
            if (serverIdentifier.isUsed(providedConfigServerPath)) {
                currentServerIdentifier = serverIdentifier;
                currentServerIdentifier.updatePaths();
                break;
            }
        }
    }
}
