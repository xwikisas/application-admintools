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
package com.xwiki.admintools;

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Gathers specific parts of data and provides the template for it.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface DataProvider
{
    /**
     * Provides the template rendering the data provider information.
     *
     * @return {@link String} representing the data provider template.
     */
    String provideData();

    /**
     * Provides the info structured in a json.
     *
     * @return Map containing the generated info.
     */
    Map<String, String> generateJson();

    /**
     * Extract the hint of a component.
     *
     * @return {@link String} representing the component hint
     */
    String getIdentifier();
}
