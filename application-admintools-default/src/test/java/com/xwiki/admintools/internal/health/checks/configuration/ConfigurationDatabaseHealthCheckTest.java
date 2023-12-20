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

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
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
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class ConfigurationDatabaseHealthCheckTest
{
    @MockComponent
    @Named(ConfigurationDataProvider.HINT)
    private static DataProvider configurationDataProvider;

    @InjectMockComponents
    private ConfigurationDatabaseHealthCheck databaseHealthCheck;

    @MockComponent
    private CurrentServer currentServer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent
    static void setUp() throws Exception
    {
        Map<String, String> jsonResponse = Map.of("databaseName", "MYSQL");
        when(configurationDataProvider.getDataAsJSON()).thenReturn(jsonResponse);
    }

    @BeforeEach
    void beforeEach()
    {
        List<String> supportedDatabases = List.of("MySQL", "HSQL");
        when(currentServer.getSupportedDBs()).thenReturn(supportedDatabases);
    }

    @Test
    void check()
    {
        assertEquals("adminTools.dashboard.healthcheck.database.info", databaseHealthCheck.check().getMessage());
    }

    @Test
    void checkNullJSON() throws Exception
    {
        when(configurationDataProvider.getDataAsJSON()).thenThrow(new Exception("error while generating the json"));

        assertEquals("adminTools.dashboard.healthcheck.database.warn", databaseHealthCheck.check().getMessage());
        assertEquals(
            "Failed to generate the instance configuration data. Root cause is: [Exception: error while generating the json]",
            logCapture.getMessage(0));
        assertEquals("Database not found!", logCapture.getMessage(1));
    }

    @Test
    void checkDatabaseNotCompatible() throws Exception
    {
        Map<String, String> jsonResponse = Map.of("databaseName", "NOT_COMPATIBLE");
        when(configurationDataProvider.getDataAsJSON()).thenReturn(jsonResponse);

        assertEquals("adminTools.dashboard.healthcheck.database.notSupported",
            databaseHealthCheck.check().getMessage());
        assertEquals("Used database is not supported!", logCapture.getMessage(0));
    }
}
