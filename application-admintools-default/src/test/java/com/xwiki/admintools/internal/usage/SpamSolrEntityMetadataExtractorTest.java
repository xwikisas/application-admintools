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

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class SpamSolrEntityMetadataExtractorTest
{
    private static final EntityReference COMMENTSCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiComments");

    @InjectMockComponents
    private SpamSolrEntityMetadataExtractor spamSolrEntityMetadataExtractor;

    @Mock
    private XWikiDocument entity;

    @Mock
    private SolrInputDocument solrDocument;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @Mock
    private BaseObject baseObject;

    @BeforeEach
    void beforeEach()
    {
        List<BaseObject> results = List.of(baseObject);
        when(entity.getXObjects(COMMENTSCLASS_REFERENCE)).thenReturn(results);
    }

    @Test
    void extract()
    {
        spamSolrEntityMetadataExtractor.extract(entity, solrDocument);

        verify(solrDocument, Mockito.times(1)).setField("AdminTools.NumberOfComments_sortInt", 1);
    }

    @Test
    void extractError()
    {
        when(spamSolrEntityMetadataExtractor.extract(entity, solrDocument)).thenThrow(
            new RuntimeException("extract error"));
        spamSolrEntityMetadataExtractor.extract(entity, solrDocument);

        assertEquals("Failed to index the right for document [null]", logCapture.getMessage(0));
    }
}
