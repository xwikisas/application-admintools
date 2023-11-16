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
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.ContextualLocalizationManager;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.health.HealthCheck;

/**
 * {@link HealthCheck} implementations to simplify the code.
 *
 * @version $Id$
 */
public abstract class AbstractConfigurationHealthCheck implements HealthCheck
{
    /**
     * Used to render the translations inside the logger.
     */
    @Inject
    protected ContextualLocalizationManager localization;

    @Inject
    protected Logger logger;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    /**
     * Get the JSON needed in the health checks execution.
     *
     * @param hint identifies the {@link DataProvider} for which JSON is required.
     * @return a {@link Map} with the {@link DataProvider} info, or an empty {@link Map} in case of an error.
     */
    protected Map<String, String> getJSON(String hint)
    {
        try {
            TimeUnit.SECONDS.sleep(1);
            DataProvider dataProvider = findDataProvider(hint);
            if (dataProvider == null) {
                throw new NullPointerException("Could not find a matching DataProvider.");
            }
            return dataProvider.getDataAsJSON();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private DataProvider findDataProvider(String hint) throws ComponentLookupException
    {
        return this.contextComponentManager.getInstance(DataProvider.class, hint);
    }
}
