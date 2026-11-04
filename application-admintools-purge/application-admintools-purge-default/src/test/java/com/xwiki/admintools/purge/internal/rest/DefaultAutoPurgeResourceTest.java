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
package com.xwiki.admintools.purge.internal.rest;

import java.util.Map;

import javax.inject.Provider;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.purge.internal.PurgeManager;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultAutoPurgeResource}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultAutoPurgeResourceTest
{
    private static final String MAIN_WIKI = "xwiki";

    private static final String SUBWIKI = "subwiki";

    private static final String TOKEN = "token";

    private static final DocumentReference USER = new DocumentReference(MAIN_WIKI, "XWiki", "Admin");

    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 0, null);

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultAutoPurgeResource resource;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private CSRFToken csrfToken;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private PurgeManager purgeManager;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgePolicyResolver policyResolver;

    @Mock
    private XWikiRequest request;

    private XWikiContext context;

    @BeforeComponent
    void beforeComponent()
    {
        // XWikiResource uses the context when it is initialized.
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getRequest()).thenReturn(this.request);
        when(this.context.getUserReference()).thenReturn(USER);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn(MAIN_WIKI);
        when(this.wikiDescriptorManager.exists(SUBWIKI)).thenReturn(true);
        when(this.request.getHeader("XWiki-Form-Token")).thenReturn(TOKEN);
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(true);
        when(this.purgeManager.isLicensed()).thenReturn(true);
    }

    @Test
    void saveDefaultSettings() throws Exception
    {
        when(this.request.getParameter("deletedDocumentsMode")).thenReturn("enabled");
        when(this.request.getParameter("deletedDocumentsInterval")).thenReturn("7");
        when(this.request.getParameter("deletedDocumentsUnit")).thenReturn("days");
        when(this.request.getParameter("deletedAttachmentsMode")).thenReturn("disabled");
        when(this.request.getParameter("maxHistoryEntries")).thenReturn("20");

        assertEquals(204, this.resource.saveDefaultSettings().getStatus());

        verify(this.settingsStore).saveGlobalSettings(
            Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS),
                DELETED_ATTACHMENTS, new PurgeTypeSettings(PurgeMode.DISABLED, 0, null)), 20);
    }

    @Test
    void saveWikiSettings() throws Exception
    {
        when(this.request.getParameter("deletedDocumentsMode")).thenReturn("inherit");
        when(this.request.getParameter("deletedAttachmentsMode")).thenReturn("inherit");

        assertEquals(204, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore).saveWikiSettings(SUBWIKI,
            Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT));
    }

    @Test
    void saveSettingsWithInvalidValues() throws Exception
    {
        when(this.request.getParameter("deletedDocumentsMode")).thenReturn("sometimes");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        when(this.request.getParameter("deletedDocumentsMode")).thenReturn("enabled");
        when(this.request.getParameter("deletedDocumentsInterval")).thenReturn("seven");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        when(this.request.getParameter("deletedDocumentsInterval")).thenReturn("7");
        when(this.request.getParameter("deletedDocumentsUnit")).thenReturn("decades");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveWikiSettings(anyString(), any());

        // The settings store rejects the settings that can be parsed but are invalid, such as the missing history
        // size of the defaults.
        when(this.request.getParameter("deletedDocumentsUnit")).thenReturn("days");
        when(this.request.getParameter("deletedAttachmentsMode")).thenReturn("inherit");
        assertEquals(400, this.resource.saveDefaultSettings().getStatus());
        doThrow(new IllegalArgumentException("Invalid")).when(this.settingsStore).saveWikiSettings(anyString(), any());
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());
    }

    @Test
    void purgeWiki() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        mockLimit("deletedDocuments", "10", "years");
        mockLimit("deletedAttachments", "1", "months");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 10, IntervalUnit.YEARS, USER))
            .thenReturn(true);
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_ATTACHMENTS, 1, IntervalUnit.MONTHS, USER))
            .thenReturn(false);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals(202, response.getStatus());
        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
    }

    @Test
    void purgeWikiWithRepeatedType() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedDocuments" });
        mockLimit("deletedDocuments", "1", "weeks");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1, IntervalUnit.WEEKS, USER))
            .thenReturn(true);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
        verify(this.purgeManager).startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1, IntervalUnit.WEEKS, USER);
    }

    @Test
    void purgeWikiWithInvalidTypes() throws Exception
    {
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // No job is started when one of the item types is unknown.
        mockLimit("deletedDocuments", "1", "months");
        when(this.request.getParameterValues("type")).thenReturn(new String[] { "deletedDocuments", "comments" });
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), anyInt(), any(), any());
    }

    @Test
    void purgeWikiWithInvalidLimits() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        mockLimit("deletedDocuments", "1", "months");

        // The limit of an item type is missing.
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // Only half of the limit is set.
        mockLimit("deletedAttachments", "1", null);
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", null, "days");
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // The limit is invalid: the purge manager rejects an interval out of bounds, e.g. below 0 or more than 1000.
        mockLimit("deletedAttachments", "-1", "days");
        doThrow(new IllegalArgumentException("Invalid purge limit")).when(this.purgeManager).checkLimit(-1,
            IntervalUnit.DAYS);
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "1001", "years");
        doThrow(new IllegalArgumentException("Invalid purge limit")).when(this.purgeManager).checkLimit(1001,
            IntervalUnit.YEARS);
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "seven", "days");
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "7", "decades");
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // Nothing is started, not even the item type whose limit is valid.
        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), anyInt(), any(), any());
    }

    @Test
    void previewPurge() throws Exception
    {
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_DOCUMENTS)).thenReturn(
            new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS));
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_ATTACHMENTS)).thenReturn(
            new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS));
        // The stored limit applies to the pages, and the requested one to the attachments, although they are disabled.
        mockLimit("deletedAttachments", "1", "years");
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_DOCUMENTS, 1, IntervalUnit.MONTHS)).thenReturn(7L);
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_ATTACHMENTS, 1, IntervalUnit.YEARS)).thenReturn(2L);
        // Reading doesn't need a form token.
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(false);

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":{\"enabled\":true,\"interval\":1,\"unit\":\"months\",\"count\":7},"
            + "\"deletedAttachments\":{\"enabled\":false,\"interval\":1,\"unit\":\"years\",\"count\":2}}",
            response.getEntity());
    }

    @Test
    void previewPurgeWithInvalidLimit() throws Exception
    {
        when(this.policyResolver.getSettings(eq(SUBWIKI), any())).thenReturn(
            new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS));

        mockLimit("deletedDocuments", "seven", "days");
        assertEquals(400, this.resource.previewPurge(SUBWIKI).getStatus());

        // A requested interval below 0 is rejected by the purge manager, instead of being skipped.
        mockLimit("deletedDocuments", "-1", "days");
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_DOCUMENTS, -1, IntervalUnit.DAYS)).thenThrow(
            new IllegalArgumentException("Invalid purge limit"));
        assertEquals(400, this.resource.previewPurge(SUBWIKI).getStatus());
    }

    @Test
    void requestWithoutAdminRights() throws Exception
    {
        doThrow(new AccessDeniedException(Right.ADMIN, USER, new WikiReference(MAIN_WIKI))).when(
            this.authorizationManager).checkAccess(Right.ADMIN, USER, new WikiReference(MAIN_WIKI));

        assertEquals(401, this.resource.saveDefaultSettings().getStatus());
        assertEquals(401, this.resource.previewPurge(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveGlobalSettings(any(), anyInt());
        verify(this.policyResolver, never()).getSettings(anyString(), any());
        assertEquals("Failed to access the automatic purge due to restricted rights.", this.logCapture.getMessage(0));
        assertEquals("Failed to access the automatic purge due to restricted rights.", this.logCapture.getMessage(1));
    }

    @Test
    void requestWithInvalidFormToken() throws Exception
    {
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(false);

        Response response = this.resource.saveWikiSettings(SUBWIKI);

        assertEquals(403, response.getStatus());
        assertNull(response.getEntity());
        verify(this.settingsStore, never()).saveWikiSettings(anyString(), any());
    }

    @Test
    void requestWithInactiveLicense() throws Exception
    {
        when(this.purgeManager.isLicensed()).thenReturn(false);

        assertEquals(402, this.resource.purgeWiki(SUBWIKI).getStatus());

        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), anyInt(), any(), any());
    }

    @Test
    void requestOnUnknownWiki() throws Exception
    {
        assertEquals(404, this.resource.previewPurge("unknown").getStatus());

        verify(this.policyResolver, never()).getSettings(anyString(), any());
    }

    @Test
    void requestFailure() throws Exception
    {
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_DOCUMENTS)).thenThrow(new XWikiException());

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(500, response.getStatus());
        assertNull(response.getEntity());
        assertEquals("Failed to count the expired items of the recycle bin of wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeWikiWhenOneTypeFails() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        mockLimit("deletedDocuments", "1", "months");
        mockLimit("deletedAttachments", "1", "months");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, 1, IntervalUnit.MONTHS, USER))
            .thenThrow(new JobException("Failed"));
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_ATTACHMENTS, 1, IntervalUnit.MONTHS, USER))
            .thenReturn(true);

        Response response = this.resource.purgeWiki(SUBWIKI);

        // The other type is still started, and its job is returned.
        assertEquals(202, response.getStatus());
        assertEquals("[[\"adminTools\",\"purge\",\"deletedAttachments\",\"subwiki\"]]", response.getEntity());
        assertEquals("Failed to start the purge of [deletedDocuments] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    /**
     * Set the limit of a manual purge of an item type in the request.
     *
     * @param typeId the identifier of the item type
     * @param interval the interval, or {@code null} to leave it out
     * @param unit the unit, or {@code null} to leave it out
     */
    private void mockLimit(String typeId, String interval, String unit)
    {
        when(this.request.getParameter(typeId + "Interval")).thenReturn(interval);
        when(this.request.getParameter(typeId + "Unit")).thenReturn(unit);
    }
}
