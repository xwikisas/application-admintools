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
package com.xwiki.admintools.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import com.xwiki.admintools.internal.AdminToolsManager;

/**
 * Admin Tools script services.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("admintools")
@Singleton
public class AdminToolsScriptService implements ScriptService
{
    /**
     * Manages the XWiki configuration info.
     */
    @Inject
    private AdminToolsManager adminToolsManager;

    /**
     * Function used to retrieve all the templates of Admin Tools.
     *
     * @return all templates.
     */
    public String getAllData()
    {
        return this.adminToolsManager.generateData();
    }

    /**
     * Get a specific data provider template.
     *
     * @param hint represents the data provider
     * @return the template of a specific data provider
     */
    public String getSpecificData(String hint)
    {
        return this.adminToolsManager.generateData(hint);
    }
}
