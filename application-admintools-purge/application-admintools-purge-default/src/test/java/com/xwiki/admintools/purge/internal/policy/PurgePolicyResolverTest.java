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
package com.xwiki.admintools.purge.internal.policy;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgePolicyResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgePolicyResolverTest
{
    private static final String WIKI_ID = "subwiki";

    private static final ZonedDateTime NOW = ZonedDateTime.of(2026, 10, 30, 1, 0, 0, 0, ZoneId.of("Europe/Paris"));

    private static final Instant EXPIRATION_DATE = Instant.parse("2026-10-23T00:00:00Z");

    private static final PurgeTypeSettings GLOBAL_ENABLED =
        new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings DISABLED = new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    @InjectMockComponents
    private PurgePolicyResolver resolver;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private RetentionCalculator retentionCalculator;

    @Test
    void getExpirationDateWithWikiSettings() throws Exception
    {
        mockSettings(GLOBAL_ENABLED,
            Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS)));
        when(this.retentionCalculator.getExpirationDate(NOW, 7, IntervalUnit.DAYS)).thenReturn(EXPIRATION_DATE);

        assertEquals(Optional.of(EXPIRATION_DATE), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));
    }

    @Test
    void getExpirationDateWithInheritedSettings() throws Exception
    {
        when(this.retentionCalculator.getExpirationDate(NOW, 1, IntervalUnit.MONTHS)).thenReturn(EXPIRATION_DATE);

        mockSettings(GLOBAL_ENABLED, Map.of(DELETED_DOCUMENTS, INHERIT));
        assertEquals(Optional.of(EXPIRATION_DATE), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));

        // A wiki without settings for the item type inherits them too.
        mockSettings(GLOBAL_ENABLED, Map.of());
        assertEquals(Optional.of(EXPIRATION_DATE), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));
    }

    @Test
    void getExpirationDateWhenDisabled() throws Exception
    {
        // Disabled by the wiki, although enabled by default.
        mockSettings(GLOBAL_ENABLED, Map.of(DELETED_DOCUMENTS, DISABLED));
        assertEquals(Optional.empty(), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));

        // Disabled by default, and inherited by the wiki.
        mockSettings(DISABLED, Map.of(DELETED_DOCUMENTS, INHERIT));
        assertEquals(Optional.empty(), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));

        verify(this.retentionCalculator, never()).getExpirationDate(any(), anyInt(), any());
    }

    @Test
    void getExpirationDateWithoutGlobalSettings() throws Exception
    {
        // An item type without global settings is not purged for the wikis that inherit them.
        mockSettings(null, Map.of(DELETED_DOCUMENTS, INHERIT));
        assertEquals(Optional.empty(), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));

        mockSettings(null, Map.of());
        assertEquals(Optional.empty(), this.resolver.getExpirationDate(WIKI_ID, DELETED_DOCUMENTS, NOW));

        verify(this.retentionCalculator, never()).getExpirationDate(any(), anyInt(), any());
    }

    /**
     * Make the test wiki use the given settings.
     *
     * @param globalSettings the global settings of the deleted pages, or {@code null} if there are none
     * @param wikiSettings the settings of the test wiki
     */
    private void mockSettings(PurgeTypeSettings globalSettings, Map<PurgeType, PurgeTypeSettings> wikiSettings)
        throws Exception
    {
        GlobalPurgeSettings settings = mock(GlobalPurgeSettings.class);
        when(settings.getTypeSettings(DELETED_DOCUMENTS)).thenReturn(Optional.ofNullable(globalSettings));
        when(this.settingsStore.getGlobalSettings()).thenReturn(settings);
        when(this.settingsStore.getWikiSettings(WIKI_ID)).thenReturn(wikiSettings);
    }
}
