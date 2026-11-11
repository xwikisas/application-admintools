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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Converts between the settings objects stored in the wiki and the automatic purge settings. Writing is strict: every
 * value written is validated, and nothing is written if a value is invalid. Only an enabled item type needs a limit:
 * an interval and a unit, or, for a history item type, a number of versions. The limit of an enabled item type is
 * written exactly as given, so that a limit left out is cleared; an item type that is not enabled keeps its stored
 * values when none are given.
 * Reading doesn't validate: the stored values are returned as they are, so that invalid ones, which can only come
 * from manual edits, are reported by the code using them instead of being silently replaced.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeSettingsMapper.class)
@Singleton
public class PurgeSettingsMapper
{
    /**
     * The largest interval, high enough for any real limit and low enough to keep the date computations valid. It is
     * also the largest limit of a manual purge.
     */
    public static final int MAX_INTERVAL = 1000;

    /**
     * The smallest number of versions kept by a history item type: 1 keeps only the current version.
     */
    private static final int MIN_VERSIONS = 1;

    /**
     * The largest number of versions kept by a history item type.
     */
    private static final int MAX_VERSIONS = 1000;

    private static final int DEFAULT_INTERVAL = 1;

    /**
     * The smallest interval. An interval of 0 purges every item deleted until the purge runs.
     */
    private static final int MIN_INTERVAL = 0;

    private static final IntervalUnit DEFAULT_UNIT = IntervalUnit.MONTHS;

    private static final int DEFAULT_MAX_HISTORY_ENTRIES = 20;

    private static final int MIN_HISTORY_ENTRIES = 1;

    private static final int MAX_HISTORY_ENTRIES = 200;

    private static final String MODE_SUFFIX = "Mode";

    private static final String INTERVAL_SUFFIX = "Interval";

    private static final String UNIT_SUFFIX = "Unit";

    private static final String MAX_VERSIONS_SUFFIX = "MaxVersions";

    private static final String MAX_HISTORY_ENTRIES_PROPERTY = "maxHistoryEntries";

    private static final String WIKI_PROPERTY = "wiki";

    /**
     * Names the global settings in warnings, where the settings of a wiki are named by the wiki identifier. It is only
     * a name: whether settings are global never depends on it.
     */
    private static final String GLOBAL_SETTINGS = "GLOBAL SETTINGS";

    /**
     * Names the limit of a manual purge in errors.
     */
    private static final String MANUAL_PURGE = "MANUAL PURGE";

    @Inject
    private Logger logger;

    /**
     * Read the global settings from the global settings object. Besides the settings of each item type, the global
     * object holds the purge history size.
     *
     * @param object the global settings object, or {@code null} if there is none
     * @return the global settings, with the stored values; every item type is disabled if there is no object
     * @throws IllegalArgumentException if the stored history size is lower than 1
     */
    public GlobalPurgeSettings readSettings(BaseObject object) throws IllegalArgumentException
    {
        Map<PurgeType, PurgeTypeSettings> typeSettings = readTypeSettings(object, true, GLOBAL_SETTINGS);
        if (object == null) {
            return new GlobalPurgeSettings(typeSettings, DEFAULT_MAX_HISTORY_ENTRIES);
        }
        return new GlobalPurgeSettings(typeSettings,
            object.getIntValue(MAX_HISTORY_ENTRIES_PROPERTY, DEFAULT_MAX_HISTORY_ENTRIES));
    }

    /**
     * Read the settings of a wiki from its settings object.
     *
     * @param object the settings object of the wiki, or {@code null} if there is none
     * @param wikiId the identifier of the wiki, used in warnings
     * @return the settings of each item type for the wiki, with the stored values; every item type is
     *     {@link PurgeMode#INHERIT} if there is no object
     */
    public Map<PurgeType, PurgeTypeSettings> readSettings(BaseObject object, String wikiId)
    {
        return readTypeSettings(object, false, wikiId);
    }

    /**
     * Validate and write the global settings to the global settings object. Nothing is written if the settings are
     * invalid. The page holding the object still needs to be saved.
     *
     * @param object the global settings object to write to
     * @param typeSettings the global settings of every item type; the mode can't be {@link PurgeMode#INHERIT}
     * @param maxHistoryEntries the number of purge history entries kept for each item type of each wiki, between 1
     *     and 200
     * @throws IllegalArgumentException if a setting is missing or invalid, if a mode is {@link PurgeMode#INHERIT},
     *     or if the history size is not between 1 and 200
     */
    public void writeSettings(BaseObject object, Map<PurgeType, PurgeTypeSettings> typeSettings, int maxHistoryEntries)
        throws IllegalArgumentException
    {
        validate(typeSettings, true, GLOBAL_SETTINGS);
        checkMaxHistoryEntries(maxHistoryEntries);
        writeTypeSettings(object, typeSettings);
        object.setIntValue(MAX_HISTORY_ENTRIES_PROPERTY, maxHistoryEntries);
    }

    /**
     * Validate and write the settings of a wiki to its settings object. Nothing is written if the settings are invalid.
     * The page holding the object still needs to be saved.
     *
     * @param object the settings object of the wiki to write to
     * @param wikiId the identifier of the wiki, stored on the object
     * @param typeSettings the settings of every item type for the wiki
     * @throws IllegalArgumentException if a setting is missing or invalid
     */
    public void writeSettings(BaseObject object, String wikiId, Map<PurgeType, PurgeTypeSettings> typeSettings)
        throws IllegalArgumentException
    {
        validate(typeSettings, false, wikiId);
        object.setStringValue(WIKI_PROPERTY, wikiId);
        writeTypeSettings(object, typeSettings);
    }

    /**
     * Read which settings a settings object holds. The global settings and the settings of every customized wiki are
     * objects of the same class on the same page, told apart by this value.
     *
     * @param object a settings object
     * @return the identifier of the wiki whose settings the object holds, or empty for the global settings
     */
    public Optional<String> readWiki(BaseObject object)
    {
        return Optional.ofNullable(StringUtils.trimToNull(object.getStringValue(WIKI_PROPERTY)));
    }

    /**
     * Read the settings of every item type from a settings object, without validating them. The global and the wiki
     * settings objects are read the same way; they only differ by the mode used when the stored one is missing or
     * invalid. A missing interval or number of versions, and a missing or unknown unit, are read as missing values.
     *
     * @param object the settings object, or {@code null} if there is none, in which case every item type gets the
     *     fallback mode and the default interval and unit
     * @param global {@code true} for the global settings object, {@code false} for the settings object of a wiki
     * @param settingsName the name of the settings in warnings: the wiki identifier, or {@link #GLOBAL_SETTINGS}
     * @return the settings of each item type, as {@link HistoryPurgeTypeSettings} for the history item types
     */
    private Map<PurgeType, PurgeTypeSettings> readTypeSettings(BaseObject object, boolean global, String settingsName)
    {
        Map<PurgeType, PurgeTypeSettings> typeSettings = new EnumMap<>(PurgeType.class);
        for (PurgeType type : PurgeType.values()) {
            if (object == null) {
                typeSettings.put(type, createSettings(type, parseMode(type, null, global, settingsName),
                    DEFAULT_INTERVAL, DEFAULT_UNIT, null));
            } else {
                typeSettings.put(type, createSettings(type,
                    parseMode(type, object.getStringValue(type.getId() + MODE_SUFFIX), global, settingsName),
                    readNumber(object, type.getId() + INTERVAL_SUFFIX),
                    IntervalUnit.fromId(object.getStringValue(type.getId() + UNIT_SUFFIX)).orElse(null),
                    readNumber(object, type.getId() + MAX_VERSIONS_SUFFIX)));
            }
        }
        return typeSettings;
    }

    /**
     * @param type the item type
     * @param mode how the automatic purge is configured for the item type
     * @param interval the interval, or {@code null} if it is missing
     * @param unit the unit of the interval, or {@code null} if it is missing
     * @param maxVersions the number of versions kept, or {@code null} if it is missing; ignored for an item type that
     *     is not a history
     * @return the settings of the item type, as {@link HistoryPurgeTypeSettings} for a history item type
     */
    private PurgeTypeSettings createSettings(PurgeType type, PurgeMode mode, Integer interval, IntervalUnit unit,
        Integer maxVersions)
    {
        return type.isHistory() ? new HistoryPurgeTypeSettings(mode, interval, unit, maxVersions)
            : new PurgeTypeSettings(mode, interval, unit);
    }

    /**
     * Read a stored number. A missing or empty number is read as missing, not as 0, which is a valid interval that
     * purges every deleted item.
     *
     * @param object the settings object
     * @param property the name of the number property
     * @return the stored number, or {@code null} if it is missing or empty
     */
    private Integer readNumber(BaseObject object, String property)
    {
        // The default is returned for a missing property and for an empty one; stored, it would be invalid anyway.
        int number = object.getIntValue(property, Integer.MIN_VALUE);
        return number == Integer.MIN_VALUE ? null : number;
    }

    /**
     * Parse a stored mode. A missing mode silently falls back to {@link PurgeMode#DISABLED} for the global settings and
     * to {@link PurgeMode#INHERIT} for a wiki. An unknown mode, or {@link PurgeMode#INHERIT} in the global settings,
     * falls back the same way, with a warning.
     *
     * @param type the item type, used in warnings
     * @param modeId the stored mode, possibly empty or invalid
     * @param global {@code true} for the global settings, {@code false} for the settings of a wiki
     * @param settingsName the name of the settings in warnings: the wiki identifier, or {@link #GLOBAL_SETTINGS}
     * @return the mode
     */
    private PurgeMode parseMode(PurgeType type, String modeId, boolean global, String settingsName)
    {
        PurgeMode fallback = global ? PurgeMode.DISABLED : PurgeMode.INHERIT;
        if (StringUtils.isBlank(modeId)) {
            return fallback;
        }
        Optional<PurgeMode> mode = PurgeMode.fromId(modeId);
        if (mode.isEmpty() || (global && mode.get() == PurgeMode.INHERIT)) {
            this.logger.warn("Invalid automatic purge mode [{}] for [{}] in the [{}] settings. Using [{}] instead.",
                modeId, type.getId(), settingsName, fallback.getId());
            return fallback;
        }
        return mode.get();
    }

    /**
     * Check the settings of every item type before they are written. Every item type must have settings, the global
     * settings can't inherit, and the limit of every item type must pass {@link #checkLimit}.
     *
     * @param typeSettings the settings of every item type
     * @param global {@code true} for the global settings, {@code false} for the settings of a wiki
     * @param settingsName the name of the settings in errors: the wiki identifier, or {@link #GLOBAL_SETTINGS}
     * @throws IllegalArgumentException if the settings are invalid
     */
    private void validate(Map<PurgeType, PurgeTypeSettings> typeSettings, boolean global, String settingsName)
    {
        for (PurgeType type : PurgeType.values()) {
            PurgeTypeSettings settings = typeSettings.get(type);
            if (settings == null || settings.getMode() == null) {
                throw new IllegalArgumentException(String.format("The settings of [%s] are missing.", type.getId()));
            }
            if (global && settings.getMode() == PurgeMode.INHERIT) {
                throw new IllegalArgumentException(
                    String.format("The global settings of [%s] can't inherit.", type.getId()));
            }
            checkLimit(type, settings, settings.getMode() == PurgeMode.ENABLED, settingsName);
        }
    }

    /**
     * Check the limit of a manual purge, which follows the rules of the limit of an enabled item type, see
     * {@link #writeSettings(BaseObject, String, Map)}: an interval between 0 and 1000, which purges every item deleted
     * until now, with a unit, and for a history item type, a number of versions between 1 and 1000 instead, or as well.
     *
     * @param type the item type
     * @param limit the limit chosen by the user, whose mode is ignored
     * @throws IllegalArgumentException if the limit is invalid
     */
    public void checkLimit(PurgeType type, PurgeTypeSettings limit) throws IllegalArgumentException
    {
        checkLimit(type, limit, true, MANUAL_PURGE);
    }

    /**
     * Check the limit of an item type before it is written or used. An enabled item type needs an interval and a unit,
     * unless it is a history item type limited by a number of versions; an enabled history item type that has both
     * limits needs both an interval and a unit too. An item type that is not enabled can leave them out, to keep its
     * stored ones. A given interval must be between {@link #MIN_INTERVAL} and {@link #MAX_INTERVAL}, and a given number
     * of versions between {@link #MIN_VERSIONS} and {@link #MAX_VERSIONS}, whatever the mode, so that no invalid value
     * is written. A given unit is always valid.
     *
     * @param type the item type, used in errors
     * @param settings the settings of the item type
     * @param enabled {@code true} if the item type is enabled, or if the settings are the limit of a manual purge
     * @param settingsName the name of the settings in errors: the wiki identifier, {@link #GLOBAL_SETTINGS} or
     *     {@link #MANUAL_PURGE}
     * @throws IllegalArgumentException if the limit is invalid
     */
    private void checkLimit(PurgeType type, PurgeTypeSettings settings, boolean enabled, String settingsName)
    {
        Integer maxVersions = getMaxVersions(settings).orElse(null);
        boolean ageLimitGiven = settings.getInterval().isPresent() || settings.getUnit().isPresent();
        if (enabled && (maxVersions == null || ageLimitGiven)) {
            checkAgeLimit(type, settings, settingsName);
        }
        checkRange(type, "interval", settings.getInterval().orElse(null), MIN_INTERVAL, MAX_INTERVAL, settingsName);
        checkRange(type, "number of versions", maxVersions, MIN_VERSIONS, MAX_VERSIONS, settingsName);
    }

    /**
     * Check that an age limit has both its interval and its unit.
     *
     * @param type the item type, used in errors
     * @param settings the settings of the item type
     * @param settingsName the name of the settings in errors
     * @throws IllegalArgumentException if the interval or the unit is missing
     */
    private void checkAgeLimit(PurgeType type, PurgeTypeSettings settings, String settingsName)
    {
        if (settings.getInterval().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("The interval of [%s] in the [%s] settings is missing.", type.getId(), settingsName));
        }
        if (settings.getUnit().isEmpty()) {
            throw new IllegalArgumentException(
                String.format("The interval unit of [%s] in the [%s] settings is missing.", type.getId(),
                    settingsName));
        }
    }

    /**
     * Check that a number of a limit is in its range.
     *
     * @param type the item type, used in errors
     * @param name the name of the number, used in errors
     * @param value the number, or {@code null} if it is missing, in which case it is not checked
     * @param min the smallest valid value
     * @param max the largest valid value
     * @param settingsName the name of the settings in errors
     * @throws IllegalArgumentException if the number is out of its range
     */
    private void checkRange(PurgeType type, String name, Integer value, int min, int max, String settingsName)
    {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException(
                String.format("The %s of [%s] in the [%s] settings must be between %d and %d, but was [%d].", name,
                    type.getId(), settingsName, min, max, value));
        }
    }

    /**
     * @param settings the settings of an item type
     * @return the number of versions kept, or an empty value if the settings don't limit it
     */
    private Optional<Integer> getMaxVersions(PurgeTypeSettings settings)
    {
        return settings instanceof HistoryPurgeTypeSettings ? ((HistoryPurgeTypeSettings) settings).getMaxVersions()
            : Optional.empty();
    }

    /**
     * Check the purge history size before it is written.
     *
     * @param maxHistoryEntries the number of purge history entries kept for each item type of each wiki
     * @throws IllegalArgumentException if the history size is not between {@link #MIN_HISTORY_ENTRIES} and
     *     {@link #MAX_HISTORY_ENTRIES}
     */
    private void checkMaxHistoryEntries(int maxHistoryEntries)
    {
        if (maxHistoryEntries < MIN_HISTORY_ENTRIES || maxHistoryEntries > MAX_HISTORY_ENTRIES) {
            throw new IllegalArgumentException(
                String.format("The purge history size must be between %d and %d, but was [%d].", MIN_HISTORY_ENTRIES,
                    MAX_HISTORY_ENTRIES, maxHistoryEntries));
        }
    }

    /**
     * Write the mode and the limit of every item type to a settings object. The limit of an enabled item type is
     * written exactly as given: a missing interval, unit or number of versions is cleared, so that a limit left out no
     * longer applies. For an item type that is not enabled, a missing value leaves the stored one unchanged.
     *
     * @param object the settings object to write to
     * @param typeSettings the validated settings of every item type
     */
    private void writeTypeSettings(BaseObject object, Map<PurgeType, PurgeTypeSettings> typeSettings)
    {
        for (PurgeType type : PurgeType.values()) {
            PurgeTypeSettings settings = typeSettings.get(type);
            boolean enabled = settings.getMode() == PurgeMode.ENABLED;
            object.setStringValue(type.getId() + MODE_SUFFIX, settings.getMode().getId());
            writeNumber(object, type.getId() + INTERVAL_SUFFIX, settings.getInterval().orElse(null), enabled);
            String unitProperty = type.getId() + UNIT_SUFFIX;
            if (settings.getUnit().isPresent()) {
                object.setStringValue(unitProperty, settings.getUnit().get().getId());
            } else if (enabled) {
                clearValue(object, unitProperty);
            }
            if (type.isHistory()) {
                writeNumber(object, type.getId() + MAX_VERSIONS_SUFFIX, getMaxVersions(settings).orElse(null),
                    enabled);
            }
        }
    }

    /**
     * Write a number of the limit of an item type.
     *
     * @param object the settings object to write to
     * @param property the name of the number property
     * @param value the number, or {@code null} if it is missing
     * @param clearMissing {@code true} to clear the stored number when it is missing, {@code false} to keep it
     */
    private void writeNumber(BaseObject object, String property, Integer value, boolean clearMissing)
    {
        if (value != null) {
            object.setIntValue(property, value);
        } else if (clearMissing) {
            clearValue(object, property);
        }
    }

    /**
     * Clear a stored value, so that it is read as missing. The property is emptied instead of removed: when the
     * object is saved, the store adds an empty property for each property of the class that the object lacks, which
     * would conflict with the deletion of the removed one.
     *
     * @param object the settings object
     * @param property the name of the property
     */
    private void clearValue(BaseObject object, String property)
    {
        BaseProperty<?> field = (BaseProperty<?>) object.safeget(property);
        if (field != null) {
            field.setValue(null);
        }
    }
}
