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
 * TBC.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
public interface ServerIdentifier
{
    /**
     * TBC.
     * @param providedConfigServerPath TBC
     * @return TBC
     */
    boolean isUsed(String providedConfigServerPath);

    /**
     * Extract the hint of a component.
     *
     * @return component hint
     */
    String getIdentifier();

    /**
     * TBC.
     *
     * @return TBC
     */
    String getServerCfgPath();

    /**
     * TBC.
     *
     * @return TBC
     */
    String getXwikiCfgPath();

    /**
     * TBC.
     *
     * @param providedConfigServerPath TBC
     */
    void updatePaths(String providedConfigServerPath);

    /**
     * TBC.
     * @return TBC
     */
    Map<String, String> getServerIdentifiers();
}
