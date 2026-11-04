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
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

/**
 * Resolves the automatic purge settings that apply to an item type of a wiki: the settings of the wiki, unless they
 * inherit, in which case the global settings apply.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgePolicyResolver.class)
@Singleton
public class PurgePolicyResolver
{
    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private RetentionCalculator retentionCalculator;

    /**
     * Get the expiration date of the deleted items of a type on a wiki: the items deleted before it are purged. For
     * example, with a limit of 1 month, a purge running on 30 October removes the items deleted before 30 September.
     *
     * @param wikiId the identifier of a wiki
     * @param type an item type
     * @param now when the purge runs
     * @return the expiration date, or an empty value if the item type is not purged on the wiki
     * @throws XWikiException if the settings can't be loaded
     */
    public Optional<Instant> getExpirationDate(String wikiId, PurgeType type, ZonedDateTime now) throws XWikiException
    {
        PurgeTypeSettings settings = getSettings(wikiId, type);
        if (settings.getMode() != PurgeMode.ENABLED) {
            return Optional.empty();
        }
        return Optional.of(this.retentionCalculator.getExpirationDate(now, settings.getInterval(), settings.getUnit()));
    }

    /**
     * Get the settings that apply to an item type on a wiki: the settings of the wiki, or the default settings if the
     * wiki inherits them.
     *
     * @param wikiId the identifier of a wiki
     * @param type an item type
     * @return the settings that apply, either {@link PurgeMode#ENABLED} or {@link PurgeMode#DISABLED}; the interval and
     *     unit are the stored ones even if the item type is disabled
     * @throws XWikiException if the settings can't be loaded
     */
    public PurgeTypeSettings getSettings(String wikiId, PurgeType type) throws XWikiException
    {
        PurgeTypeSettings settings = this.settingsStore.getWikiSettings(wikiId).get(type);
        if (settings == null || settings.getMode() == PurgeMode.INHERIT) {
            settings = this.settingsStore.getGlobalSettings().getTypeSettings(type);
        }
        return settings;
    }
}
