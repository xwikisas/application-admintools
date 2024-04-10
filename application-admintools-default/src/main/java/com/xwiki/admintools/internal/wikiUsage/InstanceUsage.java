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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
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
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.licensing.Licensor;

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

    private static final String ERROR_TEMPLATE = "licenseError.vm";

    private static final String NAME_KEY = "name";

    private static final String USER_COUNT_KEY = "userCount";

    private static final String ATTACHMENTS_SIZE_KEY = "attachmentsSize";

    private static final String ATTACHMENTS_COUNT_KEY = "attachmentsCount";

    private static final String DOCUMENTS_COUNT_KEY = "documentsCount";

    private static final String INTERVAL_SEPARATOR = "-";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

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

    @Inject
    private Provider<Licensor> licensorProvider;

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

            String wikiName = this.wikiDescriptorManager.getCurrentWikiDescriptor().getPrettyName();
            List<WikiSizeResult> currentWikiUsage = getWikisSize(new HashMap<>(Map.of(NAME_KEY, wikiName)), "", "");
            WikiSizeResult currentWiki = !currentWikiUsage.isEmpty() ? currentWikiUsage.get(0) : null;
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
     * Retrieves in descending order the documents that have more than a given number of comments.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @return a {@link List} with the documents that have more than the given number of comments.
     * @throws QueryException if the query to retrieve the document fails.
     */
    public List<String> getDocumentsOverGivenNumberOfComments(long maxComments) throws QueryException
    {
        return this.queryManager.createQuery(
                "select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' "
                    + "group by obj.name having count(*) > :maxComments order by count(*) desc", Query.HQL)
            .setWiki(wikiDescriptorManager.getCurrentWikiId()).bindValue("maxComments", maxComments).execute();
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
        List<WikiSizeResult> result = new ArrayList<>();
        try {
            Collection<WikiDescriptor> wikisDescriptors = this.wikiDescriptorManager.getAll();
            String filteredName = filters.get(NAME_KEY);
            if (filteredName != null && !filteredName.isEmpty()) {
                wikisDescriptors.removeIf(
                    wiki -> !wiki.getPrettyName().toLowerCase().contains(filteredName.toLowerCase()));
                filters.remove(NAME_KEY);
            }

            for (WikiDescriptor wikiDescriptor : wikisDescriptors) {
                WikiSizeResult wikiSizeResult = usageDataProvider.getWikiSize(wikiDescriptor);
                if (checkFilters(filters, wikiSizeResult)) {
                    result.add(wikiSizeResult);
                }
            }
            applySort(result, sortColumn, order);
            return result;
        } catch (Exception e) {
            logger.warn("There have been issues while gathering instance usage data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return new ArrayList<>();
        }
    }

    private boolean checkFilters(Map<String, String> filters, WikiSizeResult wikiData)
    {
        return filters.entrySet().stream().filter(
            filter -> filter.getValue() != null && !filter.getValue().isEmpty() && !filter.getValue()
                .equals(INTERVAL_SEPARATOR)).allMatch(filter -> {
                    switch (filter.getKey()) {
                        case USER_COUNT_KEY:
                            return wikiData.getUserCount().equals(Long.parseLong(filter.getValue()));
                        case ATTACHMENTS_SIZE_KEY:
                            String[] interval = filter.getValue().split(INTERVAL_SEPARATOR);
                            long attachmentsSize = wikiData.getAttachmentsSize();
                            long lowerBound = Long.parseLong(interval[0]);
                            long upperBound = "x".equals(interval[1]) ? Long.MAX_VALUE : Long.parseLong(interval[1]);
                            return attachmentsSize > lowerBound && attachmentsSize < upperBound;
                        case ATTACHMENTS_COUNT_KEY:
                            return wikiData.getAttachmentsCount().equals(Long.parseLong(filter.getValue()));
                        case DOCUMENTS_COUNT_KEY:
                            return wikiData.getDocumentsCount().equals(Long.parseLong(filter.getValue()));
                        default:
                            throw new IllegalArgumentException("Invalid filter field: " + filter.getKey());
                    }
                });
    }

    private void applySort(List<WikiSizeResult> list, String sort, String order)
    {
        Comparator<WikiSizeResult> comparator = null;
        switch (sort) {
            case NAME_KEY:
                comparator = Comparator.comparing(WikiSizeResult::getName);
                break;
            case USER_COUNT_KEY:
                comparator = Comparator.comparing(WikiSizeResult::getUserCount);
                break;
            case ATTACHMENTS_SIZE_KEY:
                comparator = Comparator.comparing(WikiSizeResult::getAttachmentsSize);
                break;
            case ATTACHMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiSizeResult::getAttachmentsCount);
                break;
            case DOCUMENTS_COUNT_KEY:
                comparator = Comparator.comparing(WikiSizeResult::getDocumentsCount);
                break;
            default:
                break;
        }
        if (comparator != null) {
            if ("desc".equals(order)) {
                comparator = comparator.reversed();
            }
            list.sort(comparator);
        }
    }
}
