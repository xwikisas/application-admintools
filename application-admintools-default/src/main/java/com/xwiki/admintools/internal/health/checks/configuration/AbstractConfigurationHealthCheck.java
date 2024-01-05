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
package com.xwiki.admintools.internal.health.checks.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;

/**
 * {@link HealthCheck} implementations to simplify the code for configuration related health checks.
 *
 * @version $Id$
 */
public abstract class AbstractConfigurationHealthCheck implements HealthCheck
{
    @Inject
    protected Logger logger;

    @Inject
    @Named(ConfigurationDataProvider.HINT)
    private DataProvider configurationDataProvider;

    /**
     * Retrieve a JSON containing the necessary instance configuration information required for executing the
     * configuration health checks.
     *
     * @return a {@link Map} with the {@link ConfigurationDataProvider} info, or an empty {@link Map} in case of an
     *     error.
     */
    protected Map<String, String> getConfigurationProviderJSON()
    {
        try {
            return configurationDataProvider.getDataAsJSON();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
