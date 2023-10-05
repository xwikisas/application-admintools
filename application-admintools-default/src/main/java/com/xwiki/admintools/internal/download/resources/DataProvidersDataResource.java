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
package com.xwiki.admintools.internal.download.resources;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.download.DataResource;

/**
 * Merges data from all {@link DataProvider} to be retrieved as a file.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(DataProvidersDataResource.HINT)
@Singleton
public class DataProvidersDataResource implements DataResource
{
    /**
     * Component identifier.
     */
    public static final String HINT = "dataProviderResource";

    @Inject
    private Provider<List<DataProvider>> dataProviders;

    @Inject
    private Logger logger;

    @Override
    public void addZipEntry(ZipOutputStream zipOutputStream, Map<String, String> filters) throws IOException
    {
        if (filters == null) {
            createArchiveEntry(zipOutputStream);
        }
    }

    @Override
    public byte[] getByteData(String input) throws IOException
    {
        Map<String, String> providersResults = new HashMap<>();
        for (DataProvider dataProvider : dataProviders.get()) {
            try {
                providersResults.putAll(dataProvider.getDataAsJSON());
            } catch (Exception e) {
                logger.warn(String.format("Error getting json from DataProvider %s", dataProvider.getIdentifier()),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return providersResults.toString().getBytes();
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    private void createArchiveEntry(ZipOutputStream zipOutputStream) throws IOException
    {
        ZipEntry zipEntry = new ZipEntry("configuration_json.txt");
        zipOutputStream.putNextEntry(zipEntry);
        byte[] buffer = getByteData(null);
        zipOutputStream.write(buffer, 0, buffer.length);
        zipOutputStream.closeEntry();
    }
}
