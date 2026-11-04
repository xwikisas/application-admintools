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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    private static final String TOTAL = "total";

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
        when(this.historyStore.getLatestEntries(any())).thenReturn(Map.of("alpha", Map.of(DELETED_DOCUMENTS, SUCCESS)));
    }

    @Test
    void getWikis() throws Exception
    {
        Map<String, Object> result = this.provider.getWikis(Map.of(), "asc", 0, 10);

        assertEquals(3, result.get(TOTAL));
        assertEquals(List.of("alpha", "beta", "xwiki"), getWikiIds(result));
        Map<String, Object> expectedRow = new HashMap<>();
        expectedRow.put("wikiId", "alpha");
        expectedRow.put("wikiName", "alpha");
        expectedRow.put("deletedDocuments", ENABLED);
        expectedRow.put("deletedDocumentsLastPurge", SUCCESS);
        expectedRow.put("deletedAttachments", INHERIT);
        assertEquals(expectedRow, getRows(result).get(0));
        // Without a status filter, the newest purges are read only for the wikis of the page.
        verify(this.historyStore).getLatestEntries(List.of("alpha", "beta", "xwiki"));

        assertEquals(List.of("xwiki", "beta", "alpha"), getWikiIds(this.provider.getWikis(Map.of(), "desc", 0, 10)));
    }

    @Test
    void getWikisPage() throws Exception
    {
        Map<String, Object> result = this.provider.getWikis(Map.of(), "asc", 1, 1);
        assertEquals(3, result.get(TOTAL));
        assertEquals(List.of("beta"), getWikiIds(result));
        verify(this.historyStore).getLatestEntries(List.of("beta"));

        // A page after the last wiki is empty.
        result = this.provider.getWikis(Map.of(), "asc", 5, 10);
        assertEquals(3, result.get(TOTAL));
        assertEquals(List.of(), getWikiIds(result));
        verify(this.historyStore).getLatestEntries(List.of());
    }

    @Test
    void getWikisWithFilters() throws Exception
    {
        assertEquals(List.of("alpha"), getWikiIds(this.provider.getWikis(Map.of("wikiName", "ALP"), "asc", 0, 10)));
        assertEquals(List.of("alpha", "beta"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsMode", "enabled"), "asc", 0, 10)));
        verify(this.historyStore, never()).getLatestEntries(null);

        // Filtering by the status of the newest purge needs the newest purges of every wiki.
        assertEquals(List.of("alpha"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsStatus", "success"), "asc", 0, 10)));
        verify(this.historyStore).getLatestEntries(null);
        assertEquals(List.of("beta", "xwiki"),
            getWikiIds(this.provider.getWikis(Map.of("deletedDocumentsStatus", "none"), "asc", 0, 10)));
        Map<String, Object> result = this.provider.getWikis(
            Map.of("deletedDocumentsMode", "enabled", "deletedDocumentsStatus", "none", "deletedAttachmentsMode", ""),
            "asc", 0, 10);
        assertEquals(List.of("beta"), getWikiIds(result));
        assertEquals(1, result.get(TOTAL));
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
        Map<String, Object> result = this.provider.getWikis(Map.of(), "asc", 0, 10);
        assertEquals(List.of("alpha", "beta", "zeta"), getWikiIds(result));
        assertEquals("zeta", getRows(result).get(2).get("wikiName"));
        assertEquals(List.of("zeta"), getWikiIds(this.provider.getWikis(Map.of("wikiName", "ZE"), "asc", 0, 10)));
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
     * @param result a page of the rows of the LiveData
     * @return the rows of the page
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getRows(Map<String, Object> result)
    {
        return (List<Map<String, Object>>) result.get("rows");
    }

    /**
     * @param result a page of the rows of the LiveData
     * @return the identifiers of the wikis of the rows, in the same order
     */
    private List<Object> getWikiIds(Map<String, Object> result)
    {
        return getRows(result).stream().map(row -> row.get("wikiId")).collect(Collectors.toList());
    }
}
