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
package com.xwiki.admintools.internal.usage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.internal.PingDataProvider;
import org.xwiki.activeinstalls2.internal.data.DatabasePing;
import org.xwiki.activeinstalls2.internal.data.ExtensionPing;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.activeinstalls2.internal.data.ServletContainerPing;
import org.xwiki.activeinstalls2.internal.data.UsersPing;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.internal.usage.wikiResult.WikiSizeResult;
import com.xwiki.admintools.usage.WikiUsageResult;

/**
 * Retrieves info related to the wiki and instance.
 *
 * @version $Id$
 */
@Component(roles = UsageDataProvider.class)
@Singleton
public class UsageDataProvider extends AbstractInstanceUsageProvider
{
    private static final String METADATA_NAME = "name";

    private static final String METADATA_VERSION = "version";

    @Inject
    @Named("database")
    private PingDataProvider databasePingDataProvider;

    @Inject
    @Named("servlet")
    private PingDataProvider servletPingDataProvider;

    @Inject
    @Named("extensions")
    private PingDataProvider extensionsPingDataProvider;

    @Inject
    @Named("users")
    private PingDataProvider usersPingDataProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    /**
     * Get the database metadata using {@link DatabasePing}.
     *
     * @return a {@link Map} containing the database metadata.
     */
    public Map<String, String> getDatabaseMetadata()
    {
        Ping ping = new Ping();
        databasePingDataProvider.provideData(ping);
        DatabasePing databasePing = ping.getDatabase();
        if (databasePing == null) {
            return new HashMap<>();
        }
        return Map.of(METADATA_NAME, databasePing.getName(), METADATA_VERSION, databasePing.getVersion());
    }

    /**
     * Get the server metadata using {@link ServletContainerPing}.
     *
     * @return a {@link Map} containing the server metadata.
     */
    public Map<String, String> getServerMetadata()
    {
        Ping ping = new Ping();
        servletPingDataProvider.provideData(ping);
        String serverName = ping.getServletContainer().getName();
        String serverVersion = ping.getServletContainer().getVersion();
        return Map.of(METADATA_NAME, serverName, METADATA_VERSION, serverVersion);
    }

    /**
     * Get the number of extensions using {@link ExtensionPing}.
     *
     * @return a count of the current extensions.
     */
    public int getExtensionCount()
    {
        Ping ping = new Ping();
        extensionsPingDataProvider.provideData(ping);
        return ping.getExtensions().size();
    }

    /**
     * Get the number of users in instance using {@link UsersPing}.
     *
     * @return the number of users in the XWiki instance.
     */
    public long getInstanceUsersCount()
    {
        Ping ping = new Ping();
        usersPingDataProvider.provideData(ping);
        return ping.getUsers().getTotal();
    }

    /**
     * Get instance wikis size info, like documents, attachments and users count.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn the column after which to be sorted.
     * @param order the sort oder.
     * @return a sorted and filtered {@link List} of {@link WikiSizeResult} objects containing size info for wikis in
     *     instance.
     */
    public List<WikiSizeResult> getWikisSize(Map<String, String> filters, String sortColumn, String order)
        throws WikiManagerException
    {
        List<WikiUsageResult> results = new ArrayList<>();
        Collection<WikiDescriptor> searchedWikis = getRequestedWikis(filters);

        searchedWikis.forEach(wikiDescriptor -> {
            try {
                WikiSizeResult wikiRecycleBinResult = getWikiSize(wikiDescriptor);
                if (checkFilters(filters, wikiRecycleBinResult)) {
                    results.add(wikiRecycleBinResult);
                }
            } catch (QueryException e) {
                throw new RuntimeException(e);
            }
        });
        applySort(results, sortColumn, order);
        return results.stream().filter(WikiSizeResult.class::isInstance).map(WikiSizeResult.class::cast)
            .collect(Collectors.toList());
    }

    /**
     * Retrieve info about the size of a given wiki.
     *
     * @param wikiDescriptor the wiki for which the data will be retrieved.
     * @return a {@link WikiSizeResult} containing info about the size of the wiki.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public WikiSizeResult getWikiSize(WikiDescriptor wikiDescriptor) throws QueryException
    {
        WikiSizeResult wikiData = new WikiSizeResult();
        String wikiId = wikiDescriptor.getId();
        wikiData.setWikiName(wikiDescriptor.getPrettyName());
        wikiData.setUserCount(getWikiUserCount(wikiId));
        wikiData.setDocumentsCount(getWikiDocumentsCount(wikiId));
        wikiData.setAttachmentsCount(getWikiAttachmentsCount(wikiId));
        wikiData.setAttachmentsSize(getWikiAttachmentSize(wikiId));

        return wikiData;
    }

    private Long getWikiUserCount(String wikiId) throws QueryException
    {
        StringBuilder statement = new StringBuilder(", BaseObject as obj, IntegerProperty as prop ");
        statement.append("where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and ");
        statement.append("prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'");

        List<Long> results =
            this.queryManager.createQuery(statement.toString(), Query.HQL).addFilter(this.countFilter).setWiki(wikiId)
                .execute();
        return results.get(0);
    }

    private long getWikiDocumentsCount(String wikiId) throws QueryException
    {
        List<Long> results =
            this.queryManager.createQuery("", Query.XWQL).setWiki(wikiId).addFilter(this.countFilter).execute();
        return results.get(0);
    }

    private Long getWikiAttachmentSize(String wikiId) throws QueryException
    {
        List<Long> results =
            this.queryManager.createQuery("select sum(attach.longSize) from XWikiAttachment attach", Query.XWQL)
                .setWiki(wikiId).execute();
        return results.get(0);
    }

    private Long getWikiAttachmentsCount(String wikiId) throws QueryException
    {
        List<Long> results =
            this.queryManager.createQuery("select count(attach) from XWikiAttachment attach", Query.XWQL)
                .setWiki(wikiId).execute();
        return results.get(0);
    }
}
