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
package com.xwiki.admintools.internal.health.operations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import com.xwiki.admintools.health.WikiRecycleBinResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ComponentTest
class RecycleBinOperationsTest
{
    @InjectMockComponents
    RecycleBinOperations recycleBinOperations;

    @MockComponent
    WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    QueryManager queryManager;

    @Mock
    Query createQueryAttach;

    @Mock
    Query setWikiQueryAttach;

    @Mock
    Query createQueryDoc;

    @Mock
    Query setWikiQueryDoc;

    @Mock
    WikiDescriptor wikiDescriptor;

    String wikiId1 = "wikiId1";

    @Test
    void getAllWikisRecycleBinInfo() throws WikiManagerException, QueryException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);

        when(wikiDescriptorManager.getAll()).thenReturn(wikiDescriptors);
        when(wikiDescriptor.getId()).thenReturn(wikiId1);
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        when(queryManager.createQuery("select count(ddoc) from DeletedAttachment as ddoc", Query.XWQL)).thenReturn(createQueryAttach);
        when(createQueryAttach.setWiki(wikiId1)).thenReturn(setWikiQueryAttach);
        when(setWikiQueryAttach.execute()).thenReturn(List.of(4L));
        when(queryManager.createQuery("select count(ddoc) from XWikiDeletedDocument as ddoc", Query.XWQL)).thenReturn(createQueryDoc);
        when(createQueryDoc.setWiki(wikiId1)).thenReturn(setWikiQueryDoc);
        when(setWikiQueryDoc.execute()).thenReturn(List.of(23L));
        WikiRecycleBinResult wikiRecycleBinResult = recycleBinOperations.getAllWikisRecycleBinInfo().get(0);
        assertEquals(1, recycleBinOperations.getAllWikisRecycleBinInfo().size());
        assertEquals(23, wikiRecycleBinResult.getPageSize());
        assertEquals(4, wikiRecycleBinResult.getAttachmentSize());
        assertEquals("wiki pretty name", wikiRecycleBinResult.getWikiName());
    }
}
