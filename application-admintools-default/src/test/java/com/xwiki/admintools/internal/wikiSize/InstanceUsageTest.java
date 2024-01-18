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
import java.util.Vector;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.activeinstalls2.internal.data.ExtensionPing;
import org.xwiki.activeinstalls2.internal.data.UsersPing;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.internal.PingProvider;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class InstanceUsageTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    @InjectMockComponents
    private InstanceUsage instanceUsage;

    @MockComponent
    private PingProvider pingProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private CurrentServer currentServer;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
    private WikiSizeProvider wikiSizeProvider;

    @Mock
    private UsersPing usersPing;

    @Mock
    private WikiSizeResult wikiSizeResult;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @Mock
    private XWikiContext wikiContext;

    @Mock
    private XWiki wiki;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private XWikiDocument wikiDocument;

    @MockComponent
    private XWikiDocument secondWikiDocument;

    @Mock
    private DocumentReference firstDocumentReference;

    @Mock
    private DocumentReference secondDocumentReference;

    @MockComponent
    private QueryManager queryManager;

    @Mock
    private Query docQuery;

    @Mock
    private Query docQueryRes;

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
        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));
        when(wikiSizeProvider.getWikiSizeInfo(wikiDescriptor)).thenReturn(wikiSizeResult);

        when(templateManager.render(TEMPLATE_NAME)).thenReturn("success");

        assertEquals("success", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals(0, logCapture.size());
    }

    @Test
    void renderTemplateCurrentServerNotFound() throws Exception
    {
        when(currentServer.getCurrentServer()).thenReturn(null);
        when(templateManager.render(TEMPLATE_NAME)).thenReturn("fail");
        assertEquals("fail", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", false, ScriptContext.ENGINE_SCOPE);
        assertEquals("Used server not found!", logCapture.getMessage(0));
    }

    @Test
    void renderTemplateGetWikisSizeInfoError() throws Exception
    {
        when(wikiDescriptorManager.getAll()).thenThrow(new WikiManagerException("Failed to get wiki descriptors."));
        when(templateManager.render(TEMPLATE_NAME)).thenReturn("fail");
        assertEquals("fail", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals("There have been issues while gathering info about the size of the Wikis. Root cause is: "
            + "[WikiManagerException: Failed to get wiki descriptors.]", logCapture.getMessage(0));
    }

    @Test
    void renderTemplateError() throws Exception
    {
        when(wikiDescriptorManager.getAll()).thenReturn(List.of(wikiDescriptor));

        when(templateManager.render(TEMPLATE_NAME)).thenThrow(new Exception("Failed to render template."));

        assertNull(instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(eq("wikisInfo"), anyList(), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute("numberOfExtensions", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalNumberOfUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals(
            "Failed to render [" + TEMPLATE_NAME + "] template. Root cause is: [Exception: Failed to render template.]",
            logCapture.getMessage(0));
    }

    @Test
    void getPagesOverGivenNumberOfComments() throws QueryException, XWikiException
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(queryManager.createQuery(
            "select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' group by obj.name",
            "xwql")).thenReturn(docQuery);
        when(docQuery.setWiki("wikiId")).thenReturn(docQueryRes);
        when(docQueryRes.execute()).thenReturn(List.of("Page.one", "Page.two"));
        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWiki()).thenReturn(wiki);
        when(resolver.resolve("Page.one")).thenReturn(firstDocumentReference);
        when(resolver.resolve("Page.two")).thenReturn(secondDocumentReference);
        when(wiki.getDocument(resolver.resolve("Page.one"), wikiContext)).thenReturn(wikiDocument);
        when(wiki.getDocument(resolver.resolve("Page.two"), wikiContext)).thenReturn(secondWikiDocument);
        Vector<BaseObject> vectorOne = new Vector<>();
        Vector<BaseObject> vectorTwo = new Vector<>();
        vectorOne.add(new BaseObject());
        vectorOne.add(new BaseObject());
        vectorOne.add(new BaseObject());
        when(wikiDocument.getComments()).thenReturn(vectorOne);
        when(secondWikiDocument.getComments()).thenReturn(vectorTwo);
        assertEquals(1, instanceUsage.getPagesOverGivenNumberOfComments(2).size());
    }

    @Test
    void getPagesOverGivenNumberOfCommentsError() throws QueryException
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(queryManager.createQuery(
            "select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' group by obj.name",
            "xwql")).thenThrow(new QueryException("ERROR IN QUERY", docQuery, null));
        Exception exception = assertThrows(QueryException.class, () -> {
            this.instanceUsage.getPagesOverGivenNumberOfComments(5);
        });
        assertEquals("ERROR IN QUERY. Query statement = [null]", exception.getMessage());
    }
}
