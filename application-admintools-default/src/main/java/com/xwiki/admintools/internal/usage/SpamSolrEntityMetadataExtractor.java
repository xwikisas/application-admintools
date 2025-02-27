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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This extractor retrieves all comment objects associated with the given XWiki document
 * and stores their count in the Solr index under the field "AdminTools.NumberOfComments_sortInt".
 *
 * @version $Id$
 * @since 1.0.2
 */
@Component
@Named("spammed-doc")
@Singleton
public class SpamSolrEntityMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {
        try {
            List<BaseObject> results = entity.getXObjects(documentReferenceResolver.resolve("XWiki.XWikiComments",
                entity.getDocumentReference().getWikiReference()));
            solrDocument.setField("AdminTools.NumberOfComments_sortInt", results.size());
        } catch (Exception e) {
            this.logger.error("Failed to index the right for document [{}]", entity.getDocumentReference(), e);
        }

        return true;
    }
}
