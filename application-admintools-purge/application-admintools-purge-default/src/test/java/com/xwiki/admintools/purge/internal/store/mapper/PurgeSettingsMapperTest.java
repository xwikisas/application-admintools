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
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
        // A missing interval is read as -1 and a missing unit as null, both invalid, instead of the defaults. The
        // interval is not read as 0, which is valid and purges every deleted item.
        mockSettings("enabled", -1, "", "", -1, null);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, -1, null)),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, -1, null)),
            settings.getTypeSettings(DELETED_ATTACHMENTS));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void readGlobalSettingsWithInvalidModes()
    {
        mockSettings("inherit", 7, "days", "bogus", 7, "days");

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS)),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, 7, IntervalUnit.DAYS)),
            settings.getTypeSettings(DELETED_ATTACHMENTS));
        assertEquals("Invalid automatic purge mode [inherit] for [deletedDocuments] in the [GLOBAL SETTINGS] settings. "
            + "Using [disabled] instead.", this.logCapture.getMessage(0));
        assertEquals("Invalid automatic purge mode [bogus] for [deletedAttachments] in the [GLOBAL SETTINGS] settings. "
            + "Using [disabled] instead.", this.logCapture.getMessage(1));
    }

    @Test
    void readGlobalSettingsWithInvalidValues()
    {
        // Invalid values are returned as they are stored, so that the code using them reports them.
        mockSettings("enabled", -3, "days", "enabled", 5000, "decades");
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(500);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new GlobalPurgeSettings(settings(new PurgeTypeSettings(PurgeMode.ENABLED, -3, IntervalUnit.DAYS),
            new PurgeTypeSettings(PurgeMode.ENABLED, 5000, null)), 500), settings);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void readGlobalSettingsWithHistorySizeLowerThanOne()
    {
        mockSettings("disabled", 1, "months", "disabled", 1, "months");
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(0);

        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.mapper.readSettings(this.object));

        assertEquals("The purge history size must be at least 1, but was [0].", exception.getMessage());
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
    void readWikiSettingsWithInvalidValues()
    {
        // Invalid values are returned as they are stored, whatever the mode.
        mockSettings("enabled", -3, "weeks", "disabled", 1001, "bogus");

        assertEquals(settings(new PurgeTypeSettings(PurgeMode.ENABLED, -3, IntervalUnit.WEEKS),
            new PurgeTypeSettings(PurgeMode.DISABLED, 1001, null)), this.mapper.readSettings(this.object, WIKI_ID));
        assertEquals(0, this.logCapture.size());
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
        assertInvalidGlobalSettings("The interval of [deletedDocuments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [-1].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, -1, IntervalUnit.DAYS), DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The interval of [deletedDocuments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [1001].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1001, IntervalUnit.YEARS), DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The interval unit of [deletedAttachments] in the [GLOBAL SETTINGS] settings is "
            + "missing.", settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.ENABLED, 3, null)), 20);
        // The interval and unit of a disabled type are validated too.
        assertInvalidGlobalSettings("The interval of [deletedAttachments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [-1].",
            settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.DISABLED, -1, IntervalUnit.MONTHS)), 20);
        assertInvalidGlobalSettings("The interval unit of [deletedAttachments] in the [GLOBAL SETTINGS] settings is "
            + "missing.", settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.DISABLED, 1, null)), 20);
        assertInvalidGlobalSettings("The purge history size must be between 1 and 200, but was [0].",
            settings(DISABLED_DEFAULT, DISABLED_DEFAULT), 0);
        assertInvalidGlobalSettings("The purge history size must be between 1 and 200, but was [201].",
            settings(DISABLED_DEFAULT, DISABLED_DEFAULT), 201);
        verifyNoInteractions(this.object);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void writeGlobalSettingsWithLimitValues()
    {
        // An interval of 0 is valid: it purges every item deleted until the purge runs.
        this.mapper.writeSettings(this.object,
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1000, IntervalUnit.YEARS),
                new PurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS)), 200);

        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 1000);
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 0);
        verify(this.object).setIntValue(MAX_HISTORY_ENTRIES, 200);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void writeWikiSettings()
    {
        this.mapper.writeSettings(this.object, WIKI_ID, settings(ENABLED_7_DAYS, INHERIT_DEFAULT));

        verify(this.object).setStringValue(WIKI, WIKI_ID);
        verify(this.object).setStringValue(DOCUMENTS_MODE, "enabled");
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(ATTACHMENTS_MODE, "inherit");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
    }

    @Test
    void writeWikiSettingsRejectsInvalidSettings()
    {
        assertInvalidWikiSettings("The interval of [deletedDocuments] in the [subwiki] settings must be between 0 and "
            + "1000, but was [-1].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, -1, IntervalUnit.DAYS), INHERIT_DEFAULT));
        // The interval and unit of an inherited or disabled type are validated too.
        assertInvalidWikiSettings("The interval of [deletedAttachments] in the [subwiki] settings must be between 0 "
            + "and 1000, but was [-1].",
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.INHERIT, -1, IntervalUnit.MONTHS)));
        assertInvalidWikiSettings("The interval unit of [deletedAttachments] in the [subwiki] settings is missing.",
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.INHERIT, 1, null)));
        assertInvalidWikiSettings("The interval of [deletedAttachments] in the [subwiki] settings must be between 0 "
            + "and 1000, but was [5000].",
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.DISABLED, 5000, IntervalUnit.MONTHS)));
        verifyNoInteractions(this.object);
        assertEquals(0, this.logCapture.size());
    }

    private void mockSettings(String documentsMode, int documentsInterval, String documentsUnit, String attachmentsMode,
        int attachmentsInterval, String attachmentsUnit)
    {
        when(this.object.getStringValue(DOCUMENTS_MODE)).thenReturn(documentsMode);
        when(this.object.getIntValue(DOCUMENTS_INTERVAL, -1)).thenReturn(documentsInterval);
        when(this.object.getStringValue(DOCUMENTS_UNIT)).thenReturn(documentsUnit);
        when(this.object.getStringValue(ATTACHMENTS_MODE)).thenReturn(attachmentsMode);
        when(this.object.getIntValue(ATTACHMENTS_INTERVAL, -1)).thenReturn(attachmentsInterval);
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

    private void assertInvalidWikiSettings(String expectedMessage, Map<PurgeType, PurgeTypeSettings> settings)
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.mapper.writeSettings(this.object, WIKI_ID, settings));
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
