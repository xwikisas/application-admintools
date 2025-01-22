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
package com.xwiki.admintools.internal.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xwiki.admintools.security.RightsResult;

@Component(roles = GroupsRightsProvider.class)
@Singleton
public class GroupsRightsProvider extends AbstractRightsProvider
{
    private static final String GLOBAL_RIGHTS_CLASS = "XWiki.XWikiGlobalRights";

    private static final String DOCUMENT_RIGHTS_CLASS = "XWiki.XWikiRights";

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentFilter;

    /**
     * Retrieves those documents that have no content, {@link XWikiAttachment}, {@link BaseClass}, {@link BaseObject},
     * or comments.
     *
     * @return a {@link List} with the {@link DocumentReference} of the empty documents.
     */
    public List<RightsResult> getGroupsRights(Map<String, String> filters, String sortColumn, String order,
        String entityType)
    {
        try {
            List<RightsResult> groupsRightsList = new ArrayList<>();
            switch (filters.get("type") == null ? "" : filters.get("type")) {
                case "Global":
                    addGlobalRights(groupsRightsList, filters, entityType);
                    break;
                case "Space":
                    addRights(groupsRightsList, filters, GLOBAL_RIGHTS_CLASS, "Space", "space", entityType);
                    break;
                case "Page":
                    addRights(groupsRightsList, filters, DOCUMENT_RIGHTS_CLASS, "Page", "fullName", entityType);
                    break;
                default:
                    addGlobalRights(groupsRightsList, filters, entityType);
                    addRights(groupsRightsList, filters, GLOBAL_RIGHTS_CLASS, "Space", "space", entityType);
                    addRights(groupsRightsList, filters, DOCUMENT_RIGHTS_CLASS, "Page", "fullName", entityType);
                    break;
            }
            applySort(groupsRightsList, sortColumn, order);
            return groupsRightsList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void addGlobalRights(List<RightsResult> groupsRightsList, Map<String, String> filters, String entityType)
        throws XWikiException
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference docReference = documentReferenceResolver.resolve("XWiki.XWikiPreferences");
        XWikiDocument document = wiki.getDocument(docReference, wikiContext);
        DocumentReference rightsClassRef = documentReferenceResolver.resolve(GLOBAL_RIGHTS_CLASS);

        processDocumentRightsObjects(groupsRightsList, filters, "Global", document, rightsClassRef, entityType);
    }

    private void addRights(List<RightsResult> groupsRightsList, Map<String, String> filters,
        String rightsClassReference, String type, String target, String entityType)
        throws WikiManagerException, QueryException, XWikiException
    {
        List<String> documentsReference =
            getRightsForWiki(filters.get("docName"), filters.get("space"), target, rightsClassReference);
        processRightsResults(groupsRightsList, filters, rightsClassReference, documentsReference, type, entityType);
    }

    private void processRightsResults(List<RightsResult> groupsRightsList, Map<String, String> filters,
        String documentReferenceRepresentation, List<String> documentsReference, String type, String entityType)
        throws XWikiException
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        DocumentReference rightsClassRef = documentReferenceResolver.resolve(documentReferenceRepresentation);
        for (String docRefString : documentsReference) {
            DocumentReference docRef = documentReferenceResolver.resolve(docRefString);
            XWikiDocument document = wiki.getDocument(docRef, wikiContext);
            processDocumentRightsObjects(groupsRightsList, filters, type, document, rightsClassRef, entityType);
        }
    }

    private void processDocumentRightsObjects(List<RightsResult> groupsRightsList, Map<String, String> filters,
        String type, XWikiDocument document, DocumentReference rightsClassRef, String entityType) throws XWikiException
    {
        Set<RightsResult> allResults = new HashSet<>();
        for (BaseObject object : document.getXObjects(rightsClassRef)) {
            if (object != null) {
                String entity = object.get(entityType).toFormString();
                String groupFilter = filters.get("group");
                if (entity.isEmpty() || groupFilter != null && !groupFilter.isEmpty() && !entity.toLowerCase()
                    .contains(groupFilter.toLowerCase()))
                {
                    continue;
                }
                RightsResult result = new RightsResult(type);
                result.setSpace(document.getDocumentReference().getLastSpaceReference().getName());
                result.setEntity(documentReferenceResolver.resolve(entity).toString());
                result.setLevel(object.get("levels").toFormString());
                result.setDocName(object.getName());
                result.setPolicy(object.get("allow").toFormString().equals("1") ? "Allowed" : "Denied");
                if (checkFilters(filters, result)) {
                    allResults.add(result);
                }
            }
        }
        groupsRightsList.addAll(allResults);
    }

    private List<String> getRightsForWiki(String searchedDocument, String searchedSpace, String target,
        String rightsClass) throws QueryException, WikiManagerException
    {
        String wikiId = this.wikiDescriptorManagerProvider.get().getCurrentWikiDescriptor().getId();
        String searchDocString = "%";
        String searchSpaceString = "%";
        if (searchedDocument != null && !searchedDocument.isEmpty()) {
            searchDocString = String.format("%%%s%%", searchedDocument);
        }
        if (searchedSpace != null && !searchedSpace.isEmpty()) {
            searchSpaceString = String.format("%%%s%%", searchedSpace);
        }
        String query = ("select distinct doc.fullName from XWikiDocument as doc, BaseObject as globalrights "
            + "where doc.fullName = globalrights.name and globalrights.className = :rightsClass "
            + "and lower(doc.title) like lower(:searchDocString) "
            + "and lower(doc.space) like lower(:searchSpaceString) "
            + "and doc.fullName != 'XWiki.XWikiPreferences' order by doc.fullName");

        if (Objects.equals(target, "space")) {
            return this.queryManager.createQuery(query, Query.HQL).setWiki(wikiId)
                .bindValue("searchDocString", searchDocString).bindValue("searchSpaceString", searchSpaceString)
                .bindValue("rightsClass", rightsClass).execute();
        } else {
            return this.queryManager.createQuery(query, Query.HQL).setWiki(wikiId)
                .bindValue("searchDocString", searchDocString).bindValue("searchSpaceString", searchSpaceString)
                .bindValue("rightsClass", rightsClass).addFilter(hiddenDocumentFilter).execute();
        }
    }
}
