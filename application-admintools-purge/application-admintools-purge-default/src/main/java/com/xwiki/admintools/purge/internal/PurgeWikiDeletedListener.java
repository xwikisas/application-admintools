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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;

/**
 * Removes the automatic purge settings and the purge history of a wiki when the wiki is deleted.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(PurgeWikiDeletedListener.HINT)
@Singleton
public class PurgeWikiDeletedListener extends AbstractEventListener
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "PurgeWikiDeletedListener";

    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgeHistoryStore historyStore;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private Logger logger;

    /**
     * Create an event listener for the deletion of wikis.
     */
    public PurgeWikiDeletedListener()
    {
        super(HINT, new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // In a cluster, only the node where the wiki was deleted deletes the page.
        if (this.remoteObservationManagerContext.isRemoteState()) {
            return;
        }
        String wikiId = ((WikiDeletedEvent) event).getWikiId();
        // Each removal is attempted even if the other fails, since the event is never sent again.
        try {
            this.settingsStore.removeWikiSettings(wikiId);
        } catch (XWikiException e) {
            this.logger.error("Failed to remove the automatic purge settings of the deleted wiki [{}].", wikiId, e);
        }
        try {
            this.historyStore.deleteHistory(wikiId);
        } catch (XWikiException e) {
            this.logger.error("Failed to delete the automatic purge history of the deleted wiki [{}].", wikiId, e);
        }
    }
}
