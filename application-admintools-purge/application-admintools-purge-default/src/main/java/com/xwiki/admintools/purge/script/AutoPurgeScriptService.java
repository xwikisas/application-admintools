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
package com.xwiki.admintools.purge.script;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.PurgeManager;
import com.xwiki.admintools.purge.internal.PurgeWikisDataProvider;

/**
 * Script service of the automatic purge of the recycle bins, available as {@code $services.adminToolsAutoPurge}.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named("adminToolsAutoPurge")
@Singleton
@Unstable
public class AutoPurgeScriptService implements ScriptService
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private PurgeWikisDataProvider purgeWikisDataProvider;

    @Inject
    private PurgeManager purgeManager;

    /**
     * Get the rows of the LiveData listing the automatic purge of each wiki, sorted by wiki name. Each row holds the
     * {@code wikiId} and {@code wikiName} of a wiki and, for each item type, its settings for the wiki under the
     * identifier of the item type, and its newest purge history entry under {@code <typeId>LastPurge}, which is missing
     * if the item type was never purged on the wiki. Requires admin rights on the main wiki, since the rows describe
     * every wiki.
     *
     * @param filters the filters of the LiveData, empty ones being ignored: {@code wikiName}, a part of the wiki
     *     name, ignoring the case; {@code <typeId>Mode}, the identifier of the mode of the item type for the wiki;
     *     {@code <typeId>Status}, the identifier of the status of the newest purge of the item type, or {@code none} if
     *     it was never purged
     * @param order {@code desc} to sort the wikis by descending name, anything else to sort them by ascending name
     * @return the rows of the wikis matching the filters; no rows if the license of Admin Tools is not active
     * @throws AccessDeniedException if the current user is not an admin of the main wiki
     * @throws WikiManagerException if the wikis can't be listed
     * @throws QueryException if the purge history can't be queried
     * @throws XWikiException if the settings page can't be loaded
     */
    public List<Map<String, Object>> getWikis(Map<String, String> filters, String order)
        throws AccessDeniedException, WikiManagerException, QueryException, XWikiException
    {
        this.contextualAuthorizationManager.checkAccess(Right.ADMIN,
            new WikiReference(this.wikiDescriptorManager.getMainWikiId()));
        if (!this.purgeManager.isLicensed()) {
            return List.of();
        }
        return this.purgeWikisDataProvider.getWikis(filters, order);
    }
}
