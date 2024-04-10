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
package com.xwiki.admintools.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.query.QueryException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.health.WikiRecycleBins;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.files.ImportantFilesManager;
import com.xwiki.admintools.internal.wikiUsage.InstanceUsage;
import com.xwiki.admintools.internal.wikiUsage.RecycleBinsManager;

/**
 * Manages the data that needs to be used by the Admin Tools application.
 *
 * @version $Id$
 */
@Component(roles = AdminToolsManager.class)
@Singleton
public class AdminToolsManager
{
    /**
     * A list of all the data providers for Admin Tools.
     */
    @Inject
    private Provider<List<DataProvider>> dataProviderProvider;

    /**
     * Currently used server.
     */
    @Inject
    private CurrentServer currentServer;

    @Inject
    private ImportantFilesManager importantFilesManager;

    @Inject
    private InstanceUsage instanceUsage;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    @Inject
    private RecycleBinsManager recycleBinsManager;

    /**
     * Get data generated in a specific format, using a template, by each provider and merge it.
     *
     * @return a {@link String} containing all templates builds.
     */
    public String generateData()
    {
        StringBuilder strBuilder = new StringBuilder();

        for (DataProvider dataProvider : this.dataProviderProvider.get()) {
            strBuilder.append(dataProvider.getRenderedData());
            strBuilder.append("\n");
        }
        return strBuilder.toString();
    }

    /**
     * Extract a specific data provider template.
     *
     * @param hint {@link String} represents the data provider identifier.
     * @return a {@link String} representing a template
     */
    public String generateData(String hint) throws ComponentLookupException
    {
        DataProvider dataProvider = contextComponentManager.getInstance(DataProvider.class, hint);
        return dataProvider.getRenderedData();
    }

    /**
     * Get supported databases.
     *
     * @return a {@link List} with the supported databases.
     */
    public List<String> getSupportedDBs()
    {
        return this.currentServer.getSupportedDBs();
    }

    /**
     * Get supported servers.
     *
     * @return the servers that are compatible with the application.
     */
    public List<String> getSupportedServers()
    {
        return this.currentServer.getSupportedServers();
    }

    /**
     * Get the rendered template for accessing the downloads UI.
     *
     * @return a {@link String} representation of the template.
     */
    public String getFilesSection()
    {
        return this.importantFilesManager.renderTemplate();
    }

    /**
     * Get the rendered template for viewing info about the size of the XWiki instance.
     *
     * @return a {@link String} representation of the template.
     */
    public String getInstanceSizeTemplate()
    {
        return instanceUsage.renderTemplate();
    }

    /**
     * Retrieve the pages that have more than a given number of comments.
     *
     * @param maxComments maximum number of comments below which the page is ignored.
     * @return a {@link List} with the documents that have more than the given number of comments.
     * @throws QueryException if the query to retrieve the document fails.
     */
    public List<String> getPagesOverGivenNumberOfComments(long maxComments) throws QueryException
    {
        return instanceUsage.getDocumentsOverGivenNumberOfComments(maxComments);
    }

    /**
     * Get instance recycle bins info, like deleted documents and attachment.
     *
     * @return @return a {@link List} of {@link WikiRecycleBins} objects containing recycle bins info for each wiki of
     *     the instance.
     * @throws RuntimeException when there is an issue regarding the queries that retrieve the number of deleted
     *     documents and attachments.
     * @throws WikiManagerException for any exception while retrieving the {@link Collection} of
     *     {@link WikiDescriptor}.
     */
    public List<WikiRecycleBins> getWikisRecycleBinsSize() throws WikiManagerException
    {
        return this.recycleBinsManager.getWikisRecycleBinsSize();
    }

    /**
     * Get a {@link List} of {@link WikiSizeResult} with the options to sort it and apply filters on it.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a filtered and sorted {@link List} of {@link WikiSizeResult}.
     */
    public List<WikiSizeResult> getWikiSizeResults(Map<String, String> filters, String sortColumn, String order)
    {
        return this.instanceUsage.getWikisSize(filters, sortColumn, order);
    }
}
