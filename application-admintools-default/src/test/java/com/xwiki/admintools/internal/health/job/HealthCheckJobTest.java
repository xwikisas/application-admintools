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
package com.xwiki.admintools.internal.health.job;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;
import com.xwiki.admintools.jobs.HealthCheckJobRequest;
import com.xwiki.admintools.jobs.HealthCheckJobStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class HealthCheckJobTest
{
    @InjectMockComponents
    private HealthCheckJob healthCheckJob;

    @MockComponent
    private Provider<List<HealthCheck>> listProvider;

    @Mock
    private HealthCheck firstHealthCheck;

    @Mock
    private HealthCheck secondHealthCheck;

    @Test
    void createNewStatus()
    {
        assertEquals(HealthCheckJobStatus.class,
            healthCheckJob.createNewStatus(new HealthCheckJobRequest()).getClass());
    }

    @Test
    void runInternal()
    {
        List<HealthCheck> healthCheckList = new ArrayList<>();
        healthCheckList.add(firstHealthCheck);
        healthCheckList.add(secondHealthCheck);

        when(listProvider.get()).thenReturn(healthCheckList);
        when(firstHealthCheck.check()).thenReturn(new HealthCheckResult("err", "err_rec"));
        when(secondHealthCheck.check()).thenReturn(new HealthCheckResult());

        healthCheckJob.initialize(new HealthCheckJobRequest());
        healthCheckJob.runInternal();
        HealthCheckJobStatus healthCheckJobStatus = healthCheckJob.getStatus();
        assertEquals(1, healthCheckJobStatus.getHealthCheckResults().size());
    }
}
