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
package com.xwiki.admintools.internal.wikiSize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.internal.PingProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Access info about the size of the existing Wikis.
 *
 * @version $Id$
 */
@Component(roles = InstanceUsage.class)
@Singleton
public class InstanceUsage
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    private PingProvider pingProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Inject
    private CurrentServer currentServer;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private WikiSizeProvider wikiSizeProvider;

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

    /**
     * Retrieve the pages that have more than a given number of comments.
     *
     * @param maxComment maximum number of comments below which the page is ignored.
     * @return a {@link List} with the documents that have more than the given number of comments.
     * @throws QueryException if the query to retrieve the document fails.
     * @throws XWikiException if a document is not found.
     */
    public List<XWikiDocument> getPagesOverGivenNumberOfComments(int maxComment) throws QueryException, XWikiException
    {
        List<String> pagesWithComments = this.queryManager.createQuery(
            "select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' group by obj.name",
            Query.XWQL).setWiki(wikiDescriptorManager.getCurrentWikiId()).execute();

        List<XWikiDocument> pagesOverMaxComments = new ArrayList<>();
        for (String pageName : pagesWithComments) {
            XWikiContext wikiContext = wikiContextProvider.get();
            XWiki wiki = wikiContext.getWiki();
            XWikiDocument document = wiki.getDocument(resolver.resolve(pageName), wikiContext);
            int numberOfComments = document.getComments().size();
            if (numberOfComments > maxComment) {
                pagesOverMaxComments.add(document);
            }
        }
        pagesOverMaxComments.sort((t1, t2) -> Integer.compare(t2.getComments().size(), t1.getComments().size()));
        return pagesOverMaxComments;
    }

    private List<WikiSizeResult> getWikisSizeInfo()
    {
        List<WikiSizeResult> result = new ArrayList<>();
        try {
            Collection<WikiDescriptor> wikisDescriptors = this.wikiDescriptorManager.getAll();
            for (WikiDescriptor wikiDescriptor : wikisDescriptors) {
                result.add(wikiSizeProvider.getWikiSizeInfo(wikiDescriptor));
            }
            return result;
        } catch (Exception e) {
            logger.warn("There have been issues while gathering info about the size of the Wikis. Root cause is: [{}]",
                org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage(e));
            return new ArrayList<>();
        }
    }
}
