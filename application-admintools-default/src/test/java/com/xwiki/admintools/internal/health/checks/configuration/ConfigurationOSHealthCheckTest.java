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

import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.data.ConfigurationDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class ConfigurationOSHealthCheckTest
{
    @MockComponent
    @Named(ConfigurationDataProvider.HINT)
    private static DataProvider dataProvider;

    @InjectMockComponents
    private ConfigurationOSHealthCheck osHealthCheck;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent
    static void setUp() throws Exception
    {
        Map<String, String> jsonResponse =
            Map.of("osName", "testDBName", "osVersion", "os_version", "osArch", "os_arch");
        when(dataProvider.getDataAsJSON()).thenReturn(jsonResponse);
    }

    @Test
    void check()
    {
        assertEquals("adminTools.dashboard.healthcheck.os.info", osHealthCheck.check().getMessage());
    }

    @Test
    void checkNullJSON() throws Exception
    {
        when(dataProvider.getDataAsJSON()).thenThrow(new Exception("error while generating the json"));

        assertEquals("adminTools.dashboard.healthcheck.os.warn", osHealthCheck.check().getMessage());
        assertEquals(
            "Failed to generate the instance configuration data. Root cause is: [Exception: error while generating the json]",
            logCapture.getMessage(0));
        assertEquals("There has been an error while gathering OS info!", logCapture.getMessage(1));
    }
}
