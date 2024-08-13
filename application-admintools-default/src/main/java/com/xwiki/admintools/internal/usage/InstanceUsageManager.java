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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.usage.wikiResult.WikiRecycleBins;
import com.xwiki.admintools.internal.usage.wikiResult.WikiSizeResult;
import com.xwiki.licensing.Licensor;

/**
 * Access info about the size of the existing wikis.
 *
 * @version $Id$
 */
@Component(roles = InstanceUsageManager.class)
@Singleton
public class InstanceUsageManager
{
    private static final String ERROR_TEMPLATE = "licenseError.vm";

    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String WIKI_NAME_KEY = "wikiName";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    private UsageDataProvider usageDataProvider;

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
    private Provider<Licensor> licensorProvider;

    @Inject
    private SpamPagesProvider spamPagesProvider;

    @Inject
    private EmptyDocumentsProvider emptyDocumentsProvider;

    @Inject
    private RecycleBinsProvider recycleBinsProvider;

    /**
     * Get the data in a format given by the associated template.
     *
     * @return the rendered template as a {@link String}.
     */
    public String renderTemplate()
    {
        try {
            Licensor licensor = licensorProvider.get();
            String wiki = xcontextProvider.get().getWikiId();
            DocumentReference mainRef =
                new DocumentReference(wiki, Arrays.asList("AdminTools", "Code"), "ConfigurationClass");
            if (licensor == null || !licensor.hasLicensure(mainRef)) {
                return this.templateManager.render(ERROR_TEMPLATE);
            }

            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            boolean found = currentServer.getCurrentServer() != null;
            scriptContext.setAttribute("found", found, ScriptContext.ENGINE_SCOPE);
            if (!found) {
                this.logger.error("Used server not found!");
                return this.templateManager.render(TEMPLATE_NAME);
            }

            WikiDescriptor currentWikiDescriptor = this.wikiDescriptorManager.getCurrentWikiDescriptor();
            WikiSizeResult currentWiki = usageDataProvider.getWikiSize(currentWikiDescriptor);
            scriptContext.setAttribute("currentWikiUsage", currentWiki, ScriptContext.ENGINE_SCOPE);

            scriptContext.setAttribute("extensionCount", usageDataProvider.getExtensionCount(),
                ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("totalUsers", usageDataProvider.getInstanceUsersCount(),
                ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render(TEMPLATE_NAME);
        } catch (Exception e) {
            this.logger.warn("Failed to render [{}] template. Root cause is: [{}]", TEMPLATE_NAME,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Get a {@link List} of {@link WikiSizeResult} with the options to sort it and apply filters on it.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a filtered and sorted {@link List} of {@link WikiSizeResult}.
     */
    public List<WikiSizeResult> getWikisSize(Map<String, String> filters, String sortColumn, String order)
    {
        try {
            return usageDataProvider.getWikisSize(filters, sortColumn, order);
        } catch (Exception e) {
            logger.warn("There have been issues while gathering instance usage data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the documents that have more than a given number of comments.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the documents that have more than the given number of comments.
     */
    public List<DocumentReference> getSpammedPages(long maxComments, Map<String, String> filters, String sortColumn,
        String order)
    {
        try {
            return spamPagesProvider.getDocumentsOverGivenNumberOfComments(maxComments, filters, sortColumn, order);
        } catch (Exception e) {
            logger.warn("There have been issues while gathering wikis spammed pages. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves those documents that have no content, {@link XWikiAttachment}, {@link BaseClass}, {@link BaseObject},
     * or comments.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the {@link DocumentReference} of the empty documents.
     */
    public List<DocumentReference> getEmptyDocuments(Map<String, String> filters, String sortColumn, String order)
    {
        try {
            return emptyDocumentsProvider.getEmptyDocuments(filters, sortColumn, order);
        } catch (Exception e) {
            logger.warn("There have been issues while gathering wikis empty pages. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a {@link List} of {@link WikiRecycleBins} with the options to sort it and apply filters on it.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a filtered and sorted {@link List} of {@link WikiRecycleBins}.
     */
    public List<WikiRecycleBins> getWikisRecycleBinsData(Map<String, String> filters, String sortColumn, String order)
    {
        try {
            return recycleBinsProvider.getWikisRecycleBinsSize(filters, sortColumn, order);
        } catch (Exception e) {
            logger.warn("There have been issues while gathering wikis recycle bins data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }
}
