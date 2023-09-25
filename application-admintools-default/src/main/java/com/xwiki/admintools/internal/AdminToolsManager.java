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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Manages the data providers.
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = AdminToolsManager.class)
@Singleton
public class AdminToolsManager
{
    private final String delimiter = ", ";

    /**
     * A list of all the data providers for Admin Tools.
     */
    @Inject
    private Provider<List<DataProvider>> dataProviderProvider;

    /**
     * Currently used server.
     */
    @Inject
    private CurrentServer currentServer;

    /**
     * Get data generated in a specific format, using a template, by each provider and merge it.
     *
     * @return a {@link String} containing all templates builds.
     */
    public String generateData()
    {
        StringBuilder strBuilder = new StringBuilder();

        for (DataProvider dataProvider : this.dataProviderProvider.get()) {
            strBuilder.append(dataProvider.provideData());
            strBuilder.append("\n");
        }
        return strBuilder.toString();
    }

    /**
     * Extract a specific data provider template.
     *
     * @param hint {@link String} represents the data provider identifier.
     * @return a {@link String} representing a template
     */
    public String generateData(String hint)
    {
        for (DataProvider dataProvider : this.dataProviderProvider.get()) {
            if (dataProvider.getIdentifier().equals(hint)) {
                return dataProvider.provideData();
            }
        }
        return null;
    }

    /**
     * Get supported databases.
     *
     * @return supported databases inline and separated with a ",".
     */
    public String getSupportedDB()
    {
        List<String> supportedDBList = new ArrayList<>(currentServer.getSupportedDB().values());
        return String.join(delimiter, supportedDBList);
    }

    /**
     * Get supported servers.
     *
     * @return supported servers inline and separated with a ",".
     */
    public String getSupportedServers()
    {
        return String.join(delimiter, currentServer.getSupportedServers());
    }
}
