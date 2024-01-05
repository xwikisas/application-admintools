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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Access info about the size of the Wikis inside the XWiki instance.
 *
 * @version $Id$
 */
@Component(roles = InstanceUsage.class)
@Singleton
public class InstanceUsage
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    @Inject
    private PingProvider pingProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * Get the data in a format given by the associated template.
     *
     * @return the rendered template as a {@link String}.
     */
    public String renderTemplate()
    {
        try {
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            boolean found = currentServer.getCurrentServer() != null;
            scriptContext.setAttribute("found", found, ScriptContext.ENGINE_SCOPE);
            if (!found) {
                this.logger.error("Used server not found!");
                return this.templateManager.render(TEMPLATE_NAME);
            }
            List<WikiSizeResult> wikisInfo = getWikisSizeInfo();
            scriptContext.setAttribute("wikisInfo", wikisInfo, ScriptContext.ENGINE_SCOPE);
            int numberOfExtensions = pingProvider.getExtensionPing().size();
            scriptContext.setAttribute("numberOfExtensions", numberOfExtensions, ScriptContext.ENGINE_SCOPE);
            long totalNumberOfUsers = pingProvider.getUsersPing().getTotal();
            scriptContext.setAttribute("totalNumberOfUsers", totalNumberOfUsers, ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render(TEMPLATE_NAME);
        } catch (Exception e) {
            this.logger.warn("Failed to render [{}] template. Root cause is: [{}]", TEMPLATE_NAME,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private List<WikiSizeResult> getWikisSizeInfo()
    {
        List<WikiSizeResult> result = new ArrayList<>();
        try {
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
                wiki.setAttachmentSize(readableSize(attachmentSizeResult));
                result.add(wiki);
            }
            return result;
        } catch (Exception e) {
            logger.warn("There have been issues while gathering info about the size of the Wikis. Root cause is: [{}]",
                org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage(e));
            return new ArrayList<>();
        }
    }

    private Long getWikiNumberOfUsers(String wikiId) throws QueryException
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
            "select sum(attach.longSize) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
            Query.XWQL).setWiki(wikiId).execute();
        return results.get(0);
    }

    private Long getWikiNumberOfAttachments(String wikiId) throws QueryException
    {
        List<Long> results = this.queryManager.createQuery(
                "select count(attach) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
                Query.XWQL)
            .setWiki(wikiId).addFilter(this.countFilter).execute();
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
