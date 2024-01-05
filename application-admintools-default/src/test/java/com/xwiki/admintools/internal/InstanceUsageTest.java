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
package com.xwiki.admintools.internal;

import java.util.List;

import javax.inject.Named;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.xwiki.activeinstalls2.internal.data.ExtensionPing;
import org.xwiki.activeinstalls2.internal.data.UsersPing;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class InstanceUsageTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String WIKI_ID = "wikiId";

    @InjectMockComponents
    private InstanceUsage instanceUsage;

    @MockComponent
    private PingProvider pingProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private CurrentServer currentServer;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    @Named("count")
    private QueryFilter queryFilter;

    @Mock
    private Logger logger;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Mock
    private ServerIdentifier serverIdentifier;

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

    @Mock
    private UsersPing usersPing;

    @BeforeEach
    void setUp()
    {
        when(currentServer.getCurrentServer()).thenReturn(serverIdentifier);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        when(pingProvider.getExtensionPing()).thenReturn(List.of(new ExtensionPing(), new ExtensionPing()));
        when(pingProvider.getUsersPing()).thenReturn(usersPing);
        when(usersPing.getTotal()).thenReturn(400L);
    }

    @Test
    void renderTemplate() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(instanceUsage, "logger", this.logger);

        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));
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

        assertEquals("success", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        verify(logger, never()).warn(any());
    }

    @Test
    void renderTemplateCurrentServerNotFound() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(instanceUsage, "logger", this.logger);
        when(currentServer.getCurrentServer()).thenReturn(null);
        when(templateManager.render(TEMPLATE_NAME)).thenReturn("fail");
        assertEquals("fail", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", false, ScriptContext.ENGINE_SCOPE);
        verify(logger).error("Used server not found!");
    }

    @Test
    void renderTemplateGetWikisSizeInfoError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(instanceUsage, "logger", this.logger);

        when(wikiDescriptorManager.getAll()).thenThrow(new WikiManagerException("Failed to get wiki descriptors."));
        when(templateManager.render(TEMPLATE_NAME)).thenReturn("fail");
        assertEquals("fail", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        verify(logger).warn(
            "There have been issues while gathering info about the size of the Wikis. Root cause is: " + "[{}]",
            "WikiManagerException: Failed to get wiki descriptors.");
    }

    @Test
    void renderTemplateError() throws Exception
    {
        when(logger.isWarnEnabled()).thenReturn(true);
        ReflectionUtils.setFieldValue(instanceUsage, "logger", this.logger);

        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));
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
        when(attSizeQueryRes.execute()).thenReturn(List.of(-5L));

        when(queryManager.createQuery(
            "select count(attach) from XWikiAttachment attach, XWikiDocument doc where attach.docId=doc.id",
            "xwql")).thenReturn(attCountQuery);
        when(attCountQuery.setWiki(WIKI_ID)).thenReturn(attCountQueryRes);
        when(attCountQueryRes.addFilter(queryFilter)).thenReturn(attCountQueryFilter);
        when(attCountQueryFilter.execute()).thenReturn(List.of(123456L));

        when(templateManager.render(TEMPLATE_NAME)).thenThrow(new Exception("Failed to render template."));

        assertNull(instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        verify(logger).warn("Failed to render [{}] template. Root cause is: [{}]", TEMPLATE_NAME,
            "Exception: Failed to render template.");
    }
}
