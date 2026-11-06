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
package com.xwiki.admintools.purge.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeWikisDataProvider}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeWikisDataProviderTest
{
    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings ENABLED = new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS);

    private static final PurgeHistoryEntry SUCCESS =
        new PurgeHistoryEntry(DELETED_DOCUMENTS, Instant.parse("2026-10-30T01:00:00Z"), null, PurgeStatus.SUCCESS, 3, 0,
            null);

    @InjectMockComponents
    private PurgeWikisDataProvider provider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgeHistoryStore historyStore;

    @BeforeEach
    void setUp() throws Exception
    {
        // The wiki names differ in case, to check that the sort ignores it.
        when(this.wikiDescriptorManager.getAll()).thenAnswer(invocation -> new ArrayList<>(
            List.of(mockWiki("xwiki", "Main wiki"), mockWiki("beta", "Beta"), mockWiki("alpha", "alpha"))));
        when(this.settingsStore.getSettingsOfWikis(any())).thenReturn(
            Map.of("xwiki", Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT), "beta",
                Map.of(DELETED_DOCUMENTS, ENABLED, DELETED_ATTACHMENTS, INHERIT), "alpha",
                Map.of(DELETED_DOCUMENTS, ENABLED, DELETED_ATTACHMENTS, INHERIT)));
        when(this.historyStore.getAllLatestEntries()).thenReturn(Map.of("alpha", Map.of(DELETED_DOCUMENTS, SUCCESS)));
    }

    @Test
    void getWikis() throws Exception
    {
        List<Map<String, Object>> rows = this.provider.getWikis(Map.of(), "asc");

        assertEquals(List.of("alpha", "beta", "xwiki"), getWikiIds(rows));
        Map<String, Object> expectedRow = new HashMap<>();
        expectedRow.put("wikiId", "alpha");
        expectedRow.put("wikiName", "alpha");
        expectedRow.put("deletedDocuments", ENABLED);
        expectedRow.put("deletedDocumentsLastPurge", SUCCESS);
        expectedRow.put("deletedAttachments", INHERIT);
        assertEquals(expectedRow, rows.get(0));

        assertEquals(List.of("xwiki", "beta", "alpha"), getWikiIds(this.provider.getWikis(Map.of(), "desc")));
    }

    @Test
    void getWikisWithFilters() throws Exception
    {
        assertEquals(List.of("alpha"), getWikiIds(this.provider.getWikis(Map.of("wikiName", "ALP"), "asc")));
        assertEquals(List.of("alpha", "beta"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsMode", "enabled"), "asc")));
        assertEquals(List.of("alpha"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsStatus", "success"), "asc")));
        assertEquals(List.of("beta", "xwiki"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsStatus", "none"), "asc")));
        assertEquals(List.of("beta"), getWikiIds(this.provider.getWikis(
            Map.of("deletedDocumentsMode", "enabled", "deletedDocumentsStatus", "none", "deletedAttachmentsMode", ""),
            "asc")));
    }

    @Test
    void getWikisNamesWikisWithoutPrettyName() throws Exception
    {
        when(this.wikiDescriptorManager.getAll()).thenAnswer(invocation -> new ArrayList<>(
            List.of(mockWiki("zeta", null), mockWiki("beta", " "), mockWiki("alpha", "Alpha"))));
        when(this.settingsStore.getSettingsOfWikis(any())).thenReturn(
            Map.of("zeta", Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT), "beta",
                Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT), "alpha",
                Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT)));

        // A wiki without a pretty name is named, sorted and filtered by its identifier.
        List<Map<String, Object>> rows = this.provider.getWikis(Map.of(), "asc");
        assertEquals(List.of("alpha", "beta", "zeta"), getWikiIds(rows));
        assertEquals("zeta", rows.get(2).get("wikiName"));
        assertEquals(List.of("zeta"), getWikiIds(this.provider.getWikis(Map.of("wikiName", "ZE"), "asc")));
    }

    /**
     * @param id the identifier of the wiki
     * @param name the pretty name of the wiki
     * @return the descriptor of the wiki
     */
    private WikiDescriptor mockWiki(String id, String name)
    {
        WikiDescriptor wiki = mock(WikiDescriptor.class);
        when(wiki.getId()).thenReturn(id);
        when(wiki.getPrettyName()).thenReturn(name);
        return wiki;
    }

    /**
     * @param rows rows of the LiveData
     * @return the identifiers of the wikis of the rows, in the same order
     */
    private List<Object> getWikiIds(List<Map<String, Object>> rows)
    {
        return rows.stream().map(row -> row.get("wikiId")).collect(Collectors.toList());
    }
}
