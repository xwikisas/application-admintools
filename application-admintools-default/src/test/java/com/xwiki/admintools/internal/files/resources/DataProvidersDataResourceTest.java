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
package com.xwiki.admintools.internal.files.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.DataProvider;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DataProvidersDataResource}
 *
 * @version $Id$
 */
@ComponentTest
class DataProvidersDataResourceTest
{
    @InjectMockComponents
    private DataProvidersDataResource dataProviderResource;

    @Mock
    private ZipOutputStream zipOutputStream;

    @MockComponent
    private Provider<List<DataProvider>> dataProviders;

    @MockComponent
    private DataProvider dataProvider;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        List<DataProvider> dataProviderList = new ArrayList<>();
        dataProviderList.add(dataProvider);
        when(dataProviders.get()).thenReturn(dataProviderList);
        when(dataProvider.getIdentifier()).thenReturn("data_provider_identifier");
    }

    @Test
    void getIdentifier()
    {
        assertEquals(DataProvidersDataResource.HINT, dataProviderResource.getIdentifier());
    }

    @Test
    void getByteDataSuccess() throws Exception
    {
        Map<String, String> providerJson = new HashMap<>();
        providerJson.put("success", "true");
        when(dataProvider.getDataAsJSON()).thenReturn(providerJson);

        Map<String, Map<String, String>> providersResults = new HashMap<>();
        providersResults.put("data_provider_identifier", providerJson);
        assertArrayEquals(providersResults.toString().getBytes(), dataProviderResource.getByteData(null));
    }

    @Test
    void getByteDataThrowError() throws Exception
    {
        when(dataProvider.getDataAsJSON()).thenThrow(new Exception("TEST - PROVIDER ERROR AT GET DATA AS JASON!"));
        Exception exception = assertThrows(Exception.class, () -> {
            this.dataProviderResource.getByteData(null);
        });
        assertEquals("Error while getting JSON data for [data_provider_identifier] DataProvider.",
            exception.getMessage());
    }

    @Test
    void addZipEntry() throws Exception
    {
        Map<String, String> providerJson = new HashMap<>();
        providerJson.put("success", "true");
        when(dataProvider.getDataAsJSON()).thenReturn(providerJson);

        Map<String, Map<String, String>> providersResults = new HashMap<>();
        providersResults.put("data_provider_identifier", providerJson);

        byte[] buffer = providersResults.toString().getBytes();
        dataProviderResource.addZipEntry(zipOutputStream, null);
        verify(zipOutputStream).write(buffer, 0, buffer.length);
        verify(zipOutputStream).closeEntry();
    }

    @Test
    void addZipEntryGetByteDataError() throws Exception
    {
        when(dataProvider.getDataAsJSON()).thenThrow(new Exception("ERROR AT GET DATA AS JSON."));
        dataProviderResource.addZipEntry(zipOutputStream, null);

        verify(zipOutputStream, never()).write(any(), eq(0), eq(0));
        assertEquals(
            "Could not add gathered configuration to the archive. Root cause is: [Exception: ERROR AT GET DATA AS JSON.]",
            logCapture.getMessage(0));
    }
}
