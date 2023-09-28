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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;

import com.xwiki.admintools.health.HealthCheckComponent;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;

@Component
@Named(ConfigurationHealthCheckComponent.HINT)
@Singleton
public class ConfigurationHealthCheckComponent implements HealthCheckComponent
{
    public static final String HINT = "CONFIG_HEALTH";

    @Inject
    JobProgressManager jobProgressManager;

    Map<String, String> configurationJson;

    @Inject
    Provider<List<AbstractConfigurationHealthCheck>> configurationHealthChecks;

    @Inject
    ConfigurationDataProvider configurationDataProvider;

    @Override
    public List<HealthCheckResult> check()
    {
        List<HealthCheckResult> results = new ArrayList<>();
        try {
            updateJson();
            for (AbstractConfigurationHealthCheck configurationHealthCheck : configurationHealthChecks.get()) {
                jobProgressManager.startStep(this);
                HealthCheckResult checkResult = configurationHealthCheck.check(configurationJson);
                if (checkResult.getErrorMessage() != null) {
                    results.add(checkResult);
                }
                jobProgressManager.endStep(this);
            }
        } catch (Exception e) {
            results.add(new HealthCheckResult("server_not_found", "documentation_link"));
        }
        return results;
    }

    protected void updateJson() throws Exception
    {
        configurationJson = configurationDataProvider.provideJson();
    }
}
