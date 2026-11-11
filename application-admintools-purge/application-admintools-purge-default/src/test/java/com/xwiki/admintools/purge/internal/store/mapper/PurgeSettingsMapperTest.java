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
import com.xpn.xwiki.objects.BaseProperty;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.PAGE_HISTORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    private static final String HISTORY_MODE = "pageHistoryMode";

    private static final String HISTORY_INTERVAL = "pageHistoryInterval";

    private static final String HISTORY_UNIT = "pageHistoryUnit";

    private static final String HISTORY_MAX_VERSIONS = "pageHistoryMaxVersions";

    private static final String MAX_HISTORY_ENTRIES = "maxHistoryEntries";

    private static final String WIKI = "wiki";

    /**
     * What {@link BaseObject#getIntValue(String, int)} returns for a missing or empty interval, the default it is
     * given.
     */
    private static final int MISSING = Integer.MIN_VALUE;

    private static final PurgeTypeSettings DISABLED_DEFAULT =
        new PurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings INHERIT_DEFAULT =
        new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    private static final PurgeTypeSettings ENABLED_7_DAYS =
        new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS);

    private static final PurgeTypeSettings HISTORY_DISABLED_DEFAULT =
        new HistoryPurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS, null);

    private static final PurgeTypeSettings HISTORY_INHERIT_DEFAULT =
        new HistoryPurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS, null);

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

        assertEquals(
            new GlobalPurgeSettings(settings(DISABLED_DEFAULT, DISABLED_DEFAULT, HISTORY_DISABLED_DEFAULT), 20),
            settings);
    }

    @Test
    void readGlobalSettings()
    {
        mockSettings("enabled", 7, "days", "disabled", 2, "years");
        // The history is limited only by its number of versions.
        mockHistorySettings("enabled", MISSING, "", 50);
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(50);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new GlobalPurgeSettings(
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.DISABLED, 2, IntervalUnit.YEARS),
                new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 50)), 50), settings);
    }

    @Test
    void readGlobalSettingsWithMissingValues()
    {
        // A missing interval or unit is read as missing, instead of the defaults. The interval is not read as 0, which
        // is valid and purges every deleted item.
        mockSettings("enabled", MISSING, "", "", MISSING, null);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, null, null)),
            settings.getTypeSettings(DELETED_DOCUMENTS));
        assertEquals(Optional.of(new PurgeTypeSettings(PurgeMode.DISABLED, null, null)),
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
        mockHistorySettings("enabled", 1, "months", 0);
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(500);

        GlobalPurgeSettings settings = this.mapper.readSettings(this.object);

        assertEquals(new GlobalPurgeSettings(settings(new PurgeTypeSettings(PurgeMode.ENABLED, -3, IntervalUnit.DAYS),
            new PurgeTypeSettings(PurgeMode.ENABLED, 5000, null),
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 1, IntervalUnit.MONTHS, 0)), 500), settings);
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
        assertEquals(settings(INHERIT_DEFAULT, INHERIT_DEFAULT, HISTORY_INHERIT_DEFAULT),
            this.mapper.readSettings(null, WIKI_ID));
    }

    @Test
    void readWikiSettingsWithInvalidMode()
    {
        mockSettings("enabled", 7, "days", "bogus", 1, "months");

        assertEquals(settings(ENABLED_7_DAYS, INHERIT_DEFAULT, HISTORY_DISABLED_DEFAULT),
            this.mapper.readSettings(this.object, WIKI_ID));
        assertEquals("Invalid automatic purge mode [bogus] for [deletedAttachments] in the [subwiki] "
            + "settings. Using [inherit] instead.", this.logCapture.getMessage(0));
    }

    @Test
    void readWikiSettingsWithInvalidValues()
    {
        // Invalid values are returned as they are stored, whatever the mode.
        mockSettings("enabled", -3, "weeks", "disabled", 1001, "bogus");

        assertEquals(settings(new PurgeTypeSettings(PurgeMode.ENABLED, -3, IntervalUnit.WEEKS),
            new PurgeTypeSettings(PurgeMode.DISABLED, 1001, null), HISTORY_DISABLED_DEFAULT),
            this.mapper.readSettings(this.object, WIKI_ID));
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
        this.mapper.writeSettings(this.object, settings(ENABLED_7_DAYS, DISABLED_DEFAULT,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 3, IntervalUnit.MONTHS, 10)), 30);

        verify(this.object).setStringValue(DOCUMENTS_MODE, "enabled");
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(ATTACHMENTS_MODE, "disabled");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
        verify(this.object).setStringValue(HISTORY_MODE, "enabled");
        verify(this.object).setIntValue(HISTORY_INTERVAL, 3);
        verify(this.object).setStringValue(HISTORY_UNIT, "months");
        verify(this.object).setIntValue(HISTORY_MAX_VERSIONS, 10);
        verify(this.object).setIntValue(MAX_HISTORY_ENTRIES, 30);
        verify(this.object, never()).safeget(anyString());
    }

    @Test
    void writeGlobalSettingsRejectsInvalidSettings()
    {
        assertInvalidGlobalSettings("The global settings of [deletedDocuments] can't inherit.",
            settings(INHERIT_DEFAULT, DISABLED_DEFAULT, HISTORY_DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The settings of [deletedAttachments] are missing.",
            new EnumMap<>(Map.of(DELETED_DOCUMENTS, DISABLED_DEFAULT)), 20);
        assertInvalidGlobalSettings("The interval of [deletedDocuments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [-1].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, -1, IntervalUnit.DAYS), DISABLED_DEFAULT,
                HISTORY_DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The interval of [deletedDocuments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [1001].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1001, IntervalUnit.YEARS), DISABLED_DEFAULT,
                HISTORY_DISABLED_DEFAULT), 20);
        // An enabled item type needs an interval and a unit.
        assertInvalidGlobalSettings("The interval of [deletedAttachments] in the [GLOBAL SETTINGS] settings is missing.",
            settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.ENABLED, null, IntervalUnit.DAYS),
                HISTORY_DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The interval unit of [deletedAttachments] in the [GLOBAL SETTINGS] settings is "
            + "missing.", settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.ENABLED, 3, null),
                HISTORY_DISABLED_DEFAULT), 20);
        // The given interval of a disabled type is validated too.
        assertInvalidGlobalSettings("The interval of [deletedAttachments] in the [GLOBAL SETTINGS] settings must be "
            + "between 0 and 1000, but was [-1].",
            settings(DISABLED_DEFAULT, new PurgeTypeSettings(PurgeMode.DISABLED, -1, IntervalUnit.MONTHS),
                HISTORY_DISABLED_DEFAULT), 20);
        assertInvalidGlobalSettings("The purge history size must be between 1 and 200, but was [0].",
            settings(DISABLED_DEFAULT, DISABLED_DEFAULT, HISTORY_DISABLED_DEFAULT), 0);
        assertInvalidGlobalSettings("The purge history size must be between 1 and 200, but was [201].",
            settings(DISABLED_DEFAULT, DISABLED_DEFAULT, HISTORY_DISABLED_DEFAULT), 201);
        // An enabled history needs an age limit, a number of versions, or both; a given age limit must be complete.
        assertInvalidHistorySettings("The interval of [pageHistory] in the [GLOBAL SETTINGS] settings is missing.",
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, null));
        assertInvalidHistorySettings("The interval unit of [pageHistory] in the [GLOBAL SETTINGS] settings is "
            + "missing.", new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 3, null, 10));
        assertInvalidHistorySettings("The number of versions of [pageHistory] in the [GLOBAL SETTINGS] settings must "
            + "be between 1 and 1000, but was [0].", new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 0));
        // The given number of versions of a disabled history is validated too.
        assertInvalidHistorySettings("The number of versions of [pageHistory] in the [GLOBAL SETTINGS] settings must "
            + "be between 1 and 1000, but was [1001].",
            new HistoryPurgeTypeSettings(PurgeMode.DISABLED, 1, IntervalUnit.MONTHS, 1001));
        verifyNoInteractions(this.object);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void writeGlobalSettingsWithLimitValues()
    {
        // An interval of 0 is valid: it purges every item deleted until the purge runs.
        this.mapper.writeSettings(this.object,
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, 1000, IntervalUnit.YEARS),
                new PurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS),
                new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS, 1)), 200);

        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 1000);
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 0);
        verify(this.object).setIntValue(HISTORY_INTERVAL, 0);
        verify(this.object).setIntValue(HISTORY_MAX_VERSIONS, 1);
        verify(this.object).setIntValue(MAX_HISTORY_ENTRIES, 200);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void writeGlobalSettingsClearsMissingLimitOfEnabledType()
    {
        BaseProperty<?> interval = mockProperty(HISTORY_INTERVAL);
        BaseProperty<?> unit = mockProperty(HISTORY_UNIT);
        BaseProperty<?> maxVersions = mockProperty(HISTORY_MAX_VERSIONS);

        // The limit of an enabled item type is written exactly: the age limit left out is cleared, so that it no
        // longer applies. The properties are emptied, not removed, so that the object can be saved. The largest
        // number of versions is valid.
        this.mapper.writeSettings(this.object, settings(ENABLED_7_DAYS, DISABLED_DEFAULT,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 1000)), 20);

        verify(this.object).setStringValue(HISTORY_MODE, "enabled");
        verify(this.object).setIntValue(HISTORY_MAX_VERSIONS, 1000);
        verify(interval).setValue(null);
        verify(unit).setValue(null);
        verify(this.object, never()).setIntValue(eq(HISTORY_INTERVAL), anyInt());
        verify(this.object, never()).setStringValue(eq(HISTORY_UNIT), any());
        verify(this.object, never()).removeField(anyString());

        // The number of versions left out is cleared too. A value that is not stored has nothing to clear.
        when(this.object.safeget(HISTORY_INTERVAL)).thenReturn(null);
        this.mapper.writeSettings(this.object, settings(ENABLED_7_DAYS, DISABLED_DEFAULT,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 2, IntervalUnit.WEEKS, null)), 20);

        verify(maxVersions).setValue(null);
    }

    @Test
    void writeWikiSettings()
    {
        this.mapper.writeSettings(this.object, WIKI_ID,
            settings(ENABLED_7_DAYS, INHERIT_DEFAULT, HISTORY_INHERIT_DEFAULT));

        verify(this.object).setStringValue(WIKI, WIKI_ID);
        verify(this.object).setStringValue(DOCUMENTS_MODE, "enabled");
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(ATTACHMENTS_MODE, "inherit");
        verify(this.object).setIntValue(ATTACHMENTS_INTERVAL, 1);
        verify(this.object).setStringValue(ATTACHMENTS_UNIT, "months");
        verify(this.object).setStringValue(HISTORY_MODE, "inherit");
        verify(this.object).setIntValue(HISTORY_INTERVAL, 1);
        verify(this.object).setStringValue(HISTORY_UNIT, "months");
        // The number of versions left out of an item type that is not enabled keeps the stored one.
        verify(this.object, never()).setIntValue(eq(HISTORY_MAX_VERSIONS), anyInt());
        verify(this.object, never()).safeget(anyString());
    }

    @Test
    void writeWikiSettingsWithoutLimit()
    {
        // An item type that is not enabled can leave its interval and unit out, which keeps the stored ones.
        this.mapper.writeSettings(this.object, WIKI_ID,
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.INHERIT, null, null),
                new HistoryPurgeTypeSettings(PurgeMode.DISABLED, null, null, null)));

        verify(this.object).setStringValue(ATTACHMENTS_MODE, "inherit");
        verify(this.object, never()).setIntValue(eq(ATTACHMENTS_INTERVAL), anyInt());
        verify(this.object, never()).setStringValue(eq(ATTACHMENTS_UNIT), any());
        verify(this.object).setIntValue(DOCUMENTS_INTERVAL, 7);
        verify(this.object).setStringValue(DOCUMENTS_UNIT, "days");
        verify(this.object).setStringValue(HISTORY_MODE, "disabled");
        verify(this.object, never()).setIntValue(eq(HISTORY_MAX_VERSIONS), anyInt());
        verify(this.object, never()).safeget(anyString());
    }

    @Test
    void writeWikiSettingsRejectsInvalidSettings()
    {
        assertInvalidWikiSettings("The interval of [deletedDocuments] in the [subwiki] settings must be between 0 and "
            + "1000, but was [-1].",
            settings(new PurgeTypeSettings(PurgeMode.ENABLED, -1, IntervalUnit.DAYS), INHERIT_DEFAULT,
                HISTORY_INHERIT_DEFAULT));
        // The given interval of an inherited or disabled type is validated too.
        assertInvalidWikiSettings("The interval of [deletedAttachments] in the [subwiki] settings must be between 0 "
            + "and 1000, but was [-1].",
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.INHERIT, -1, IntervalUnit.MONTHS),
                HISTORY_INHERIT_DEFAULT));
        assertInvalidWikiSettings("The interval of [deletedAttachments] in the [subwiki] settings must be between 0 "
            + "and 1000, but was [5000].",
            settings(ENABLED_7_DAYS, new PurgeTypeSettings(PurgeMode.DISABLED, 5000, IntervalUnit.MONTHS),
                HISTORY_INHERIT_DEFAULT));
        verifyNoInteractions(this.object);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void checkLimit()
    {
        // A manual limit follows the rules of an enabled item type, whatever its mode: an interval of 0 purges
        // everything until now, and a history can be limited by its number of versions only.
        this.mapper.checkLimit(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.DISABLED, 0, IntervalUnit.DAYS));
        this.mapper.checkLimit(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 1000, IntervalUnit.YEARS));
        this.mapper.checkLimit(PAGE_HISTORY, new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 1));
        this.mapper.checkLimit(PAGE_HISTORY,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, 0, IntervalUnit.DAYS, 1000));

        assertInvalidLimit("The interval of [deletedDocuments] in the [MANUAL PURGE] settings is missing.",
            DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, null, IntervalUnit.DAYS));
        assertInvalidLimit("The interval of [deletedAttachments] in the [MANUAL PURGE] settings must be between 0 "
            + "and 1000, but was [1001].", DELETED_ATTACHMENTS,
            new PurgeTypeSettings(PurgeMode.ENABLED, 1001, IntervalUnit.YEARS));
        assertInvalidLimit("The interval of [pageHistory] in the [MANUAL PURGE] settings is missing.", PAGE_HISTORY,
            new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, null));
        assertInvalidLimit("The number of versions of [pageHistory] in the [MANUAL PURGE] settings must be between 1 "
            + "and 1000, but was [0].", PAGE_HISTORY, new HistoryPurgeTypeSettings(PurgeMode.ENABLED, null, null, 0));
    }

    private void mockSettings(String documentsMode, int documentsInterval, String documentsUnit, String attachmentsMode,
        int attachmentsInterval, String attachmentsUnit)
    {
        when(this.object.getStringValue(DOCUMENTS_MODE)).thenReturn(documentsMode);
        when(this.object.getIntValue(DOCUMENTS_INTERVAL, MISSING)).thenReturn(documentsInterval);
        when(this.object.getStringValue(DOCUMENTS_UNIT)).thenReturn(documentsUnit);
        when(this.object.getStringValue(ATTACHMENTS_MODE)).thenReturn(attachmentsMode);
        when(this.object.getIntValue(ATTACHMENTS_INTERVAL, MISSING)).thenReturn(attachmentsInterval);
        when(this.object.getStringValue(ATTACHMENTS_UNIT)).thenReturn(attachmentsUnit);
        when(this.object.getIntValue(MAX_HISTORY_ENTRIES, 20)).thenReturn(20);
        mockHistorySettings("disabled", 1, "months", MISSING);
    }

    private BaseProperty<?> mockProperty(String name)
    {
        BaseProperty<?> property = mock(BaseProperty.class);
        when(this.object.safeget(name)).thenReturn(property);
        return property;
    }

    private void mockHistorySettings(String mode, int interval, String unit, int maxVersions)
    {
        when(this.object.getStringValue(HISTORY_MODE)).thenReturn(mode);
        when(this.object.getIntValue(HISTORY_INTERVAL, MISSING)).thenReturn(interval);
        when(this.object.getStringValue(HISTORY_UNIT)).thenReturn(unit);
        when(this.object.getIntValue(HISTORY_MAX_VERSIONS, MISSING)).thenReturn(maxVersions);
    }

    private void assertInvalidGlobalSettings(String expectedMessage, Map<PurgeType, PurgeTypeSettings> settings,
        int maxHistoryEntries)
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.mapper.writeSettings(this.object, settings, maxHistoryEntries));
        assertEquals(expectedMessage, exception.getMessage());
    }

    /**
     * Check that global settings whose only invalid part is the history are rejected.
     *
     * @param expectedMessage the expected error
     * @param history the settings of the history
     */
    private void assertInvalidHistorySettings(String expectedMessage, PurgeTypeSettings history)
    {
        assertInvalidGlobalSettings(expectedMessage, settings(DISABLED_DEFAULT, DISABLED_DEFAULT, history), 20);
    }

    private void assertInvalidLimit(String expectedMessage, PurgeType type, PurgeTypeSettings limit)
    {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.mapper.checkLimit(type, limit));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private void assertInvalidWikiSettings(String expectedMessage, Map<PurgeType, PurgeTypeSettings> settings)
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.mapper.writeSettings(this.object, WIKI_ID, settings));
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Map<PurgeType, PurgeTypeSettings> settings(PurgeTypeSettings documents,
        PurgeTypeSettings attachments, PurgeTypeSettings history)
    {
        Map<PurgeType, PurgeTypeSettings> settings = new EnumMap<>(PurgeType.class);
        settings.put(DELETED_DOCUMENTS, documents);
        settings.put(DELETED_ATTACHMENTS, attachments);
        settings.put(PAGE_HISTORY, history);
        return settings;
    }
}
