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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
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

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.admintools.internal.usage.wikiResult.WikiRecycleBins;
import com.xwiki.admintools.internal.usage.wikiResult.WikiSizeResult;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class InstanceUsageManagerTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    private static final String SORT_COLUMN = "sort column";

    private static final String SORT_ORDER = "sort order";

    private final DocumentReference mainRef =
        new DocumentReference("wiki_id", Arrays.asList("AdminTools", "Code"), "ConfigurationClass");

    @InjectMockComponents
    private InstanceUsageManager instanceUsageManager;

    @MockComponent
    private UsageDataProvider usageDataProvider;

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

    @MockComponent
    private SpamPagesProvider spamPagesProvider;

    @MockComponent
    private RecycleBinsProvider recycleBinsProvider;

    @MockComponent
    private EmptyDocumentsProvider emptyDocumentsProvider;

    @Mock
    private ServerInfo serverInfo;

    @Mock
    private WikiDescriptor wikiDescriptor;

    @Mock
    private WikiDescriptor wikiDescriptor2;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private WikiSizeResult wikiSizeResult;

    @Mock
    private WikiRecycleBins wikiRecycleBins;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xWikiContext;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @Mock
    private Licensor licensor;

    @Mock
    private DocumentReference document;

    private Map<String, String> filters = new HashMap<>(Map.of("wikiName", ""));

    @BeforeEach
    void setUp() throws QueryException, WikiManagerException
    {
        when(xcontextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWikiId()).thenReturn("wiki_id");
        when(licensorProvider.get()).thenReturn(licensor);
        when(licensor.hasLicensure(mainRef)).thenReturn(true);

        when(currentServer.getCurrentServer()).thenReturn(serverInfo);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(usageDataProvider.getWikiSize(wikiDescriptor)).thenReturn(wikiSizeResult);
        when(wikiDescriptorManager.getCurrentWikiDescriptor()).thenReturn(wikiDescriptor);

        when(usageDataProvider.getExtensionCount()).thenReturn(2);
        when(usageDataProvider.getInstanceUsersCount()).thenReturn(400L);
    }

    @Test
    void renderTemplate() throws Exception
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");

        when(templateManager.render(TEMPLATE_NAME)).thenReturn("success");

        assertEquals("success", instanceUsageManager.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("currentWikiUsage", wikiSizeResult, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("extensionCount", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals(0, logCapture.size());
    }

    @Test
    void renderTemplateCurrentServerNotFound() throws Exception
    {
        when(currentServer.getCurrentServer()).thenReturn(null);
        when(templateManager.render(TEMPLATE_NAME)).thenReturn("fail");
        assertEquals("fail", instanceUsageManager.renderTemplate());
        verify(scriptContext).setAttribute("found", false, ScriptContext.ENGINE_SCOPE);
        assertEquals("Used server not found!", logCapture.getMessage(0));
    }

    @Test
    void renderTemplateGetWikisSizeInfoError() throws Exception
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");

        when(wikiDescriptorManager.getCurrentWikiDescriptor()).thenThrow(
            new WikiManagerException("Failed to get wiki descriptors."));

        assertNull(this.instanceUsageManager.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext, never()).setAttribute("currentWikiUsage", null, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext, never()).setAttribute("extensionCount", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext, never()).setAttribute("totalUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals("Failed to render [wikiSizeTemplate.vm] template. Root cause is: "
            + "[WikiManagerException: Failed to get wiki descriptors.]", logCapture.getMessage(0));
    }

    @Test
    void renderTemplateError() throws Exception
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");

        when(templateManager.render(TEMPLATE_NAME)).thenThrow(new Exception("Failed to render template."));

        assertNull(instanceUsageManager.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("currentWikiUsage", wikiSizeResult, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("extensionCount", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals(
            "Failed to render [" + TEMPLATE_NAME + "] template. Root cause is: [Exception: Failed to render template.]",
            logCapture.getMessage(0));
    }

    @Test
    void getRenderedDataInvalidLicense() throws Exception
    {
        when(licensor.hasLicensure(mainRef)).thenReturn(false);
        when(templateManager.render("licenseError.vm")).thenReturn("invalid license");
        assertEquals("invalid license", instanceUsageManager.renderTemplate());
    }

    @Test
    void getWikisSize() throws WikiManagerException
    {
        List<WikiSizeResult> docs = List.of(wikiSizeResult);
        when(usageDataProvider.getWikisSize(filters, SORT_COLUMN, SORT_ORDER)).thenReturn(docs);

        assertArrayEquals(docs.toArray(),
            instanceUsageManager.getWikisSize(filters, SORT_COLUMN, SORT_ORDER).toArray());
    }

    @Test
    void getWikisSizeError() throws WikiManagerException
    {
        when(usageDataProvider.getWikisSize(filters, SORT_COLUMN, SORT_ORDER)).thenThrow(
            new RuntimeException("Runtime error"));
        Exception exception = assertThrows(RuntimeException.class,
            () -> instanceUsageManager.getWikisSize(filters, SORT_COLUMN, SORT_ORDER));
        assertEquals("java.lang.RuntimeException: Runtime error", exception.getMessage());
        assertEquals(
            "There have been issues while gathering instance usage data. Root cause is: [RuntimeException: Runtime error]",
            logCapture.getMessage(0));
    }

    @Test
    void getSpammedPages() throws WikiManagerException
    {
        List<DocumentReference> docs = List.of(document);
//        when(spamPagesProvider.getDocumentsOverGivenNumberOfComments(2, filters, SORT_COLUMN, SORT_ORDER)).thenReturn(
//            docs);

//        assertArrayEquals(docs.toArray(),
//            instanceUsageManager.getSpammedPages(2, filters, SORT_COLUMN, SORT_ORDER).toArray());
    }

    @Test
    void getPagesOverGivenNumberOfCommentsError() throws WikiManagerException
    {
//        when(spamPagesProvider.getDocumentsOverGivenNumberOfComments(2, filters, SORT_COLUMN, SORT_ORDER)).thenThrow(
//            new RuntimeException("Runtime error"));
//        Exception exception = assertThrows(RuntimeException.class,
//            () -> instanceUsageManager.getSpammedPages(2, filters, SORT_COLUMN, SORT_ORDER));
//        assertEquals("java.lang.RuntimeException: Runtime error", exception.getMessage());
//        assertEquals(
//            "There have been issues while gathering wikis spammed pages. Root cause is: [RuntimeException: Runtime error]",
//            logCapture.getMessage(0));
    }

    @Test
    void getEmptyPages() throws WikiManagerException
    {
        List<DocumentReference> docs = List.of(document);
        when(emptyDocumentsProvider.getEmptyDocuments(filters, SORT_COLUMN, SORT_ORDER)).thenReturn(docs);

        assertArrayEquals(docs.toArray(),
            instanceUsageManager.getEmptyDocuments(filters, SORT_COLUMN, SORT_ORDER).toArray());
    }

    @Test
    void getEmptyPagesError() throws WikiManagerException
    {
        when(emptyDocumentsProvider.getEmptyDocuments(filters, SORT_COLUMN, SORT_ORDER)).thenThrow(
            new RuntimeException("Runtime error"));
        Exception exception = assertThrows(RuntimeException.class,
            () -> instanceUsageManager.getEmptyDocuments(filters, SORT_COLUMN, SORT_ORDER));
        assertEquals("java.lang.RuntimeException: Runtime error", exception.getMessage());
        assertEquals(
            "There have been issues while gathering wikis empty pages. Root cause is: [RuntimeException: Runtime "
                + "error]", logCapture.getMessage(0));
    }

    @Test
    void getWikisRecycleBinsData() throws WikiManagerException
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name 1");
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki name 2");
        List<WikiRecycleBins> docs = List.of(wikiRecycleBins);
        filters.put("wikiName", "name 2");
        when(recycleBinsProvider.getWikisRecycleBinsSize(filters, SORT_COLUMN, SORT_ORDER)).thenReturn(docs);
        List<WikiRecycleBins> wikiRecycleBinsList =
            instanceUsageManager.getWikisRecycleBinsData(filters, SORT_COLUMN, SORT_ORDER);
        assertEquals(1, wikiRecycleBinsList.size());
        assertEquals(wikiRecycleBins, wikiRecycleBinsList.get(0));
    }

    @Test
    void getWikisRecycleBinsDataError() throws WikiManagerException
    {
        when(recycleBinsProvider.getWikisRecycleBinsSize(filters, SORT_COLUMN, SORT_ORDER)).thenThrow(
            new RuntimeException("Runtime error"));
        Exception exception = assertThrows(RuntimeException.class,
            () -> instanceUsageManager.getWikisRecycleBinsData(filters, SORT_COLUMN, SORT_ORDER));
        assertEquals("java.lang.RuntimeException: Runtime error", exception.getMessage());
        assertEquals(
            "There have been issues while gathering wikis recycle bins data. Root cause is: [RuntimeException: Runtime error]",
            logCapture.getMessage(0));
    }
}
