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
import java.util.Optional;

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
import org.xwiki.test.annotation.ComponentList;
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
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.PAGE_HISTORY;
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
@ComponentList(PurgeRequestReader.class)
class DefaultAutoPurgeResourceTest
{
    private static final String MAIN_WIKI = "xwiki";

    private static final String SUBWIKI = "subwiki";

    private static final String TOKEN = "token";

    private static final DocumentReference USER = new DocumentReference(MAIN_WIKI, "XWiki", "Admin");

    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings HISTORY_INHERIT =
        new HistoryPurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS, null);

    private static final String HISTORY_MAX_VERSIONS = "pageHistoryMaxVersions";

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
        mockTypeFields("deletedDocuments", "enabled", "7", "days");
        mockTypeFields("deletedAttachments", "disabled", "0", "months");
        // The history is limited only by its number of versions.
        mockTypeFields("pageHistory", "enabled", "", "");
        when(this.request.getParameter(HISTORY_MAX_VERSIONS)).thenReturn("50");
        when(this.request.getParameter("maxHistoryEntries")).thenReturn("20");

        assertEquals(204, this.resource.saveDefaultSettings().getStatus());

        verify(this.settingsStore).saveGlobalSettings(
            Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS),
                DELETED_ATTACHMENTS, new PurgeTypeSettings(PurgeMode.DISABLED, 0, IntervalUnit.MONTHS), PAGE_HISTORY,
                new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 50)), 20);
    }

    @Test
    void saveWikiSettings() throws Exception
    {
        mockTypeFields("deletedDocuments", "inherit", "1", "months");
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        mockTypeFields("pageHistory", "inherit", "1", "months");

        assertEquals(204, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore).saveWikiSettings(SUBWIKI,
            Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT, PAGE_HISTORY, HISTORY_INHERIT));
    }

    @Test
    void saveSettingsWithInvalidValues() throws Exception
    {
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        mockTypeFields("pageHistory", "inherit", "1", "months");
        mockTypeFields("deletedDocuments", "sometimes", "7", "days");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "seven", "days");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "7", "decades");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        mockTypeFields("deletedDocuments", "enabled", "7", "days");
        when(this.request.getParameter(HISTORY_MAX_VERSIONS)).thenReturn("many");
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).saveWikiSettings(anyString(), any());

        // The missing history size of the defaults is rejected, as well as the settings rejected by the settings
        // store.
        when(this.request.getParameter(HISTORY_MAX_VERSIONS)).thenReturn(null);
        mockTypeFields("deletedAttachments", "inherit", "1", "months");
        assertEquals(400, this.resource.saveDefaultSettings().getStatus());
        doThrow(new IllegalArgumentException("Invalid")).when(this.settingsStore).saveWikiSettings(anyString(), any());
        assertEquals(400, this.resource.saveWikiSettings(SUBWIKI).getStatus());
    }

    @Test
    void saveSettingsWithMissingFields() throws Exception
    {
        // A missing interval or unit is passed as missing, never as a value that could be valid: the settings store
        // keeps the stored ones of an item type that is not enabled, and rejects an enabled item type without them.
        mockTypeFields("deletedDocuments", "enabled", null, "days");
        mockTypeFields("deletedAttachments", "inherit", " ", "");
        mockTypeFields("pageHistory", "disabled", null, null);

        assertEquals(204, this.resource.saveWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore).saveWikiSettings(SUBWIKI,
            Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, null, IntervalUnit.DAYS),
                DELETED_ATTACHMENTS, new PurgeTypeSettings(PurgeMode.INHERIT, null, null), PAGE_HISTORY,
                new HistoryPurgeTypeSettings(PurgeMode.DISABLED, null, null, null)));
    }

    @Test
    void deleteWikiSettings() throws Exception
    {
        assertEquals(204, this.resource.deleteWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore).removeWikiSettings(SUBWIKI);
    }

    @Test
    void deleteWikiSettingsWithInvalidFormToken() throws Exception
    {
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(false);

        assertEquals(403, this.resource.deleteWikiSettings(SUBWIKI).getStatus());

        verify(this.settingsStore, never()).removeWikiSettings(anyString());
    }

    @Test
    void purgeWiki() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        mockLimit("deletedDocuments", "10", "years");
        mockLimit("deletedAttachments", "1", "months");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, limit(10, IntervalUnit.YEARS), USER))
            .thenReturn(true);
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_ATTACHMENTS, limit(1, IntervalUnit.MONTHS), USER))
            .thenReturn(false);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals(202, response.getStatus());
        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
    }

    @Test
    void purgeWikiHistory() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(new String[] { "pageHistory" });
        // The history can be limited by its number of versions only, or by both limits.
        when(this.request.getParameter(HISTORY_MAX_VERSIONS)).thenReturn("10");
        PurgeTypeSettings limit = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 10);
        when(this.purgeManager.startManualPurge(SUBWIKI, PAGE_HISTORY, limit, USER)).thenReturn(true);

        assertEquals("[[\"adminTools\",\"purge\",\"pageHistory\",\"subwiki\"]]",
            this.resource.purgeWiki(SUBWIKI).getEntity());

        mockLimit("pageHistory", "0", "days");
        PurgeTypeSettings bothLimits = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS, 10);
        when(this.purgeManager.startManualPurge(SUBWIKI, PAGE_HISTORY, bothLimits, USER)).thenReturn(true);

        assertEquals(202, this.resource.purgeWiki(SUBWIKI).getStatus());
        verify(this.purgeManager).checkLimit(PAGE_HISTORY, bothLimits);
    }

    @Test
    void purgeWikiWithRepeatedType() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedDocuments" });
        mockLimit("deletedDocuments", "1", "weeks");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, limit(1, IntervalUnit.WEEKS), USER))
            .thenReturn(true);

        Response response = this.resource.purgeWiki(SUBWIKI);

        assertEquals("[[\"adminTools\",\"purge\",\"deletedDocuments\",\"subwiki\"]]", response.getEntity());
        verify(this.purgeManager).startManualPurge(SUBWIKI, DELETED_DOCUMENTS, limit(1, IntervalUnit.WEEKS), USER);
    }

    @Test
    void purgeWikiWithInvalidTypes() throws Exception
    {
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // No job is started when one of the item types is unknown.
        mockLimit("deletedDocuments", "1", "months");
        when(this.request.getParameterValues("type")).thenReturn(new String[] { "deletedDocuments", "comments" });
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), any(), any());
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
        doThrow(new IllegalArgumentException("Invalid purge limit")).when(this.purgeManager)
            .checkLimit(DELETED_ATTACHMENTS, limit(-1, IntervalUnit.DAYS));
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "1001", "years");
        doThrow(new IllegalArgumentException("Invalid purge limit")).when(this.purgeManager)
            .checkLimit(DELETED_ATTACHMENTS, limit(1001, IntervalUnit.YEARS));
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "seven", "days");
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());
        mockLimit("deletedAttachments", "7", "decades");
        assertEquals(400, this.resource.purgeWiki(SUBWIKI).getStatus());

        // Nothing is started, not even the item type whose limit is valid.
        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), any(), any());
    }

    @Test
    void previewPurge() throws Exception
    {
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_DOCUMENTS)).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS)));
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_ATTACHMENTS)).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS)));
        // The stored limit applies to the pages, and the requested one to the attachments, although they are disabled.
        mockLimit("deletedAttachments", "1", "years");
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_DOCUMENTS, limit(1, IntervalUnit.MONTHS)))
            .thenReturn(7L);
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_ATTACHMENTS, limit(1, IntervalUnit.YEARS)))
            .thenReturn(2L);
        // Reading doesn't need a form token.
        when(this.csrfToken.isTokenValid(TOKEN)).thenReturn(false);

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":{\"enabled\":true,\"interval\":1,\"unit\":\"months\",\"count\":7},"
            + "\"deletedAttachments\":{\"enabled\":false,\"interval\":1,\"unit\":\"years\",\"count\":2},"
            + "\"pageHistory\":{\"enabled\":false}}",
            response.getEntity());
    }

    @Test
    void previewPurgeWithoutSettings() throws Exception
    {
        // An item type left out of the default settings is not enabled, and is counted only for a requested limit.
        when(this.policyResolver.getSettings(eq(SUBWIKI), any())).thenReturn(Optional.empty());
        mockLimit("deletedAttachments", "0", "days");
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_ATTACHMENTS, limit(0, IntervalUnit.DAYS))).thenReturn(4L);

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":{\"enabled\":false},"
            + "\"deletedAttachments\":{\"enabled\":false,\"interval\":0,\"unit\":\"days\",\"count\":4},"
            + "\"pageHistory\":{\"enabled\":false}}",
            response.getEntity());
        verify(this.purgeManager, never()).countExpired(eq(SUBWIKI), eq(DELETED_DOCUMENTS), any());
    }

    @Test
    void previewPurgeWithoutStoredLimit() throws Exception
    {
        // Settings without a stored interval or unit, which can only come from a manual edit, give no count.
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_DOCUMENTS)).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, null, IntervalUnit.DAYS)));
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_ATTACHMENTS)).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, 7, null)));

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":{\"enabled\":true},\"deletedAttachments\":{\"enabled\":false},"
            + "\"pageHistory\":{\"enabled\":false}}", response.getEntity());
        verify(this.purgeManager, never()).countExpired(anyString(), any(), any());
    }

    @Test
    void previewPurgeOfRequestedTypes() throws Exception
    {
        when(this.policyResolver.getSettings(SUBWIKI, DELETED_DOCUMENTS)).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS)));
        when(this.policyResolver.getSettings(SUBWIKI, PAGE_HISTORY)).thenReturn(
            Optional.of(new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 20)));
        // Only the requested item types are counted, with the requested limit or the stored one.
        when(this.request.getParameterValues("type")).thenReturn(new String[] { "pageHistory" });
        mockLimit("pageHistory", "1", "years");
        when(this.request.getParameter(HISTORY_MAX_VERSIONS)).thenReturn("5");
        PurgeTypeSettings limit = new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.YEARS, 5);
        when(this.purgeManager.countExpired(SUBWIKI, PAGE_HISTORY, limit)).thenReturn(12L);

        Response response = this.resource.previewPurge(SUBWIKI);

        assertEquals(200, response.getStatus());
        assertEquals("{\"deletedDocuments\":{\"enabled\":true,\"interval\":1,\"unit\":\"months\"},"
            + "\"deletedAttachments\":{\"enabled\":false},"
            + "\"pageHistory\":{\"enabled\":true,\"interval\":1,\"unit\":\"years\",\"maxVersions\":5,"
            + "\"count\":12}}",
            response.getEntity());
        verify(this.purgeManager, never()).countExpired(eq(SUBWIKI), eq(DELETED_DOCUMENTS), any());
    }

    @Test
    void previewPurgeWithInvalidLimit() throws Exception
    {
        when(this.policyResolver.getSettings(eq(SUBWIKI), any())).thenReturn(
            Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS)));

        mockLimit("deletedDocuments", "seven", "days");
        assertEquals(400, this.resource.previewPurge(SUBWIKI).getStatus());

        // A requested interval below 0 is rejected by the purge manager, instead of being skipped.
        mockLimit("deletedDocuments", "-1", "days");
        when(this.purgeManager.countExpired(SUBWIKI, DELETED_DOCUMENTS, limit(-1, IntervalUnit.DAYS))).thenThrow(
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

        verify(this.purgeManager, never()).startManualPurge(anyString(), any(), any(), any());
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
        assertEquals("Failed to count the expired items of wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void purgeWikiWhenOneTypeFails() throws Exception
    {
        when(this.request.getParameterValues("type")).thenReturn(
            new String[] { "deletedDocuments", "deletedAttachments" });
        mockLimit("deletedDocuments", "1", "months");
        mockLimit("deletedAttachments", "1", "months");
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_DOCUMENTS, limit(1, IntervalUnit.MONTHS), USER))
            .thenThrow(new JobException("Failed"));
        when(this.purgeManager.startManualPurge(SUBWIKI, DELETED_ATTACHMENTS, limit(1, IntervalUnit.MONTHS), USER))
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

    /**
     * @param interval the interval of the limit
     * @param unit the unit of the interval
     * @return the limit of a manual purge, as the resource reads it from the request
     */
    private PurgeTypeSettings limit(int interval, IntervalUnit unit)
    {
        return new PurgeTypeSettings(PurgeMode.ENABLED, interval, unit);
    }

    private void mockTypeFields(String typeId, String mode, String interval, String unit)
    {
        when(this.request.getParameter(typeId + "Mode")).thenReturn(mode);
        when(this.request.getParameter(typeId + "Interval")).thenReturn(interval);
        when(this.request.getParameter(typeId + "Unit")).thenReturn(unit);
    }
}
