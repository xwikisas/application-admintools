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
package com.xwiki.admintools.internal.usage.metadataExtractor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This extractor retrieves all objects and content from a XWiki document and if it's empty, it stores a flag with the
 * value {@code true}, or {@code false} otherwise.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("empty-doc")
@Singleton
public class EmptyPagesSolrEntityMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private Logger logger;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {
        try {
            boolean isEmpty =
                entity.getXObjects().isEmpty() && entity.getAttachmentList().isEmpty() && entity.getContent().trim()
                    .isEmpty() && entity.getXClassXML().trim().isEmpty();
            solrDocument.setField("AdminTools.DocumentContentEmpty_boolean", isEmpty);
        } catch (Exception e) {
            this.logger.error("Failed to index the right for document [{}]", entity.getDocumentReference(), e);
        }

        return true;
    }
}
