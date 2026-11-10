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

    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

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
        mockTypeFields("deletedDocuments", "enabled", "7", "days");
        mockTypeFields("deletedAttachments", "disabled", "0", "months");
        when(this.request.getParameter("maxHistoryEntries")).thenReturn("20");

        assertEquals(204, this.resource.saveDefaultSettings().getStatus());

        verify(this.settingsStore).saveGlobalSettings(
            Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS),
                DELETED_ATTACHMENTS, new PurgeTypeSettings(PurgeMode.DISABLED, 0, IntervalUnit.MONTHS)), 20);
    }

    @Test
    void saveWikiSettings() throws Exception
    {
        mockTypeFields("deletedDocuments", "inherit", "1", "months");
        mockTypeFields("deletedAttachments", "inherit", "1", "months");

        assertEquals(204, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore).saveWikiSettings(SUBWIKI,
            Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT));
    }

    @Test
    void saveSettingsWithInvalidValues() throws Exception
    {
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        mockTypeFields("deletedDocuments", "sometimes", "7", "days");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "seven", "days");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "7", "decades");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveWikiSettings(anyString(), any());

        // The missing history size of the defaults is rejected, as well as the settings rejected by the settings
        // store.
        mockTypeFields("deletedDocuments", "enabled", "7", "days");
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        assertEquals(400, this.resource.saveDefaultSettings().getStatus());
        doThrow(new IllegalArgumentException("Invalid")).when(this.settingsStore).saveWikiSettings(anyString(), any());
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());
    }

    @Test
    void saveSettingsWithMissingFields() throws Exception
    {
        // The interval and unit are required for every item type, whatever its mode, instead of being read as
        // values that could be valid.
        mockTypeFields("deletedDocuments", "enabled", null, "days");
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "7", "days");
        mockTypeFields("deletedAttachments", "disabled", "1", " ");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedAttachments", "inherit", "", "months");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveWikiSettings(anyString(), any());
    }

    @Test
    void purgeWiki() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        when(this.purgeManager.startPurge(SUBWIKI, DELETED_DOCUMENTS, USER)).thenReturn(true);
        when(this.purgeManager.startPurge(SUBWIKI, DELETED_ATTACHMENTS, USER)).thenReturn(false);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals(202, response.getStatus());
        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
    }

    @Test
    void purgeWikiWithRepeatedType() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedDocuments" });
        when(this.purgeManager.startPurge(SUBWIKI, DELETED_DOCUMENTS, USER)).thenReturn(true);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
        verify(this.purgeManager).startPurge(SUBWIKI, DELETED_DOCUMENTS, USER);
    }

    @Test
    void purgeWikiWithInvalidTypes() throws Exception
    {
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // No job is started when one of the item types is unknown.
        when(this.request.getParameterValues("type")).thenReturn(new String[] { "deletedDocuments", "comments" });
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        verify(this.purgeManager, never()).startPurge(anyString(), any(), any());
    }

    @Test
    void previewPurge() throws Exception
    {
        when(this.purgeManager.getExpiredCounts(SUBWIKI)).thenReturn(Map.of(DELETED_DOCUMENTS, 7L));
        // Reading doesn't need a form token.
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(false);

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":7}", response.getEntity());
    }

    @Test
    void requestWithoutAdminRights() throws Exception
    {
        doThrow(new AccessDeniedException(Right.ADMIN, USER, new WikiReference(MAIN_WIKI))).when(
            this.authorizationManager).checkAccess(Right.ADMIN, USER, new WikiReference(MAIN_WIKI));

        assertEquals(403, this.resource.saveDefaultSettings().getStatus());
        assertEquals(403, this.resource.previewPurge(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveGlobalSettings(any(), anyInt());
        verify(this.purgeManager, never()).getExpiredCounts(anyString());
        assertEquals("Failed to access the automatic purge due to restricted rights.", this.logCapture.getMessage(0));
        assertEquals("Failed to access the automatic purge due to restricted rights.", this.logCapture.getMessage(1));
    }

    @Test
    void requestFromGuest() throws Exception
    {
        when(this.context.getUserReference()).thenReturn(null);
        doThrow(new AccessDeniedException(Right.ADMIN, null, new WikiReference(MAIN_WIKI))).when(
            this.authorizationManager).checkAccess(Right.ADMIN, null, new WikiReference(MAIN_WIKI));

        // A guest might get the rights by logging in.
        assertEquals(401, this.resource.purgeWiki(SUBWIKI).getStatus());

        verify(this.purgeManager, never()).startPurge(anyString(), any(), any());
        assertEquals("Failed to access the automatic purge due to restricted rights.", this.logCapture.getMessage(0));
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

        verify(this.purgeManager, never()).startPurge(anyString(), any(), any());
    }

    @Test
    void requestOnUnknownWiki() throws Exception
    {
        assertEquals(404, this.resource.previewPurge("unknown").getStatus());

        verify(this.purgeManager, never()).getExpiredCounts(anyString());
    }

    @Test
    void requestFailure() throws Exception
    {
        when(this.purgeManager.getExpiredCounts(SUBWIKI)).thenThrow(new XWikiException());

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(500, response.getStatus());
        assertNull(response.getEntity());
        assertEquals("Failed to count the expired items of the recycle bin of wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void previewPurgeWithInvalidStoredSettings() throws Exception
    {
        when(this.purgeManager.getExpiredCounts(SUBWIKI)).thenThrow(
            new IllegalArgumentException("The purge interval unit is missing."));

        Response response = this.resource.previewPurge(SUBWIKI);

        // The stored settings are at fault, not the request, so it isn't rejected as invalid.
        assertEquals(409, response.getStatus());
        assertNull(response.getEntity());
        assertEquals("The stored automatic purge settings of wiki [subwiki] are invalid.",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeWikiWhenOneTypeFails() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        when(this.purgeManager.startPurge(SUBWIKI, DELETED_DOCUMENTS, USER)).thenThrow(new XWikiException());
        when(this.purgeManager.startPurge(SUBWIKI, DELETED_ATTACHMENTS, USER)).thenReturn(true);

        Response response = this.resource.purgeWiki(SUBWIKI);

        // The other type is still started, and its job is returned.
        assertEquals(202, response.getStatus());
        assertEquals("[[\"adminTools\",\"purge\",\"deletedAttachments\",\"subwiki\"]]", response.getEntity());
        assertEquals("Failed to start the purge of [deletedDocuments] on wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    private void mockTypeFields(String typeId, String mode, String interval, String unit)
    {
        when(this.request.getParameter(typeId + "Mode")).thenReturn(mode);
        when(this.request.getParameter(typeId + "Interval")).thenReturn(interval);
        when(this.request.getParameter(typeId + "Unit")).thenReturn(unit);
    }
}
