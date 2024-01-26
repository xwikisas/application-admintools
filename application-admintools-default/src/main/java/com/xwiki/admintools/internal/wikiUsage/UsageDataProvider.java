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
package com.xwiki.admintools.internal.wikiUsage;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;

import com.xwiki.admintools.WikiSizeResult;

/**
 * Retrieves {@link Ping} data from Active Installs 2.
 *
 * @version $Id$
 */
@Component(roles = UsageDataProvider.class)
@Singleton
public class UsageDataProvider implements Initializable
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

    private Ping ping;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    /**
     * Initialize {@link Ping}.
     *
     * @throws InitializationException
     */
    @Override
    public void initialize() throws InitializationException
    {
        this.ping = new Ping();
    }

    /**
     * Get the database metadata using {@link DatabasePing}.
     *
     * @return a {@link Map} containing the database metadata.
     */
    public Map<String, String> getDatabaseMetadata()
    {
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
        usersPingDataProvider.provideData(ping);
        return ping.getUsers().getTotal();
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
        wikiData.setName(wikiDescriptor.getPrettyName());
        wikiData.setUserCount(getWikiUserCount(wikiId));
        wikiData.setDocumentsCount(getWikiDocumentsCount(wikiId));
        wikiData.setAttachmentsCount(getWikiAttachmentsCount(wikiId));
        wikiData.setAttachmentsSize(readableSize(getWikiAttachmentSize(wikiId)));

        return wikiData;
    }

    private Long getWikiUserCount(String wikiId) throws QueryException
    {
        Query query = this.queryManager.createQuery("SELECT COUNT(DISTINCT doc.fullName) FROM Document doc, "
            + "doc.object(XWiki.XWikiUsers) AS obj WHERE doc.fullName NOT IN ("
            + "SELECT doc.fullName FROM XWikiDocument doc, BaseObject objLimit, IntegerProperty propActive "
            + "WHERE objLimit.name = doc.fullName AND propActive.id.id = objLimit.id AND propActive.id.name = 'active' "
            + "AND propActive.value = 0)", Query.XWQL).setWiki(wikiId);
        List<Long> results = query.execute();
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
        List<Long> results = this.queryManager.createQuery(
            "select sum(attach.longSize) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
            Query.XWQL).setWiki(wikiId).execute();
        return results.get(0);
    }

    private Long getWikiAttachmentsCount(String wikiId) throws QueryException
    {
        List<Long> results = this.queryManager.createQuery(
                "select count(attach) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
                Query.XWQL).setWiki(wikiId).addFilter(this.countFilter).execute();
        return results.get(0);
    }

    private String readableSize(Long number)
    {
        if (number == null || number <= 0) {
            return "0";
        }
        List<String> units = List.of("B", "KB", "MB", "GB");

        int digitGroup = (int) (Math.log10(number) / Math.log10(1024));
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        String resultedSize = decimalFormat.format(number / Math.pow(1024, digitGroup));
        return String.format("%s %s", resultedSize, units.get(digitGroup));
    }
}
