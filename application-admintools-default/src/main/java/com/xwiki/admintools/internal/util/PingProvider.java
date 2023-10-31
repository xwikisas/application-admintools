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
package com.xwiki.admintools.internal.util;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.internal.PingDataProvider;
import org.xwiki.activeinstalls2.internal.data.DatabasePing;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.activeinstalls2.internal.data.ServletContainerPing;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

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

    private Ping ping;

    /**
     * Initialize {@link Ping}.
     *
     * @throws InitializationException
     */
    @Override
    public void initialize() throws InitializationException
    {
        ping = new Ping();
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
}
