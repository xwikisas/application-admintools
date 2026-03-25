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
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.query.SecureQuery;
import org.xwiki.search.solr.SolrUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.security.RightsResult;

/**
 * Retrieve data about the allowed/denied rights for a certain entity type.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = EntityRightsProvider.class)
@Singleton
public class EntityRightsProvider extends AbstractRightsProvider
{
    private static final String XWIKI = "XWiki";

    private static final String XWIKI_RIGHTS = "XWikiRights";

    private static final String XWIKI_SERVER_PREFIX = "XWikiServer";

    private static final String SPACE_TYPE = "Space";

    private static final String PAGE_TYPE = "Page";

    private static final String GLOBAL_TYPE = "Global";

    private static final String WIKI_KEY = "wikiName";

    private static final LocalDocumentReference GLOBAL_RIGHTS_CLASS =
        new LocalDocumentReference(XWIKI, "XWikiGlobalRights");

    private static final LocalDocumentReference DOCUMENT_RIGHTS_CLASS = new LocalDocumentReference(XWIKI, XWIKI_RIGHTS);

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Inject
    @Named("secure")
    private QueryManager secureQueryManager;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Retrieves a filtered and sorted list of {@link RightsResult} representing the rights for the given parameters.
     *
     * @param filters a map of filters to apply.
     * @param sortColumn the column used for sorting.
     * @param order the sorting order (asc or desc).
     * @param entityType the type of entity for which rights are retrieved.
     * @return a filtered and sorted {@link List} of {@link RightsResult}.
     */
    public List<RightsResult> getEntityRights(Map<String, String> filters, String sortColumn, String order,
        String entityType)
    {
        try {
            String rightsType = filters.get(TYPE_KEY);
            logger.info("Getting the rights for entity [{}] and type [{}].", entityType, rightsType);
            Set<RightsResult> rightsResults = new HashSet<>();
            switch (rightsType == null ? "" : rightsType) {
                // We check the rights set in the wiki administration.
                case GLOBAL_TYPE:
                    addGlobalRights(rightsResults, filters, entityType);
                    break;
                case SPACE_TYPE:
                    addRights(rightsResults, filters, GLOBAL_RIGHTS_CLASS, SPACE_TYPE, entityType);
                    break;
                case PAGE_TYPE:
                    addRights(rightsResults, filters, DOCUMENT_RIGHTS_CLASS, PAGE_TYPE, entityType);
                    break;
                default:
                    addGlobalRights(rightsResults, filters, entityType);
                    addRights(rightsResults, filters, GLOBAL_RIGHTS_CLASS, SPACE_TYPE, entityType);
                    addRights(rightsResults, filters, DOCUMENT_RIGHTS_CLASS, PAGE_TYPE, entityType);
                    break;
            }

            return applySort(rightsResults, sortColumn, order);
        } catch (Exception e) {
            logger.error("There was an error while processing the rights for entity [{}]: [{}]", entityType,
                ExceptionUtils.getRootCauseMessage(e));
            throw new RuntimeException(e);
        }
    }

    private void addGlobalRights(Set<RightsResult> rightsResults, Map<String, String> filters, String entityType)
        throws XWikiException
    {
        String wikiName = filters.get(WIKI_KEY);
        String docName = filters.get(DOCUMENT_KEY);
        String space = filters.get(SPACE_KEY);

        DocumentReference docReference = getGlobalRightsDocumentReference(wikiName);

        boolean docNameMatches = isMatchingIgnoreCase(docName, docReference.getName());
        boolean spaceMatches = isMatchingIgnoreCase(space, docReference.getLastSpaceReference().getName());

        if (docNameMatches && spaceMatches) {
            processDocumentRightsObjects(rightsResults, filters, GLOBAL_TYPE, docReference, GLOBAL_RIGHTS_CLASS,
                entityType);
        }
    }

    /**
     * Get the document that stores the global rights on a given wiki.
     */
    private DocumentReference getGlobalRightsDocumentReference(String wikiName)
    {
        // We check if the given wikiName is not empty, in which case we try to extract the wiki ID and get the
        // "XWiki.XWikiPreferences" from the corresponding wiki ID. Otherwise, we resolve "XWiki.XWikiPreferences"
        // from the current wiki.
        return Optional.ofNullable(wikiName).filter(name -> !name.isEmpty() && !SEPARATOR.equals(name))
            .map(name -> name.replace(XWIKI_SERVER_PREFIX, "").toLowerCase())
            .map(id -> documentReferenceResolver.resolve(String.format("%s:XWiki.XWikiPreferences", id)))
            .orElseGet(() -> documentReferenceResolver.resolve("XWiki.XWikiPreferences"));
    }

    private boolean isMatchingIgnoreCase(String filter, String targetValue)
    {
        return filter == null || filter.isEmpty() || targetValue.toLowerCase().contains(filter.toLowerCase());
    }

    private void addRights(Set<RightsResult> rightsResults, Map<String, String> filters,
        LocalDocumentReference rightsClassReference, String type, String entityType)
        throws QueryException, XWikiException
    {
        SolrDocumentList solrDocuments =
            getRightsForWiki(filters.get(DOCUMENT_KEY), filters.get(SPACE_KEY), filters.get(WIKI_KEY), type,
                rightsClassReference);
        for (SolrDocument solrDocument : solrDocuments) {
            DocumentReference docRef = solrDocumentReferenceResolver.resolve(solrDocument);
            processDocumentRightsObjects(rightsResults, filters, type, docRef, rightsClassReference, entityType);
        }
    }

    private void processDocumentRightsObjects(Set<RightsResult> rightsResults, Map<String, String> filters, String type,
        DocumentReference docReference, LocalDocumentReference rightsClassReference, String entityType)
        throws XWikiException
    {
        XWikiContext wikiContext = xcontextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        XWikiDocument document = wiki.getDocument(docReference, wikiContext);
        logger.info("Getting rights from document [{}] and rights class [{}], for entity [{}].",
            document.getDocumentReference(), rightsClassReference, entityType);
        for (BaseObject object : document.getXObjects(rightsClassReference)) {
            if (object != null) {
                String entity = object.get(entityType).toFormString();
                String entityFilter = filters.get("entity");
                if (entity.isEmpty() || entityFilter != null && !entityFilter.isEmpty() && !wiki.getUserName(entity,
                    null, false, wikiContext).trim().toLowerCase().contains(entityFilter.toLowerCase()))
                {
                    continue;
                }
                RightsResult result = new RightsResult(type);
                result.setSpace(document.getDocumentReference().getLastSpaceReference().getName());
                result.setEntity(documentReferenceResolver.resolve(entity).toString());
                result.setLevel(object.get("levels").toFormString());
                result.setDocReference(document.getDocumentReference());
                result.setPolicy(object.get("allow").toFormString().equals("1") ? "Allowed" : "Denied");
                if (checkFilters(filters, result)) {
                    rightsResults.add(result);
                }
            }
        }
    }

    private SolrDocumentList getRightsForWiki(String searchedDocument, String searchedSpace, String wikiName,
        String type, LocalDocumentReference rightsClass) throws QueryException
    {
        List<String> filterStatements = new ArrayList<>();
        if (searchedDocument != null && !searchedDocument.isEmpty()) {
            filterStatements.add(String.format("title_:%s", solrUtils.toCompleteFilterQueryString(searchedDocument)));
        }
        if (searchedSpace != null && !searchedSpace.isEmpty()) {
            filterStatements.add(String.format("spaces:%s*", solrUtils.toCompleteFilterQueryString(searchedSpace)));
        }
        Query query =
            this.secureQueryManager.createQuery(String.format("type:DOCUMENT AND object:%s", rightsClass), "solr");
        if (query instanceof SecureQuery) {
            ((SecureQuery) query).checkCurrentAuthor(true);
            ((SecureQuery) query).checkCurrentUser(true);
        }
        // The XWikiServer document has a name format of "XWikiServer<wiki ID>". To select the wiki ID, we
        // have to remove the first part of the name and set it to lowercase, as wiki IDs are always in lowercase.
        String searchedWikiID = Optional.ofNullable(wikiName).filter(wiki -> !wiki.isEmpty() && !SEPARATOR.equals(wiki))
            .map(wiki -> wiki.replace(XWIKI_SERVER_PREFIX, "").toLowerCase())
            .orElseGet(() -> xcontextProvider.get().getWikiId());
        filterStatements.add(String.format("wiki:%s", solrUtils.toCompleteFilterQueryString(searchedWikiID)));
        filterStatements.add(type.equals(SPACE_TYPE) ? "-name:XWikiPreferences" : "hidden:false");
        query.bindValue("fl", "title_, reference, wiki, name, spaces");
        query.bindValue("fq", filterStatements);
        query.setLimit(200);
        return ((QueryResponse) query.execute().get(0)).getResults();
    }
}
