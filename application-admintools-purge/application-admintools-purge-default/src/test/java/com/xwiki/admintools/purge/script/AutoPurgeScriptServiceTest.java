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
package com.xwiki.admintools.purge.script;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.purge.internal.PurgeManager;
import com.xwiki.admintools.purge.internal.PurgeWikisDataProvider;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutoPurgeScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class AutoPurgeScriptServiceTest
{
    private static final WikiReference MAIN_WIKI = new WikiReference("xwiki");

    private static final Map<String, String> FILTERS = Map.of("wikiName", "sub");

    private static final String SUBWIKI = "subwiki";

    @InjectMockComponents
    private AutoPurgeScriptService scriptService;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private PurgeWikisDataProvider purgeWikisDataProvider;

    @MockComponent
    private PurgeManager purgeManager;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgeHistoryStore historyStore;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        when(this.purgeManager.isLicensed()).thenReturn(true);
    }

    @Test
    void getWikis() throws Exception
    {
        Map<String, Object> page = Map.of("total", 1, "rows", List.of(Map.of("wikiId", SUBWIKI)));
        when(this.purgeWikisDataProvider.getWikis(FILTERS, "asc", 0, 15)).thenReturn(page);

        assertSame(page, this.scriptService.getWikis(FILTERS, "asc", 0, 15));
        verify(this.contextualAuthorizationManager).checkAccess(Right.ADMIN, MAIN_WIKI);
    }

    @Test
    void getWikisWithoutAdminRightsOnTheMainWiki() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.ADMIN, MAIN_WIKI);

        assertThrows(AccessDeniedException.class, () -> this.scriptService.getWikis(FILTERS, "asc", 0, 15));
        verify(this.purgeWikisDataProvider, never()).getWikis(any(), any(), anyInt(), anyInt());
    }

    @Test
    void getWikisWithoutLicense() throws Exception
    {
        when(this.purgeManager.isLicensed()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> this.scriptService.getWikis(FILTERS, "asc", 0, 15));
        verify(this.purgeWikisDataProvider, never()).getWikis(any(), any(), anyInt(), anyInt());
    }

    @Test
    void getDefaultSettings() throws Exception
    {
        GlobalPurgeSettings settings = mock(GlobalPurgeSettings.class);
        when(this.settingsStore.getGlobalSettings()).thenReturn(settings);

        assertSame(settings, this.scriptService.getDefaultSettings());
        verify(this.contextualAuthorizationManager).checkAccess(Right.ADMIN, MAIN_WIKI);
    }

    @Test
    void getDefaultSettingsWithoutAdminRightsOnTheMainWiki() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.ADMIN, MAIN_WIKI);

        assertThrows(AccessDeniedException.class, () -> this.scriptService.getDefaultSettings());
        verify(this.settingsStore, never()).getGlobalSettings();
    }

    @Test
    void getDefaultSettingsWithoutLicense() throws Exception
    {
        when(this.purgeManager.isLicensed()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> this.scriptService.getDefaultSettings());
        verify(this.settingsStore, never()).getGlobalSettings();
    }

    @Test
    void getHistory() throws Exception
    {
        List<PurgeHistoryEntry> entries = List.of(mock(PurgeHistoryEntry.class));
        when(this.historyStore.getEntries(SUBWIKI)).thenReturn(entries);

        assertSame(entries, this.scriptService.getHistory(SUBWIKI));
        verify(this.contextualAuthorizationManager).checkAccess(Right.ADMIN, MAIN_WIKI);
    }

    @Test
    void getHistoryWithoutAdminRightsOnTheMainWiki() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.ADMIN, MAIN_WIKI);

        assertThrows(AccessDeniedException.class, () -> this.scriptService.getHistory(SUBWIKI));
        verify(this.historyStore, never()).getEntries(any());
    }

    @Test
    void getHistoryWithoutLicense() throws Exception
    {
        when(this.purgeManager.isLicensed()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> this.scriptService.getHistory(SUBWIKI));
        verify(this.historyStore, never()).getEntries(any());
    }
}
