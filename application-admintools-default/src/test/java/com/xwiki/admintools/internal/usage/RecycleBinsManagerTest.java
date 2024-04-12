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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.usage.WikiRecycleBins;
import com.xwiki.admintools.usage.WikiSizeResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class RecycleBinsManagerTest
{
    @InjectMockComponents
    RecycleBinsManager recycleBinsManager;

    @MockComponent
    WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    QueryManager queryManager;

    @Mock
    Query createQueryAttach;

    @Mock
    Query setWikiQueryAttach;

    @Mock
    Query setWikiQueryAttach2;

    @Mock
    Query createQueryDoc;

    @Mock
    Query setWikiQueryDoc;

    @Mock
    Query setWikiQueryDoc2;

    @Mock
    WikiDescriptor wikiDescriptor;

    @Mock
    WikiDescriptor wikiDescriptor2;

    String wikiId1 = "wikiId1";

    String wikiId2 = "wikiId2";

    @BeforeEach
    void beforeEach() throws QueryException
    {
        when(queryManager.createQuery("select count(ddoc) from DeletedAttachment as ddoc", Query.XWQL)).thenReturn(
            createQueryAttach);
        when(createQueryAttach.setWiki(wikiId1)).thenReturn(setWikiQueryAttach);
        when(createQueryAttach.setWiki(wikiId2)).thenReturn(setWikiQueryAttach2);

        when(queryManager.createQuery("select count(ddoc) from XWikiDeletedDocument as ddoc", Query.XWQL)).thenReturn(
            createQueryDoc);
        when(createQueryDoc.setWiki(wikiId1)).thenReturn(setWikiQueryDoc);
        when(createQueryDoc.setWiki(wikiId2)).thenReturn(setWikiQueryDoc2);
        when(wikiDescriptor.getId()).thenReturn(wikiId1);
        when(wikiDescriptor2.getId()).thenReturn(wikiId2);
    }

    @Test
    void getAllWikisRecycleBinInfo() throws WikiManagerException, QueryException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);
        when(wikiDescriptorManager.getAll()).thenReturn(wikiDescriptors);
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");

        when(setWikiQueryAttach.execute()).thenReturn(List.of(4L));
        when(setWikiQueryDoc.execute()).thenReturn(List.of(23L));

        List<WikiRecycleBins> wikisRecycleBins =
            recycleBinsManager.getWikisRecycleBinsSize(new HashMap<>(Map.of("wikiName", "wiki pretty name")), "", "");
        assertEquals(1, wikisRecycleBins.size());
        assertEquals(23, wikisRecycleBins.get(0).getDocumentsCount());
        assertEquals(4, wikisRecycleBins.get(0).getAttachmentsCount());
        assertEquals("wiki pretty name", wikisRecycleBins.get(0).getWikiName());
    }

    @Test
    void checkFilters() throws WikiManagerException, QueryException
    {
        when(wikiDescriptorManager.getAll()).thenReturn(new ArrayList<>(List.of(wikiDescriptor, wikiDescriptor2)));
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 pretty name");

        when(setWikiQueryAttach.execute()).thenReturn(List.of(4L));
        when(setWikiQueryDoc.execute()).thenReturn(List.of(23L));

        when(setWikiQueryAttach2.execute()).thenReturn(List.of(15L));
        when(setWikiQueryDoc2.execute()).thenReturn(List.of(2L));

        Map<String, String> filters =
            new HashMap<>(Map.of("documentsCount", "23", "attachmentsCount", "4", "totalCount", "27"));
        List<WikiRecycleBins> testResults = recycleBinsManager.getWikisRecycleBinsSize(filters, "", "");
        assertEquals(1, testResults.size());
        assertEquals("wiki pretty name", testResults.get(0).getWikiName());
    }

    @Test
    void checkSort() throws WikiManagerException, QueryException
    {
        when(wikiDescriptorManager.getAll()).thenReturn(new ArrayList<>(List.of(wikiDescriptor, wikiDescriptor2)));
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 pretty name");

        when(setWikiQueryAttach.execute()).thenReturn(List.of(4L));
        when(setWikiQueryDoc.execute()).thenReturn(List.of(23L));

        when(setWikiQueryAttach2.execute()).thenReturn(List.of(15L));
        when(setWikiQueryDoc2.execute()).thenReturn(List.of(23L));

        Map<String, String> filters =
            new HashMap<>(Map.of("documentsCount", "23", "attachmentsCount", "", "totalCount", ""));
        List<WikiRecycleBins> testResults = recycleBinsManager.getWikisRecycleBinsSize(filters, "totalCount", "desc");
        assertEquals(2, testResults.size());
        assertEquals("wiki2 pretty name", testResults.get(0).getWikiName());
        assertEquals("wiki pretty name", testResults.get(1).getWikiName());
    }
}
