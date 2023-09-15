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

    @Override
    public void initialize() throws InitializationException
    {
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
        if (currentServerIdentifier == null) {
            String providedConfigServerPath = adminToolsConfig.getServerPath();
            for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
                if (serverIdentifier.isUsed(providedConfigServerPath)) {
                    currentServerIdentifier = serverIdentifier;
                    currentServerIdentifier.updatePaths();
                    break;
                }
            }
        }
    }
}
