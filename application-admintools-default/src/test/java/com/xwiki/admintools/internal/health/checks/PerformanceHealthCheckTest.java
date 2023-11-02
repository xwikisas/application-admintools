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
package com.xwiki.admintools.internal.health.checks;

import java.io.File;
import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.sun.management.OperatingSystemMXBean;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class PerformanceHealthCheckTest
{
    @MockComponent
    private static ContextualLocalizationManager localization;

    @XWikiTempDir
    private File tmpDir;

    private File logsDir;

    @InjectMockComponents
    private PerformanceHealthCheck performanceHealthCheck;

    @Mock
    private Logger logger;

    @Mock
    private OperatingSystemMXBean osMXBean;

    @Mock
    ManagementFactory managementFactory;

    @BeforeComponent
    static void setUp() throws Exception
    {
        when(localization.getTranslationPlain("adminTools.dashboard.section.healthcheck.performance.info")).thenReturn(
            "System performance OK");
    }

    @BeforeEach
    void beforeEach()
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(performanceHealthCheck, "logger", this.logger);

        logsDir = new File("/");
    }

    @Test
    void check()
    {
        assertNull(performanceHealthCheck.check().getErrorMessage());
        verify(logger).info("System performance OK");
    }
}
