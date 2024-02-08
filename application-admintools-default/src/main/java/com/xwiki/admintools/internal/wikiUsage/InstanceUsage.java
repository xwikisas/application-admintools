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
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Access info about the size of the existing wikis.
 *
 * @version $Id$
 */
@Component(roles = InstanceUsage.class)
@Singleton
public class InstanceUsage
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    @Inject
    private UsageDataProvider usageDataProvider;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

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
            List<WikiSizeResult> instanceUsage = getWikisSize();
            scriptContext.setAttribute("instanceUsage", instanceUsage, ScriptContext.ENGINE_SCOPE);
            int extensionCount = usageDataProvider.getExtensionCount();
            scriptContext.setAttribute("extensionCount", extensionCount, ScriptContext.ENGINE_SCOPE);
            long totalUsers = usageDataProvider.getInstanceUsersCount();
            scriptContext.setAttribute("totalUsers", totalUsers, ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render(TEMPLATE_NAME);
        } catch (Exception e) {
            this.logger.warn("Failed to render [{}] template. Root cause is: [{}]", TEMPLATE_NAME,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Retrieves in descending order the documents that have more than a given number of comments.
     *
     * @param maxComment maximum number of comments below which the document is ignored.
     * @return a {@link List} with the documents that have more than the given number of comments.
     * @throws QueryException if the query to retrieve the document fails.
     * @throws XWikiException if a document is not found.
     */
    public List<XWikiDocument> getDocumentsOverGivenNumberOfComments(int maxComment)
        throws QueryException, XWikiException
    {
        List<String> documentsWithComments = this.queryManager.createQuery(
            "select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' group by obj.name",
            Query.XWQL).setWiki(wikiDescriptorManager.getCurrentWikiId()).execute();

        List<XWikiDocument> documentsOverMaxComments = new ArrayList<>();
        for (String documentName : documentsWithComments) {
            XWikiContext wikiContext = wikiContextProvider.get();
            XWiki wiki = wikiContext.getWiki();
            XWikiDocument document = wiki.getDocument(resolver.resolve(documentName), wikiContext);
            int numberOfComments = document.getComments().size();
            if (numberOfComments > maxComment) {
                documentsOverMaxComments.add(document);
            }
        }
        documentsOverMaxComments.sort((d1, d2) -> Integer.compare(d2.getComments().size(), d1.getComments().size()));
        return documentsOverMaxComments;
    }

    private List<WikiSizeResult> getWikisSize()
    {
        List<WikiSizeResult> result = new ArrayList<>();
        try {
            Collection<WikiDescriptor> wikisDescriptors = this.wikiDescriptorManager.getAll();
            for (WikiDescriptor wikiDescriptor : wikisDescriptors) {
                result.add(usageDataProvider.getWikiSize(wikiDescriptor));
            }
            return result;
        } catch (Exception e) {
            logger.warn("There have been issues while gathering instance usage data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return new ArrayList<>();
        }
    }
}
