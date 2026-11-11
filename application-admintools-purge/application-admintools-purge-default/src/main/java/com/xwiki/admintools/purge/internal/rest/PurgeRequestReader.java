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

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.purge.model.HistoryPurgeTypeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;
import com.xwiki.admintools.purge.rest.AutoPurgeResource;

/**
 * Reads the automatic purge settings and limits from the fields of the requests of {@link AutoPurgeResource}, and
 * describes a limit with the fields of its responses. The values are only parsed here: their ranges are checked by the
 * settings store and the purge manager.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeRequestReader.class)
@Singleton
public class PurgeRequestReader
{
    private static final String TYPE = "type";

    private static final String INTERVAL_SUFFIX = "Interval";

    private static final String UNIT_SUFFIX = "Unit";

    private static final String MAX_VERSIONS_SUFFIX = "MaxVersions";

    private static final String INVALID_VALUE = "Invalid value [%s] for [%s].";

    /**
     * Read the item types of the repeatable field {@code type}.
     *
     * @param request the request holding the field
     * @return the item types, each one once, or an empty set if the field is missing
     * @throws IllegalArgumentException if an item type is unknown
     */
    public Set<PurgeType> readTypes(XWikiRequest request)
    {
        Set<PurgeType> types = EnumSet.noneOf(PurgeType.class);
        String[] typeIds = request.getParameterValues(TYPE);
        if (typeIds != null) {
            for (String typeId : typeIds) {
                types.add(PurgeType.fromId(typeId)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, typeId, TYPE))));
            }
        }
        return types;
    }

    /**
     * Read the automatic purge settings of every item type from the form fields {@code <typeId>Mode},
     * {@code <typeId>Interval}, {@code <typeId>Unit} and, for a history item type, {@code <typeId>MaxVersions}. The
     * mode is required; the limit can be left out for an item type that is not enabled, which keeps its stored one. The
     * settings store rejects an enabled item type without a limit.
     *
     * @param request the request holding the form fields
     * @return the settings of every item type
     * @throws IllegalArgumentException if a mode is missing, or if a field has an invalid value
     */
    public Map<PurgeType, PurgeTypeSettings> readTypeSettings(XWikiRequest request)
    {
        Map<PurgeType, PurgeTypeSettings> typeSettings = new EnumMap<>(PurgeType.class);
        for (PurgeType type : PurgeType.values()) {
            String mode = type.getId() + "Mode";
            String modeValue = request.getParameter(mode);
            typeSettings.put(type, createSettings(type, PurgeMode.fromId(modeValue)
                    .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, modeValue, mode))),
                readNumber(request, type.getId() + INTERVAL_SUFFIX).orElse(null), readUnit(request, type).orElse(null),
                readMaxVersions(request, type).orElse(null)));
        }
        return typeSettings;
    }

    /**
     * Read the limit of a manual purge of an item type from the fields {@code <typeId>Interval} and
     * {@code <typeId>Unit}, which must be both set or both missing, and, for a history item type,
     * {@code <typeId>MaxVersions}.
     *
     * @param request the request holding the fields
     * @param type the item type
     * @return the limit, as enabled settings, or an empty value if all the fields are missing
     * @throws IllegalArgumentException if only one of the interval and unit fields is set, or if a field has an
     *     invalid value
     */
    public Optional<PurgeTypeSettings> readLimit(XWikiRequest request, PurgeType type)
    {
        Optional<Integer> interval = readNumber(request, type.getId() + INTERVAL_SUFFIX);
        Optional<IntervalUnit> unit = readUnit(request, type);
        Optional<Integer> maxVersions = readMaxVersions(request, type);
        if (interval.isEmpty() && unit.isEmpty() && maxVersions.isEmpty()) {
            return Optional.empty();
        }
        if (interval.isPresent() != unit.isPresent()) {
            throw new IllegalArgumentException(
                String.format("The limit of [%s] needs both an interval and a unit.", type.getId()));
        }
        return Optional.of(createSettings(type, PurgeMode.ENABLED, interval.orElse(null), unit.orElse(null),
            maxVersions.orElse(null)));
    }

    /**
     * @param settings the settings of an item type
     * @return {@code true} if the settings have a complete limit: an interval and a unit, or a number of versions
     */
    public boolean hasLimit(PurgeTypeSettings settings)
    {
        return (settings.getInterval().isPresent() && settings.getUnit().isPresent()) || getMaxVersions(
            settings).isPresent();
    }

    /**
     * Describe a limit with the fields of the preview responses.
     *
     * @param limit the limit of a purge
     * @return the parts of the limit: {@code interval}, {@code unit} (its identifier) and {@code maxVersions}
     */
    public Map<String, Object> getLimitFields(PurgeTypeSettings limit)
    {
        Map<String, Object> fields = new LinkedHashMap<>();
        limit.getInterval().ifPresent(interval -> fields.put("interval", interval));
        limit.getUnit().ifPresent(unit -> fields.put("unit", unit.getId()));
        getMaxVersions(limit).ifPresent(maxVersions -> fields.put("maxVersions", maxVersions));
        return fields;
    }

    /**
     * @param type the item type
     * @param mode how the automatic purge is configured for the item type
     * @param interval the interval, or {@code null} if it is missing
     * @param unit the unit of the interval, or {@code null} if it is missing
     * @param maxVersions the number of versions kept, or {@code null} if it is missing; always missing for an item
     *     type that is not a history
     * @return the settings of the item type, as {@link HistoryPurgeTypeSettings} for a history item type
     */
    private PurgeTypeSettings createSettings(PurgeType type, PurgeMode mode, Integer interval, IntervalUnit unit,
        Integer maxVersions)
    {
        return type.isHistory() ? new HistoryPurgeTypeSettings(mode, interval, unit, maxVersions)
            : new PurgeTypeSettings(mode, interval, unit);
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
     * @param request the request holding the fields
     * @param type the item type
     * @return the value of the field {@code <typeId>MaxVersions} of a history item type, or an empty value if it is
     *     missing or if the item type is not a history
     * @throws IllegalArgumentException if the value is not a number
     */
    private Optional<Integer> readMaxVersions(XWikiRequest request, PurgeType type)
    {
        return type.isHistory() ? readNumber(request, type.getId() + MAX_VERSIONS_SUFFIX) : Optional.empty();
    }

    /**
     * @param request the request holding the fields
     * @param field the name of the field
     * @return the value of the field, or an empty value if it is missing
     * @throws IllegalArgumentException if the value is not a number
     */
    private Optional<Integer> readNumber(XWikiRequest request, String field)
    {
        String value = request.getParameter(field);
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(Integer.parseInt(value));
    }

    /**
     * @param request the request holding the fields
     * @param type the item type
     * @return the value of the field {@code <typeId>Unit}, or an empty value if it is missing
     * @throws IllegalArgumentException if the unit is unknown
     */
    private Optional<IntervalUnit> readUnit(XWikiRequest request, PurgeType type)
    {
        String field = type.getId() + UNIT_SUFFIX;
        String value = request.getParameter(field);
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(IntervalUnit.fromId(value)
            .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, value, field))));
    }
}
