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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xwiki.admintools.WikiSizeResult;

@Component(roles = InstanceUsage.class)
@Singleton
public class InstanceUsage
{
    @Inject
    private PingProvider pingProvider;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    public List<WikiSizeResult> getGroupNumberOfMembers() throws Exception
    {
        List<WikiSizeResult> result = new ArrayList<>();
        getWikiGroupsUsers();
        Collection<WikiDescriptor> wikisDescriptors = this.wikiDescriptorManager.getAll();

        for (WikiDescriptor wikiDescriptor : wikisDescriptors) {
            String wikiId = wikiDescriptor.getId();
            WikiSizeResult wiki = new WikiSizeResult();
            wiki.setWikiName(wikiDescriptor.getPrettyName());
            wiki.setNumberOfUsers(getWikiNumberOfUsers(wikiId));
            wiki.setNumberOfDocuments(getDocumentsCountInWiki(wikiId));
            Long attachmentSizeResult = getWikiAttachmentSize(wikiId);
            Long numberOfAttachments = getWikiNumberOfAttachments(wikiId);
            wiki.setNumberOfAttachments(numberOfAttachments);
            wiki.setAttachmentSize(attachmentSizeResult);
            result.add(wiki);
        }
        return result;
    }

    private Map<String, Long> getWikiGroupsUsers() throws XWikiException
    {
        Map<String, Long> groupsMap = new HashMap<>();
        XWikiContext wikiContext = wikiContextProvider.get();
        XWikiGroupService groupService = wikiContext.getWiki().getGroupService(wikiContext);
        List<String> groupNames = (List<String>) groupService.getAllMatchedGroups(null, false, 0, 0, null, wikiContext);
        for (String groupName : groupNames) {
            int numberOfGroupMembers = groupService.countAllMembersNamesForGroup(groupName, wikiContext);
            groupsMap.put(groupName, (long) numberOfGroupMembers);
        }
        return groupsMap;
    }

    private Long getWikiNumberOfUsers(String wikiId) throws WikiManagerException, QueryException
    {
        Query query = this.queryManager.createQuery("SELECT COUNT(DISTINCT doc.fullName) FROM Document doc, "
            + "doc.object(XWiki.XWikiUsers) AS obj WHERE doc.fullName NOT IN ("
            + "SELECT doc.fullName FROM XWikiDocument doc, BaseObject objLimit, IntegerProperty propActive "
            + "WHERE objLimit.name = doc.fullName AND propActive.id.id = objLimit.id AND propActive.id.name = 'active' "
            + "AND propActive.value = 0)", Query.XWQL).setWiki(wikiId);
        List<Long> results = query.execute();
        return results.get(0);
    }

    private long getDocumentsCountInWiki(String wikiId) throws QueryException
    {
        List<Long> results =
            this.queryManager.createQuery("", Query.XWQL).setWiki(wikiId).addFilter(this.countFilter).execute();
        return results.get(0);
    }

    private Long getWikiAttachmentSize(String wikiId) throws QueryException
    {
        List<Long> results = this.queryManager.createQuery(
            "select sum(attach.longSize) " + "from XWikiAttachment attach, XWikiDocument doc "
                + "where attach.docId=doc.id", Query.XWQL).setWiki(wikiId).addFilter(this.countFilter).execute();
        return results.get(0);
    }

    private Long getWikiNumberOfAttachments(String wikiId) throws QueryException
    {
        List<Long> results = this.queryManager.createQuery(
            "select count(attach) " + "from XWikiAttachment attach, XWikiDocument doc " + "where attach.docId=doc.id",
            Query.XWQL).setWiki(wikiId).addFilter(this.countFilter).execute();
        return results.get(0);
    }
}
