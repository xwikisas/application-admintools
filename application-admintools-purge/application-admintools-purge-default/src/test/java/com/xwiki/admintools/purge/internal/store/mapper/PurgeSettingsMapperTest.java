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
package com.xwiki.admintools.purge.internal.store.mapper;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeSettingsMapper}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeSettingsMapperTest
{
    private static final String WIKI_ID = "subwiki";

    private static final String DOCUMENTS_MODE = "deletedDocumentsMode";

    private static final String DOCUMENTS_INTERVAL = "deletedDocumentsInterval";

    private static final String DOCUMENTS_UNIT = "deletedDocumentsUnit";

    private static final String ATTACHMENTS_MODE = "deletedAttachmentsMode";

    private static final String ATTACHMENTS_INTERVAL = "deletedAttachmentsInterval";

    private static final String ATTACHMENTS_UNIT = "deletedAttachmentsUnit";

    private static final String MAX_HISTORY_ENTRIES = "maxHistoryEntries";

    private static final String WIKI = "wiki";

    private static final PurgeTypeSettings DISABLED_DEFAULT =
        new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings INHERIT_DEFAULT =
        new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings ENABLED_7_DAYS =
        new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS);

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PurgeSettingsMapper mapper;

    @Mock
    private BaseObject object;

    @Test
    void readGlobalSettingsWithoutObject()
    {
        GlobalPurgeSettings settings = this.mapper.readSettings(null);

        assertEquals(new GlobalPurgeSettings(settings(DISABLED_DEFAULT, DISABLED_DEFAULT), 20), settings);
    }

    @Test
    void readGlobalSettings()
    {
        mockSettings("enabled", 7, "days", "disabled", 2, "years");
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(50);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new GlobalPurgeSettings(
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.DISABLED, 2, IntervalUnit.YEARS)), 50), settings);
    }

    @Test
    void readGlobalSettingsWithMissingValues()
    {
        // A missing interval is read as the default interval, a missing unit as the default unit.
        mockSettings("enabled", 1, "", "", 1, null);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new PurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(DISABLED_DEFAULT, settings.getTypeSettings(DELETED_ATTACHMENTS));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void readGlobalSettingsWithInvalidModes()
    {
        mockSettings("inherit", 7, "days", "bogus", 7, "days");

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS),
            settings.getTypeSettings(DELETED_ATTACHMENTS));
        assertEquals("Invalid automatic purge mode [inherit] for [deletedDocuments] in the [global] settings. "
            + "Using [disabled] instead.", this.logCapture.getMessage(0));
        assertEquals("Invalid automatic purge mode [bogus] for [deletedAttachments] in the [global] settings. "
            + "Using [disabled] instead.", this.logCapture.getMessage(1));
    }

    @Test
    void readGlobalSettingsDisablesEnabledTypeWithInvalidInterval()
    {
        mockSettings("enabled", 0, "days", "enabled", 5, "decades");

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.DAYS),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(new PurgeTypeSettings(PurgeMode.DISABLED, 5, IntervalUnit.MONTHS),
            settings.getTypeSettings(DELETED_ATTACHMENTS));
        assertEquals("Invalid automatic purge interval [0 days] for [deletedDocuments] in the [global] settings. "
            + "The automatic purge is disabled for it.", this.logCapture.getMessage(0));
        assertEquals("Invalid automatic purge interval [5 decades] for [deletedAttachments] in "
            + "the [global] settings. The automatic purge is disabled for it.", this.logCapture.getMessage(1));
    }

    @Test
    void readGlobalSettingsClampsHistorySize()
    {
        mockSettings("disabled", 1, "months", "disabled", 1, "months");

        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(500);
        assertEquals(200, this.mapper.readSettings(this.object).getMaxHistoryEntries());

        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(0);
        assertEquals(1, this.mapper.readSettings(this.object).getMaxHistoryEntries());
    }

    @Test
    void readWikiSettingsWithoutObject()
    {
        assertEquals(settings(INHERIT_DEFAULT, INHERIT_DEFAULT), this.mapper.readSettings(null, WIKI_ID));
    }

    @Test
    void readWikiSettingsWithInvalidMode()
    {
        mockSettings("enabled", 7, "days", "bogus", 1, "months");

        assertEquals(settings(ENABLED_7_DAYS, INHERIT_DEFAULT), this.mapper.readSettings(this.object, WIKI_ID));
        assertEquals("Invalid automatic purge mode [bogus] for [deletedAttachments] in the [subwiki] "
            + "settings. Using [inherit] instead.", this.logCapture.getMessage(0));
    }

    @Test
    void readWikiSettingsDisablesEnabledTypeWithInvalidInterval()
    {
        // An invalid custom interval disables the purge instead of falling back to the global settings.
        mockSettings("enabled", -3, "weeks", "inherit", 1, "months");

        assertEquals(settings(new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.WEEKS), INHERIT_DEFAULT),
            this.mapper.readSettings(this.object, WIKI_ID));
        assertEquals("Invalid automatic purge interval [-3 weeks] for [deletedDocuments] in the [subwiki] "
            + "settings. The automatic purge is disabled for it.", this.logCapture.getMessage(0));
    }

    @Test
    void readWikiSettingsCapsTooLargeInterval()
    {
        mockSettings("enabled", 1001, "years", "inherit", 1, "months");

        assertEquals(settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1000, IntervalUnit.YEARS), INHERIT_DEFAULT),
            this.mapper.readSettings(this.object, WIKI_ID));
        assertEquals("The automatic purge interval [1001] of [deletedDocuments] in the [subwiki] settings is larger "
            + "than the maximum. [1000] is used instead.", this.logCapture.getMessage(0));
    }

    @Test
    void readWiki()
    {
        assertEquals(Optional.empty(), this.mapper.readWiki(this.object));

        when(this.object.getStringValue(WIKI)).thenReturn(" ");
        assertEquals(Optional.empty(), this.mapper.readWiki(this.object));

        when(this.object.getStringValue(WIKI)).thenReturn(WIKI_ID);
        assertEquals(Optional.of(WIKI_ID), this.mapper.readWiki(this.object));
    }

    @Test
    void writeGlobalSettings()
    {
        this.mapper.writeSettings(this.object, settings(ENABLED_7_DAYS, DISABLED_DEFAULT), 30);

        verify(this.object).setStringValue(DOCUMENTS_MODE, "enabled");
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(ATTACHMENTS_MODE, "disabled");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
        verify(this.object).setIntValue(MAX_HISTORY_ENTRIES, 30);
    }

    @Test
    void writeGlobalSettingsRejectsInvalidSettings()
    {
        assertInvalidGlobalSettings("The global settings of [deletedDocuments] can't inherit.",
            settings(INHERIT_DEFAULT, DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The settings of [deletedAttachments] are missing.",
            new EnumMap<>(Map.of(DELETED_DOCUMENTS, DISABLED_DEFAULT)), 20);
        assertInvalidGlobalSettings("The interval of [deletedDocuments] must be at least 1 with a unit, but was "
            + "[0 DAYS].", settings(new PurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS), DISABLED_DEFAULT),
            20);
        assertInvalidGlobalSettings("The interval of [deletedAttachments] must be at least 1 with a unit, but was "
            + "[3 null].", settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.ENABLED, 3, null)), 20);
        assertInvalidGlobalSettings("The purge history size must be at least 1, but was [0].",
            settings(DISABLED_DEFAULT, DISABLED_DEFAULT), 0);
        verifyNoInteractions(this.object);
    }

    @Test
    void writeGlobalSettingsCapsTooLargeValues()
    {
        this.mapper.writeSettings(this.object,
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1001, IntervalUnit.YEARS), DISABLED_DEFAULT), 201);

        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 1000);
        verify(this.object).setIntValue(MAX_HISTORY_ENTRIES, 200);
        assertEquals("The purge history size [201] is larger than the maximum. [200] is used instead.",
            this.logCapture.getMessage(0));
        assertEquals("The automatic purge interval [1001] of [deletedDocuments] in the [global] settings is larger "
            + "than the maximum. [1000] is used instead.", this.logCapture.getMessage(1));
    }

    @Test
    void writeWikiSettings()
    {
        // The interval and unit of an inherited type are not used, so invalid ones are replaced by the defaults.
        this.mapper.writeSettings(this.object, WIKI_ID,
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.INHERIT, 0, null)));

        verify(this.object).setStringValue(WIKI, WIKI_ID);
        verify(this.object).setStringValue(DOCUMENTS_MODE, "enabled");
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(ATTACHMENTS_MODE, "inherit");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
    }

    @Test
    void writeWikiSettingsWithDisabledTypeWithoutInterval()
    {
        // The interval and unit of a disabled type are not used either; a stored interval is kept within range.
        this.mapper.writeSettings(this.object, WIKI_ID,
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.DISABLED, 5000, null)));

        verify(this.object).setStringValue(ATTACHMENTS_MODE, "disabled");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1000);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
        assertEquals("The automatic purge interval [5000] of [deletedAttachments] in the [subwiki] settings is larger "
            + "than the maximum. [1000] is used instead.", this.logCapture.getMessage(0));
    }

    @Test
    void writeWikiSettingsRejectsInvalidSettings()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.mapper.writeSettings(this.object, WIKI_ID,
                settings(new PurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS), INHERIT_DEFAULT)));

        assertEquals("The interval of [deletedDocuments] must be at least 1 with a unit, but was [0 DAYS].",
            exception.getMessage());
        verifyNoInteractions(this.object);
    }

    private void mockSettings(String documentsMode, int documentsInterval, String documentsUnit,
        String attachmentsMode, int attachmentsInterval, String attachmentsUnit)
    {
        when(this.object.getStringValue(DOCUMENTS_MODE)).thenReturn(documentsMode);
        when(this.object.getIntValue(DOCUMENTS_INTERVAL, 1)).thenReturn(documentsInterval);
        when(this.object.getStringValue(DOCUMENTS_UNIT)).thenReturn(documentsUnit);
        when(this.object.getStringValue(ATTACHMENTS_MODE)).thenReturn(attachmentsMode);
        when(this.object.getIntValue(ATTACHMENTS_INTERVAL, 1)).thenReturn(attachmentsInterval);
        when(this.object.getStringValue(ATTACHMENTS_UNIT)).thenReturn(attachmentsUnit);
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(20);
    }

    private void assertInvalidGlobalSettings(String expectedMessage, Map<PurgeType, PurgeTypeSettings> settings,
        int maxHistoryEntries)
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.mapper.writeSettings(this.object, settings, maxHistoryEntries));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Map<PurgeType, PurgeTypeSettings> settings(PurgeTypeSettings documents,
        PurgeTypeSettings attachments)
    {
        Map<PurgeType, PurgeTypeSettings> settings = new EnumMap<>(PurgeType.class);
        settings.put(DELETED_DOCUMENTS, documents);
        settings.put(DELETED_ATTACHMENTS, attachments);
        return settings;
    }
}
