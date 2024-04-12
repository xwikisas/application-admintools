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

import java.util.ArrayList;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.ServerInfo;
import com.xwiki.admintools.WikiSizeResult;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class InstanceUsageTest
{
    private static final String TEMPLATE_NAME = "wikiSizeTemplate.vm";

    @InjectMockComponents
    private InstanceUsage instanceUsage;

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
    private WikiSizeResult wikiSizeResult2;

    @MockComponent
    private QueryManager queryManager;

    @Mock
    private Query docQuery;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xWikiContext;

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @Mock
    private Licensor licensor;

    private DocumentReference mainRef =
        new DocumentReference("wiki_id", Arrays.asList("AdminTools", "Code"), "ConfigurationClass");

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
        when(wikiDescriptorManager.getAll()).thenReturn(new ArrayList<>(List.of(wikiDescriptor)));

        when(usageDataProvider.getExtensionCount()).thenReturn(2);
        when(usageDataProvider.getInstanceUsersCount()).thenReturn(400L);
    }

    @Test
    void renderTemplate() throws Exception
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");

        when(templateManager.render(TEMPLATE_NAME)).thenReturn("success");

        assertEquals("success", instanceUsage.renderTemplate());
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
        assertEquals("fail", instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", false, ScriptContext.ENGINE_SCOPE);
        assertEquals("Used server not found!", logCapture.getMessage(0));
    }

    @Test
    void renderTemplateGetWikisSizeInfoError() throws Exception
    {
        when(wikiDescriptor.getPrettyName()).thenReturn("wiki name");

        when(wikiDescriptorManager.getCurrentWikiDescriptor()).thenThrow(
            new WikiManagerException("Failed to get wiki descriptors."));

        assertNull(this.instanceUsage.renderTemplate());
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

        assertNull(instanceUsage.renderTemplate());
        verify(scriptContext).setAttribute("found", true, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("currentWikiUsage", wikiSizeResult, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("extensionCount", 2, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("totalUsers", 400L, ScriptContext.ENGINE_SCOPE);
        assertEquals(
            "Failed to render [" + TEMPLATE_NAME + "] template. Root cause is: [Exception: Failed to render template.]",
            logCapture.getMessage(0));
    }

    @Test
    void getPagesOverGivenNumberOfComments() throws QueryException, XWikiException
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(queryManager.createQuery("select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' "
            + "group by obj.name having count(*) > :maxComments order by count(*) desc", "hql")).thenReturn(docQuery);
        when(docQuery.setWiki("wikiId")).thenReturn(docQuery);
        when(docQuery.bindValue("maxComments", 2L)).thenReturn(docQuery);
        when(docQuery.execute()).thenReturn(List.of("Page.one"));
        assertEquals(1, instanceUsage.getDocumentsOverGivenNumberOfComments(2).size());
    }

    @Test
    void getPagesOverGivenNumberOfCommentsError() throws QueryException
    {
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        when(queryManager.createQuery("select obj.name from BaseObject obj where obj.className='XWiki.XWikiComments' "
            + "group by obj.name having count(*) > :maxComments order by count(*) desc", "hql")).thenThrow(
            new QueryException("ERROR IN QUERY", docQuery, null));
        Exception exception = assertThrows(QueryException.class, () -> {
            this.instanceUsage.getDocumentsOverGivenNumberOfComments(5);
        });
        assertEquals("ERROR IN QUERY. Query statement = [null]", exception.getMessage());
    }

    @Test
    void getRenderedDataInvalidLicense() throws Exception
    {
        when(licensor.hasLicensure(mainRef)).thenReturn(false);
        when(templateManager.render("licenseError.vm")).thenReturn("invalid license");
        assertEquals("invalid license", instanceUsage.renderTemplate());
    }

    @Test
    void checkFilters() throws WikiManagerException, QueryException
    {
        when(wikiDescriptorManager.getAll()).thenReturn(new ArrayList<>(List.of(wikiDescriptor, wikiDescriptor2)));
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 name");
        when(wikiSizeResult.getAttachmentsCount()).thenReturn(12345L);
        when(wikiSizeResult.getName()).thenReturn("wiki name");
        when(wikiSizeResult.getDocumentsCount()).thenReturn(123L);
        when(wikiSizeResult.getUserCount()).thenReturn(12L);
        when(wikiSizeResult.getAttachmentsSize()).thenReturn(123456L);

        when(wikiSizeResult2.getAttachmentsCount()).thenReturn(1234L);
        when(wikiSizeResult2.getName()).thenReturn("wiki2 name");
        when(wikiSizeResult2.getDocumentsCount()).thenReturn(12L);
        when(wikiSizeResult2.getUserCount()).thenReturn(1L);
        when(wikiSizeResult2.getAttachmentsSize()).thenReturn(12345L);

        when(usageDataProvider.getWikiSize(wikiDescriptor)).thenReturn(wikiSizeResult);
        when(usageDataProvider.getWikiSize(wikiDescriptor2)).thenReturn(wikiSizeResult2);

        Map<String, String> filters = new HashMap<>(
            Map.of("userCount", "12", "attachmentsSize", "1234-1234567", "attachmentsCount", "12345", "documentsCount",
                "123"));
        List<WikiSizeResult> testResults = instanceUsage.getWikisSize(filters, "", "");
        assertEquals(1, testResults.size());
        assertEquals("wiki name", testResults.get(0).getName());
    }

    @Test
    void checkSort() throws WikiManagerException, QueryException
    {
        when(wikiDescriptorManager.getAll()).thenReturn(new ArrayList<>(List.of(wikiDescriptor, wikiDescriptor2)));
        when(wikiDescriptor2.getPrettyName()).thenReturn("wiki2 name");
        when(wikiSizeResult.getAttachmentsCount()).thenReturn(12345L);
        when(wikiSizeResult.getName()).thenReturn("wiki name");
        when(wikiSizeResult.getDocumentsCount()).thenReturn(123L);
        when(wikiSizeResult.getUserCount()).thenReturn(12L);
        when(wikiSizeResult.getAttachmentsSize()).thenReturn(123456L);

        when(wikiSizeResult2.getAttachmentsCount()).thenReturn(1234L);
        when(wikiSizeResult2.getName()).thenReturn("wiki2 name");
        when(wikiSizeResult2.getDocumentsCount()).thenReturn(12L);
        when(wikiSizeResult2.getUserCount()).thenReturn(1L);
        when(wikiSizeResult2.getAttachmentsSize()).thenReturn(1234567L);

        when(usageDataProvider.getWikiSize(wikiDescriptor)).thenReturn(wikiSizeResult);
        when(usageDataProvider.getWikiSize(wikiDescriptor2)).thenReturn(wikiSizeResult2);

        Map<String, String> filters =
            new HashMap<>(Map.of("userCount", "", "attachmentsSize", "", "attachmentsCount", "", "documentsCount", ""));
        List<WikiSizeResult> testResults = instanceUsage.getWikisSize(filters, "attachmentsSize", "desc");
        assertEquals(2, testResults.size());
        assertEquals("wiki2 name", testResults.get(0).getName());
        assertEquals("wiki name", testResults.get(1).getName());
    }
}
