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

import java.util.ArrayList;
import java.util.HashMap;

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
class EmptyPagesSolrEntityMetadataExtractorTest
{
    @InjectMockComponents
    private EmptyPagesSolrEntityMetadataExtractor emptyPagesSolrEntityMetadataExtractor;

    @Mock
    private XWikiDocument entity;

    @Mock
    private XWikiDocument entity2;

    @Mock
    private SolrInputDocument solrDocument;

    @Mock
    private SolrInputDocument solrDocument2;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    void beforeEach()
    {
        when(entity.getXObjects()).thenReturn(new HashMap<>());
        when(entity.getAttachmentList()).thenReturn(new ArrayList<>());
        when(entity.getContent()).thenReturn("     ");
        when(entity.getXClassXML()).thenReturn("");

        when(entity2.getXObjects()).thenReturn(new HashMap<>());
        when(entity2.getAttachmentList()).thenReturn(new ArrayList<>());
        when(entity2.getContent()).thenReturn("");
        when(entity2.getXClassXML()).thenReturn("this is a class");
    }

    @Test
    void extract()
    {
        emptyPagesSolrEntityMetadataExtractor.extract(entity, solrDocument);
        emptyPagesSolrEntityMetadataExtractor.extract(entity2, solrDocument2);

        verify(solrDocument, Mockito.times(1)).setField("AdminTools.DocumentContentEmpty_boolean", true);
        verify(solrDocument2, Mockito.times(1)).setField("AdminTools.DocumentContentEmpty_boolean", false);
    }

    @Test
    void extractError()
    {
        when(emptyPagesSolrEntityMetadataExtractor.extract(entity, solrDocument)).thenThrow(
            new RuntimeException("extract error"));
        emptyPagesSolrEntityMetadataExtractor.extract(entity, solrDocument);

        assertEquals("Failed to index the right for document [null]", logCapture.getMessage(0));
    }
}
