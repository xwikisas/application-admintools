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
package com.xwiki.admintools.internal.wikiSize;

import java.util.List;

import javax.inject.Named;
import javax.script.ScriptContext;

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
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ComponentTest
class WikiSizeProviderTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String WIKI_ID = "wikiId";

    @InjectMockComponents
    private WikiSizeProvider wikiSizeProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("count")
    private QueryFilter queryFilter;

    @MockComponent
    private TemplateManager templateManager;

    @Mock
    private WikiDescriptor wikiDescriptor;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private Query usersQueryRes;

    @Mock
    private Query usersQuery;

    @Mock
    private Query docQueryRes;

    @Mock
    private Query docQuery;

    @Mock
    private Query docQueryFilter;

    @Mock
    private Query attSizeQueryRes;

    @Mock
    private Query attSizeQuery;

    @Mock
    private Query attCountQueryFilter;

    @Mock
    private Query attCountQueryRes;

    @Mock
    private Query attCountQuery;

    @Test
    void getWikiSizeInfo() throws Exception
    {
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(queryManager.createQuery("SELECT COUNT(DISTINCT doc.fullName) FROM Document doc, "
            + "doc.object(XWiki.XWikiUsers) AS obj WHERE doc.fullName NOT IN ("
            + "SELECT doc.fullName FROM XWikiDocument doc, BaseObject objLimit, IntegerProperty propActive "
            + "WHERE objLimit.name = doc.fullName AND propActive.id.id = objLimit.id AND propActive.id.name = 'active' "
            + "AND propActive.value = 0)", "xwql")).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID)).thenReturn(usersQueryRes);
        when(usersQueryRes.execute()).thenReturn(List.of(1234L));

        when(queryManager.createQuery("", "xwql")).thenReturn(docQuery);
        when(docQuery.setWiki(WIKI_ID)).thenReturn(docQueryRes);
        when(docQueryRes.addFilter(queryFilter)).thenReturn(docQueryFilter);
        when(docQueryFilter.execute()).thenReturn(List.of(12345L));

        when(queryManager.createQuery(
            "select sum(attach.longSize) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
            "xwql")).thenReturn(attSizeQuery);
        when(attSizeQuery.setWiki(WIKI_ID)).thenReturn(attSizeQueryRes);
        when(attSizeQueryRes.execute()).thenReturn(List.of(123456789L));

        when(queryManager.createQuery(
            "select count(attach) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
            "xwql")).thenReturn(attCountQuery);
        when(attCountQuery.setWiki(WIKI_ID)).thenReturn(attCountQueryRes);
        when(attCountQueryRes.addFilter(queryFilter)).thenReturn(attCountQueryFilter);
        when(attCountQueryFilter.execute()).thenReturn(List.of(123456L));

        when(templateManager.render(TEMPLATE_NAME)).thenReturn("success");

        WikiSizeResult wiki = wikiSizeProvider.getWikiSizeInfo(wikiDescriptor);

        assertEquals(1234L, wiki.getNumberOfUsers());
        assertEquals(12345L, wiki.getNumberOfDocuments());
        assertEquals(123456L, wiki.getNumberOfAttachments());
        assertEquals("117.7 MB", wiki.getAttachmentSize());
    }

    @Test
    void getWikiSizeInfoQueryError() throws Exception
    {
        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));
        when(wikiDescriptor.getId()).thenReturn(WIKI_ID);
        when(wikiDescriptor.getPrettyName()).thenReturn("XWiki Wiki Name");

        when(queryManager.createQuery("SELECT COUNT(DISTINCT doc.fullName) FROM Document doc, "
            + "doc.object(XWiki.XWikiUsers) AS obj WHERE doc.fullName NOT IN ("
            + "SELECT doc.fullName FROM XWikiDocument doc, BaseObject objLimit, IntegerProperty propActive "
            + "WHERE objLimit.name = doc.fullName AND propActive.id.id = objLimit.id AND propActive.id.name = 'active' "
            + "AND propActive.value = 0)", "xwql")).thenReturn(usersQuery);
        when(usersQuery.setWiki(WIKI_ID)).thenReturn(usersQueryRes);
        when(usersQueryRes.execute()).thenThrow(new QueryException("user query error", usersQueryRes, new Exception()));
        assertThrows(QueryException.class, () -> wikiSizeProvider.getWikiSizeInfo(wikiDescriptor));
    }
}
