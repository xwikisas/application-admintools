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
package com.xwiki.admintools.internal.health.checks.security;

import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.data.SecurityDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class LangEncodingHealthCheckTest
{
    @InjectMockComponents
    private LangEncodingHealthCheck langEncodingHealthCheck;

    @MockComponent
    @Named(SecurityDataProvider.HINT)
    private DataProvider dataProvider;

    @Mock
    private Logger logger;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(langEncodingHealthCheck, "logger", this.logger);

        when(dataProvider.getIdentifier()).thenReturn(SecurityDataProvider.HINT);
        Map<String, String> jsonResponse = Map.of("LANG", "en.UTF8");
        when(dataProvider.getDataAsJSON()).thenReturn(jsonResponse);
    }

    @Test
    void check()
    {
        assertEquals("adminTools.dashboard.healthcheck.security.system.lang.info",
            langEncodingHealthCheck.check().getMessage());
    }

    @Test
    void checkNullJson() throws Exception
    {
        when(dataProvider.getDataAsJSON()).thenThrow(new Exception("error while generating the json"));

        assertEquals("adminTools.dashboard.healthcheck.security.system.lang.notFound",
            langEncodingHealthCheck.check().getMessage());
        logger.warn("Language encoding could not be detected!");
    }

    @Test
    void checkUnsafeEncoding() throws Exception
    {
        Map<String, String> jsonResponse = Map.of("LANG", "en.ISO-8859-1");
        when(dataProvider.getDataAsJSON()).thenReturn(jsonResponse);
        assertEquals("adminTools.dashboard.healthcheck.security.system.lang.warn",
            langEncodingHealthCheck.check().getMessage());
        verify(logger).warn("System language encoding is [{}], but should be UTF-8!", "ISO-8859-1");
    }
}
