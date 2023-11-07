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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.data.SecurityDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class SecuritySystemEncodingHealthCheckTest
{
    @MockComponent
    private static Provider<List<DataProvider>> dataProviders;

    @MockComponent
    private static DataProvider firstDataProvider;

    @MockComponent
    private static DataProvider secondDataProvider;

    @MockComponent
    private static ContextualLocalizationManager localization;

    @InjectMockComponents
    private SecuritySystemEncodingHealthCheck systemEncodingHealthCheck;

    @Mock
    private Logger logger;

    @BeforeComponent
    static void setUp() throws Exception
    {
        List<DataProvider> dataProviderList = new ArrayList<>();
        dataProviderList.add(firstDataProvider);
        dataProviderList.add(secondDataProvider);
        when(dataProviders.get()).thenReturn(dataProviderList);

        when(secondDataProvider.getDataAsJSON()).thenThrow(new Exception("DATA PROVIDE ERROR"));

        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.security.system.info")).thenReturn("System encoding OK");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.security.system.lang.warn")).thenReturn(
            "System language encoding is [{}] but should be UTF-8!");
        when(localization.getTranslationPlain(
            "adminTools.dashboard.healthcheck.security.system.file.warn")).thenReturn(
            "System file encoding is [{}] but should be UTF-8!");
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(systemEncodingHealthCheck, "logger", this.logger);

        when(firstDataProvider.getIdentifier()).thenReturn(SecurityDataProvider.HINT);
        when(secondDataProvider.getIdentifier()).thenReturn("second");
        Map<String, String> jsonResponse = Map.of("LANG", "en.UTF8", "fileEncoding", "UTF8");
        when(firstDataProvider.getDataAsJSON()).thenReturn(jsonResponse);
    }

    @Test
    void check()
    {
        assertNull(systemEncodingHealthCheck.check().getErrorMessage());
        verify(logger).info("System encoding OK");
    }

    @Test
    void checkNullJson() throws Exception
    {
        when(firstDataProvider.getDataAsJSON()).thenThrow(new Exception("error while generating the json"));

        assertEquals("xwiki_encoding_err", systemEncodingHealthCheck.check().getErrorMessage());
        verify(logger).warn("System language encoding is [{}] but should be UTF-8!", "invalid");
        verify(logger).warn("System file encoding is [{}] but should be UTF-8!", "invalid");
    }

    @Test
    void checkUnsafeEncoding() throws Exception
    {
        Map<String, String> jsonResponse = Map.of("LANG", "en.UTF8", "fileEncoding", "ISO-8859-1");
        when(firstDataProvider.getDataAsJSON()).thenReturn(jsonResponse);
        assertEquals("xwiki_encoding_err", systemEncodingHealthCheck.check().getErrorMessage());
        verify(logger).warn("System file encoding is [{}] but should be UTF-8!", "ISO-8859-1");
    }
}
