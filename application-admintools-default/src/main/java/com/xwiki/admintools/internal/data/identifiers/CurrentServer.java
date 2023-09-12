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

    private ServerIdentifier usedServer;

    @Inject
    @Named("default")
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
     * Get the used server identifier.
     *
     * @return {@link ServerIdentifier}
     */
    public ServerIdentifier getUsedServer()
    {
        return usedServer;
    }

    /**
     * Verifies if a server type was found and updates the paths.
     */
    public void findPaths()
    {
        if (usedServer == null) {
            String providedConfigServerPath = adminToolsConfig.getServerPath();
            findServer(providedConfigServerPath);
        }
    }

    /**
     * Go through all supported servers and return the one that is used.
     *
     * @param providedConfigServerPath {@link String} server path provided by XWiki configurations.
     */
    private void findServer(String providedConfigServerPath)
    {
        for (ServerIdentifier serverIdentifier : this.supportedServers.get()) {
            if (serverIdentifier.isUsed(providedConfigServerPath)) {
                usedServer = serverIdentifier;
                usedServer.updatePaths(providedConfigServerPath);
                break;
            }
        }
    }
}
