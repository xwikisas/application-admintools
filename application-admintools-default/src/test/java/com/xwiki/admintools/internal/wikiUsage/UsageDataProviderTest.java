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
package com.xwiki.admintools.internal.wikiUsage;

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.WikiSizeResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class UsageDataProviderTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String WIKI_ID = "wikiId";

    @InjectMockComponents
    private UsageDataProvider usageDataProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("count")
    private QueryFilter countFilter;

    @MockComponent
    private TemplateManager templateManager;

    @Mock
    private WikiDescriptor wikiDescriptor;

    @Mock
    private Query usersQuery;

    @Mock
    private Query docQuery;

    @Mock
    private Query attSizeQuery;

    @Mock
    private Query attCountQuery;

    @Test
    void getWikiSizeInfo() throws Exception
    {
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(queryManager.createQuery(", BaseObject as obj, IntegerProperty as prop "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and "
            + "prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'", "hql")).thenReturn(usersQuery);
        when(usersQuery.addFilter(countFilter)).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID)).thenReturn(usersQuery);
        when(usersQuery.execute()).thenReturn(List.of(1234L));

        when(queryManager.createQuery("", "xwql")).thenReturn(docQuery);
        when(docQuery.setWiki(WIKI_ID)).thenReturn(docQuery);
        when(docQuery.addFilter(countFilter)).thenReturn(docQuery);
        when(docQuery.execute()).thenReturn(List.of(12345L));

        when(queryManager.createQuery("select sum(attach.longSize) from XWikiAttachment attach", "xwql")).thenReturn(
            attSizeQuery);
        when(attSizeQuery.setWiki(WIKI_ID)).thenReturn(attSizeQuery);
        when(attSizeQuery.execute()).thenReturn(List.of(123456789L));

        when(queryManager.createQuery("select count(attach) from XWikiAttachment attach", "xwql")).thenReturn(
            attCountQuery);
        when(attCountQuery.setWiki(WIKI_ID)).thenReturn(attCountQuery);
        when(attCountQuery.execute()).thenReturn(List.of(123456L));

        when(templateManager.render(TEMPLATE_NAME)).thenReturn("success");

        WikiSizeResult wiki = usageDataProvider.getWikiSize(wikiDescriptor);

        assertEquals(1234L, wiki.getUserCount());
        assertEquals(12345L, wiki.getDocumentsCount());
        assertEquals(123456L, wiki.getAttachmentsCount());
        assertEquals("117.7 MB", wiki.getReadableAttachmentSize());
    }

    @Test
    void getWikiSizeInfoQueryError() throws Exception
    {
        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(queryManager.createQuery(", BaseObject as obj, IntegerProperty as prop "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and "
            + "prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'", "hql")).thenReturn(usersQuery);
        when(usersQuery.addFilter(countFilter)).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID)).thenReturn(usersQuery);
        when(usersQuery.execute()).thenThrow(new QueryException("user query error", usersQuery, new Exception()));

        assertThrows(QueryException.class, () -> usageDataProvider.getWikiSize(wikiDescriptor));
    }
}
