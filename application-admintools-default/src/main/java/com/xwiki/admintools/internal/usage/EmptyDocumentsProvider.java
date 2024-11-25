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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Retrieve data about wikis empty pages.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = EmptyDocumentsProvider.class)
@Singleton
public class EmptyDocumentsProvider extends AbstractInstanceUsageProvider
{
    @Inject
    private QueryManager queryManager;

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
     * Retrieves those documents that have no content, {@link XWikiAttachment}, {@link BaseClass}, {@link BaseObject},
     * or comments.
     *
     * @param filters {@link Map} of filters to be applied on the gathered list.
     * @param sortColumn target column to apply the sort on.
     * @param order the order of the sort.
     * @return a {@link List} with the {@link DocumentReference} of the empty documents.
     */
    public List<DocumentReference> getEmptyDocuments(Map<String, String> filters, String sortColumn, String order)
        throws WikiManagerException
    {
        Collection<WikiDescriptor> searchedWikis = getRequestedWikis(filters);
        List<DocumentReference> emptyDocuments = new ArrayList<>();
        XWikiContext wikiContext = xcontextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        searchedWikis.forEach(wikiDescriptor -> {
            try {
                List<DocumentReference> queryResults = getEmptyDocumentsForWiki(wikiDescriptor.getId());
                for (DocumentReference docRef : queryResults) {
                    XWikiDocument wikiDocument = wiki.getDocument(docRef, wikiContext);
                    if (wikiDocument.getXClassXML().isEmpty() && !wikiDocument.isHidden()) {
                        emptyDocuments.add(docRef);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        applyDocumentsSort(emptyDocuments, sortColumn, order);
        return emptyDocuments;
    }

    /**
     * Get the {@link DocumentReference} of empty documents in wiki.
     *
     * @param wikiId the wiki for which the data will be retrieved.
     * @return a {@link List} with the {@link DocumentReference} of the empty documents.
     * @throws QueryException if there are any exceptions while running the queries for data retrieval.
     */
    public List<DocumentReference> getEmptyDocumentsForWiki(String wikiId) throws QueryException
    {
        return this.queryManager.createQuery(
                "select doc.fullName from XWikiDocument doc "
                    + "where (doc.content = '' or trim(doc.content) = '') "
                    + "and not exists (select obj from BaseObject obj where obj.name = doc.fullName) "
                    + "and not exists (select att from XWikiAttachment att where att.docId = doc.id)", Query.HQL)
            .setWiki(wikiId)
            .addFilter(hiddenDocumentFilter)
            .addFilter(documentFilter)
            .addFilter(viewableFilter)
            .execute();
    }
}
