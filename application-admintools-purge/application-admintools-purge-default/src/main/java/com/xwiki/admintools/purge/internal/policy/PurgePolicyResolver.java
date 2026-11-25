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

    /**
     * Get the settings that apply to an item type on a wiki: the settings of the wiki, or the default settings if the
     * wiki inherits them.
     *
     * @param wikiId the identifier of a wiki
     * @param type an item type
     * @return the settings that apply, either {@link PurgeMode#ENABLED} or {@link PurgeMode#DISABLED}, with the stored
     *     interval and unit even if the item type is disabled; an empty value if the wiki inherits the default settings
     *     and they leave the item type out, in which case it is not purged
     * @throws XWikiException if the settings can't be loaded
     */
    public Optional<PurgeTypeSettings> getSettings(String wikiId, PurgeType type) throws XWikiException
    {
        Optional<PurgeTypeSettings> settings = Optional.ofNullable(this.settingsStore.getWikiSettings(wikiId).get(type))
            .filter(wikiSettings -> wikiSettings.getMode() != PurgeMode.INHERIT);
        if (settings.isEmpty()) {
            // Not in a lambda, since loading the global settings can throw a checked exception.
            settings = this.settingsStore.getGlobalSettings().getTypeSettings(type);
        }
        return settings;
    }
}
