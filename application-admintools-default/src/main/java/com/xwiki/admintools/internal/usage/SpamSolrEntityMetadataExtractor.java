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

import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.SolrEntityMetadataExtractor;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;

@Component
@Named("spammed-doc")
@Singleton
public class SpamSolrEntityMetadataExtractor implements SolrEntityMetadataExtractor<XWikiDocument>
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<AdminToolsConfiguration> configurationProvider;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Override
    public boolean extract(XWikiDocument entity, SolrInputDocument solrDocument)
    {
        try {
            Vector<BaseObject> results = entity.getObjects("XWiki.XWikiComments");
            if (results != null && results.size() > configurationProvider.get().getSpamSize()) {
                // add spammed field
                solrDocument.addField("spammed", "true");
            } else {
                solrDocument.addField("spammed", "false");
            }
        } catch (Exception e) {
            this.logger.error("Failed to index the right for document [{}]", entity.getDocumentReference(), e);
        }

        return true;
    }
}
