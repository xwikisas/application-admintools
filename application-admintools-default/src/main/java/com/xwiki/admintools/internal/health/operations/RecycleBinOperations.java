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
package com.xwiki.admintools.internal.health.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.health.WikiRecycleBinResult;

/**
 * Retrieve data about wikis recycle bins.
 *
 * @version $Id$
 */
@Component(roles = RecycleBinOperations.class)
@Singleton
public class RecycleBinOperations
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    /**
     * Generate a {@link List} of {@link WikiRecycleBinResult} that is populated with results for all existing wikis in
     * instance.
     *
     * @return info about all existing wikis in instance.
     * @throws QueryException when there is an issue regarding the queries that retrieve the number of deleted
     *     documents and attachments.
     * @throws WikiManagerException for any exception while retrieving the {@link Collection} of
     *     {@link WikiDescriptor}.
     */
    public List<WikiRecycleBinResult> getAllWikisRecycleBinInfo() throws QueryException, WikiManagerException
    {
        Collection<WikiDescriptor> wikiDescriptors = wikiDescriptorManager.getAll();
        List<WikiRecycleBinResult> results = new ArrayList<>();
        for (WikiDescriptor wikiDescriptor : wikiDescriptors) {
            String wikiId = wikiDescriptor.getId();
            WikiRecycleBinResult result = new WikiRecycleBinResult();
            result.setWikiName(wikiDescriptor.getPrettyName());
            result.setWikiId(wikiId);
            result.setAttachmentSize(getNumberOfDeletedDocuments(wikiId, "DeletedAttachment"));
            result.setPageSize(getNumberOfDeletedDocuments(wikiId, "XWikiDeletedDocument"));
            results.add(result);
        }
        return results;
    }

    private long getNumberOfDeletedDocuments(String wikiId, String database) throws QueryException
    {
        return (long) this.queryManager.createQuery("select count(ddoc) from " + database + " as ddoc", Query.XWQL)
            .setWiki(wikiId).execute().get(0);
    }
}
