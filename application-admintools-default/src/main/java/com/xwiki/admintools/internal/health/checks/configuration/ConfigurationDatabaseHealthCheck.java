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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.health.HealthCheckResultLevel;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Implementation of {@link HealthCheck} for checking the database configuration.
 *
 * @version $Id$
 */
@Component
@Named(ConfigurationDatabaseHealthCheck.HINT)
@Singleton
public class ConfigurationDatabaseHealthCheck extends AbstractConfigurationHealthCheck
{
    /**
     * Component identifier.
     */
    public static final String HINT = "configurationDatabase";

    @Inject
    private CurrentServer currentServer;

    @Override
    public HealthCheckResult check()
    {
        String usedDatabase = getConfigurationProviderJSON().get("databaseName");
        if (usedDatabase == null) {
            logger.warn("Database not found!");
            return new HealthCheckResult("adminTools.dashboard.healthcheck.database.warn",
                HealthCheckResultLevel.ERROR);
        }
        if (currentServer.getSupportedDBs().stream()
            .anyMatch(d -> usedDatabase.toLowerCase().contains(d.toLowerCase())))
        {
            return new HealthCheckResult("adminTools.dashboard.healthcheck.database.info",
                HealthCheckResultLevel.INFO);
        }
        logger.error("Used database is not supported!");
        return new HealthCheckResult("adminTools.dashboard.healthcheck.database.notSupported",
            HealthCheckResultLevel.ERROR, usedDatabase);
    }
}
