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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide data for the documents that are spammed.
 *
 * @version $Id$
 */
@Component(roles = SpamPagesProvider.class)
@Singleton
public class SpamPagesProvider extends AbstractInstanceUsageProvider
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("currentlanguage")
    private QueryFilter currentLanguageFilter;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    @Inject
    @Named("document")
    private QueryFilter documentFilter;

    @Inject
    @Named("viewable")
    private QueryFilter viewableFilter;

    /**
     * Retrieves the documents that have more than a given number of comments.
     *
     * @param searchedWikis {@link Collection} used to identify the searched wikis.
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the documents that have more than the given number of comments.
     */
    public List<XWikiDocument> getDocumentsOverGivenNumberOfComments(Collection<WikiDescriptor> searchedWikis,
        long maxComments, Map<String, String> filters, String sortColumn, String order)
    {
        List<XWikiDocument> spammedDocuments = new ArrayList<>();
        searchedWikis.forEach(wikiDescriptor -> {
            try {
                List<DocumentReference> queryResults =
                    getCommentsForWiki(maxComments, filters.get("docName"), wikiDescriptor.getId());
                for (DocumentReference docRef : queryResults) {
                    XWikiContext xWikiContext = xcontextProvider.get();
                    XWiki xWiki = xWikiContext.getWiki();
                    XWikiDocument wikiDocument = xWiki.getDocument(docRef, xWikiContext);
                    spammedDocuments.add(wikiDocument);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        applySpamSort(spammedDocuments, sortColumn, order);
        return spammedDocuments;
    }

    /**
     * Get the references of documents in wiki with comments above a given limit.
     *
     * @param maxComments maximum number of comments below which the document is ignored.
     * @param searchedDocument document hint to be searched.
     * @param wikiId the wiki for which the data will be retrieved.
     * @return a {@link List} with the {@link DocumentReference} of the documents with comments above a given limit.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public List<DocumentReference> getCommentsForWiki(long maxComments, String searchedDocument, String wikiId)
        throws QueryException
    {
        String searchString;
        if (searchedDocument == null || searchedDocument.isEmpty()) {
            searchString = "%";
        } else {
            searchString = String.format("%%%s%%", searchedDocument);
        }
        return this.queryManager.createQuery("select obj.name from XWikiDocument as doc, BaseObject as obj "
                + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiComments' "
                + "and lower(doc.title) like lower(:searchString) "
                + "group by obj.name having count(*) > :maxComments order by count(*) desc", Query.HQL).setWiki(wikiId)
            .bindValue("maxComments", maxComments).bindValue("searchString", searchString)
            .addFilter(currentLanguageFilter).addFilter(hiddenDocumentFilter).addFilter(documentFilter)
            .addFilter(viewableFilter).execute();
    }
}
