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
package com.xwiki.admintools.internal;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.internal.PingDataProvider;
import org.xwiki.activeinstalls2.internal.data.DatabasePing;
import org.xwiki.activeinstalls2.internal.data.ExtensionPing;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.activeinstalls2.internal.data.ServletContainerPing;
import org.xwiki.activeinstalls2.internal.data.UsersPing;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.XWikiContext;

/**
 * Retrieves {@link Ping} data from Active Installs 2.
 *
 * @version $Id$
 */
@Component(roles = PingProvider.class)
@Singleton
public class PingProvider implements Initializable
{
    @Inject
    @Named("database")
    private PingDataProvider databasePingDataProvider;

    @Inject
    @Named("servlet")
    private PingDataProvider servletPingDataProvider;

    @Inject
    @Named("extensions")
    private PingDataProvider extensionsPingDataProvider;
    
    private Ping ping;

    /**
     * Initialize {@link Ping}.
     *
     * @throws InitializationException
     */
    @Override
    public void initialize() throws InitializationException
    {
        this.ping = new Ping();
    }

    /**
     * Initialize and get {@link DatabasePing}.
     *
     * @return {@link DatabasePing} containing info about the used database.
     */
    public DatabasePing getDatabasePing()
    {
        databasePingDataProvider.provideData(ping);
        return ping.getDatabase();
    }

    /**
     * Initialize and get {@link ServletContainerPing}.
     *
     * @return {@link ServletContainerPing} containing info about the used server.
     */
    public ServletContainerPing getServletPing()
    {
        servletPingDataProvider.provideData(ping);
        return ping.getServletContainer();
    }

    /**
     * Initialize and get {@link ExtensionPing}.
     *
     * @return {@link Collection<ExtensionPing>} containing info about the used server.
     */
    public Collection<ExtensionPing> getExtensionPing()
    {
        extensionsPingDataProvider.provideData(ping);
        return ping.getExtensions();
    }
}
