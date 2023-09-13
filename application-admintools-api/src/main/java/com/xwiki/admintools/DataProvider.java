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

import org.xwiki.component.annotation.Role;

/**
 * Gathers specific data and returns it in a specific format, using a template.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface DataProvider
{
    /**
     * Provides the data provider information in a format given by the associated template.
     *
     * @return {@link String} representing the information formatted by the associated template.
     */
    String provideData();

    /**
     * Extract the hint of a component.
     *
     * @return {@link String} representing the component hint
     */
    String getIdentifier();
}
