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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
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

import com.xwiki.admintools.internal.usage.wikiResult.WikiSizeResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class UsageDataProviderTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String WIKI_ID = "wikiId";

    private static final String WIKI_ID_2 = "wikiId2";

    @InjectMockComponents
    private UsageDataProvider usageDataProvider;

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
    private WikiDescriptor wikiDescriptor2;

    @Mock
    private Query usersQuery;

    @Mock
    private Query usersQuery2;

    @Mock
    private Query docQuery;

    @Mock
    private Query docQuery2;

    @Mock
    private Query attSizeQuery;

    @Mock
    private Query attSizeQuery2;

    @Mock
    private Query attCountQuery;

    @Mock
    private Query attCountQuery2;

    @BeforeEach
    void beforeEach() throws QueryException
    {
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor2.getId()).thenReturn(WIKI_ID_2);

        when(queryManager.createQuery(", BaseObject as obj, IntegerProperty as prop "
            + "where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers' and "
            + "prop.id.id = obj.id and prop.id.name = 'active' and prop.value = '1'", "hql")).thenReturn(usersQuery);
        when(usersQuery.addFilter(countFilter)).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID)).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID_2)).thenReturn(usersQuery2);

        when(queryManager.createQuery("", "xwql")).thenReturn(docQuery);
        when(docQuery.addFilter(countFilter)).thenReturn(docQuery);
        when(docQuery.setWiki(WIKI_ID)).thenReturn(docQuery);
        when(docQuery2.addFilter(countFilter)).thenReturn(docQuery2);
        when(docQuery.setWiki(WIKI_ID_2)).thenReturn(docQuery2);

        when(queryManager.createQuery("select sum(attach.longSize) from XWikiAttachment attach", "xwql")).thenReturn(
            attSizeQuery);
        when(attSizeQuery.setWiki(WIKI_ID)).thenReturn(attSizeQuery);
        when(attSizeQuery.setWiki(WIKI_ID_2)).thenReturn(attSizeQuery2);

        when(queryManager.createQuery("select count(attach) from XWikiAttachment attach", "xwql")).thenReturn(
            attCountQuery);
        when(attCountQuery.setWiki(WIKI_ID)).thenReturn(attCountQuery);
        when(attCountQuery.setWiki(WIKI_ID_2)).thenReturn(attCountQuery2);
    }

    @Test
    void getWikiSizeInfo() throws Exception
    {

        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(usersQuery.execute()).thenReturn(List.of(1234L));
        when(docQuery.execute()).thenReturn(List.of(12345L));
        when(attSizeQuery.execute()).thenReturn(List.of(123456789L));
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
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(usersQuery.execute()).thenThrow(new QueryException("user query error", usersQuery, new Exception()));

        assertThrows(QueryException.class, () -> usageDataProvider.getWikiSize(wikiDescriptor));
    }

    @Test
    void getWikisSize() throws QueryException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);
        when(usersQuery.execute()).thenReturn(List.of(1234L));
        when(docQuery.execute()).thenReturn(List.of(12345L));
        when(attSizeQuery.execute()).thenReturn(List.of(123456789L));
        when(attCountQuery.execute()).thenReturn(List.of(123456L));
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki pretty name");
        Map<String, String> filters =
            new HashMap<>(Map.of("documentsCount", "", "attachmentsCount", "", "totalCount", ""));

        List<WikiSizeResult> wikiSizeResultList = usageDataProvider.getWikisSize(wikiDescriptors, filters, "", "");
        assertEquals(1, wikiSizeResultList.size());
        assertEquals(12345, wikiSizeResultList.get(0).getDocumentsCount());
        assertEquals(123456, wikiSizeResultList.get(0).getAttachmentsCount());
        assertEquals("wiki pretty name", wikiSizeResultList.get(0).getWikiName());
    }

    @Test
    void checkFilters() throws QueryException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);
        wikiDescriptors.add(wikiDescriptor2);
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki name 2");

        when(usersQuery.execute()).thenReturn(List.of(1234L));
        when(docQuery.execute()).thenReturn(List.of(12345L));
        when(attSizeQuery.execute()).thenReturn(List.of(123456789L));
        when(attCountQuery.execute()).thenReturn(List.of(123456L));

        when(usersQuery2.execute()).thenReturn(List.of(123L));
        when(docQuery2.execute()).thenReturn(List.of(1234L));
        when(attSizeQuery2.execute()).thenReturn(List.of(1234567L));
        when(attCountQuery2.execute()).thenReturn(List.of(12345L));

        Map<String, String> filters = new HashMap<>(
            Map.of("userCount", "123", "attachmentsSize", "1234-12345678", "attachmentsCount", "12345",
                "documentsCount", "1234"));

        List<WikiSizeResult> testResults = usageDataProvider.getWikisSize(wikiDescriptors, filters, "", "");
        assertEquals(1, testResults.size());
        assertEquals("wiki name 2", testResults.get(0).getWikiName());
    }

    @Test
    void checkSort() throws QueryException
    {
        Collection<WikiDescriptor> wikiDescriptors = new ArrayList<>();
        wikiDescriptors.add(wikiDescriptor);
        wikiDescriptors.add(wikiDescriptor2);
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki name 2");

        when(usersQuery.execute()).thenReturn(List.of(1234L));
        when(docQuery.execute()).thenReturn(List.of(12345L));
        when(attSizeQuery.execute()).thenReturn(List.of(123456789L));
        when(attCountQuery.execute()).thenReturn(List.of(123456L));

        when(usersQuery2.execute()).thenReturn(List.of(123L));
        when(docQuery2.execute()).thenReturn(List.of(1234L));
        when(attSizeQuery2.execute()).thenReturn(List.of(1234567L));
        when(attCountQuery2.execute()).thenReturn(List.of(12345L));

        Map<String, String> filters = new HashMap<>(
            Map.of("userCount", "", "attachmentsSize", "123456-12345678910", "attachmentsCount", "", "documentsCount",
                ""));

        List<WikiSizeResult> testResults =
            usageDataProvider.getWikisSize(wikiDescriptors, filters, "attachmentsSize", "asc");
        assertEquals(2, testResults.size());
        assertEquals("wiki name 2", testResults.get(0).getWikiName());
        assertEquals("wiki name", testResults.get(1).getWikiName());
    }
}
