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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xwiki.admintools.health.WikiRecycleBinResult;

/**
 * TBC.
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

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    public List<WikiRecycleBinResult> renderWikisRecycleBinTemplate() throws QueryException, WikiManagerException
    {
        Collection<WikiDescriptor> wikiDescriptors = wikiDescriptorManager.getAll();
        List<WikiRecycleBinResult> results = new ArrayList<>();
        for (WikiDescriptor wikiDescriptor : wikiDescriptors) {
            WikiReference wikiReference = new WikiReference(wikiDescriptor.getReference());
            XWiki wiki = wikiContextProvider.get().getWiki();
            String wikiId = wikiDescriptor.getId();
            WikiRecycleBinResult result = new WikiRecycleBinResult();
            result.setWikiName(wikiDescriptor.getPrettyName());
            result.setWikiId(wikiId);
            result.setAttachmentSize(getNumberOfDeletedAttachments(wikiId));
            result.setPageSize(getNumberOfDeletedDocuments(wikiId));
            results.add(result);
        }
        return results;
    }
    // check jira if the admin tools (old) works
    private Long getNumberOfDeletedDocuments(String wikiId) throws QueryException
    {
        return (long) this.queryManager.createQuery("select ddoc from XWikiDeletedDocument as ddoc", Query.XWQL)
            .setWiki(wikiId).execute().size();
    }

    private Long getNumberOfDeletedAttachments(String wikiId) throws QueryException
    {
        return (long) this.queryManager.createQuery("select ddoc from DeletedAttachment as ddoc", Query.XWQL)
            .setWiki(wikiId).execute().size();
    }
}
