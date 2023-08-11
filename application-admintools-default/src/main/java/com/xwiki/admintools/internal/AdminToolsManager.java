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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.internal.util.ConfigurationInfo;

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
    private ConfigurationInfo configurationInfo;

    /**
     * Admin Tools script services.
     *
     * @return String
     */
    public Map<String, String> getConfigurationDetails()
    {
        return this.configurationInfo.generateConfigurationDetails();
    }
}
