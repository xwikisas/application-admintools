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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;

/**
 * Manage existing instances. or admin tools manager
 *
 * @version $Id$
 * @since 1.0
 */
@Component(roles = AdminToolsManager.class)
@Singleton
public class AdminToolsManager
{
    /**
     * Contains useful functions for retrieving configuration data.
     */
    @Inject
    @Named("configuration")
    private DataProvider configurationInfo;

    /**
     * Contains useful functions for retrieving security data.
     */
    @Inject
    @Named("security")
    private DataProvider securityInfo;

    @Inject
    private Provider<List<DataProvider>> dataProviderProvider;

//    /**
//     * Extract the configuration settings.
//     *
//     * @return a json containing the configuration info
//     */
//    public Map<String, String> getConfigurationDetails()
//    {
//        return this.configurationInfo.provideData();
//    }
//
//    /**
//     * Extract the security settings.
//     *
//     * @return a json containing the security info
//     */
//    public Map<String, String> getSecurityDetails()
//    {
//        return this.securityInfo.provideData();
//    }

    /**
     * Extract the security settings.
     *
     * @return a json containing the security info
     */
    public List<Block> generateData()
    {
        List<Block> generatedData = new ArrayList<>();
        for (DataProvider dataProvider : this.dataProviderProvider.get()) {
            generatedData.add(dataProvider.provideData());
        }
        return generatedData;
    }

    /**
     * Extract the security settings.
     *
     * @return a json containing the security info
     */
    public List<Block> generateConfigData()
    {
        return configurationInfo.provideData().getChildren();
    }

//    public Map<String, String> generateData(String hint)
//    {
//        Map<String, String> generatedData = new HashMap<>();
//        for (DataProvider dataProvider : this.dataProviderProvider.get()) {
//            generatedData.putAll(dataProvider.provideData());
//        }
//        return generatedData;
//    }
}
