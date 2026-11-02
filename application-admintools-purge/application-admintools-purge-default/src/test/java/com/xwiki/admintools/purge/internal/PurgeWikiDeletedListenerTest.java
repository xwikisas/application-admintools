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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeWikiDeletedListener}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeWikiDeletedListenerTest
{
    private static final String WIKI_ID = "subwiki";

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private PurgeWikiDeletedListener listener;

    @MockComponent
    private PurgeSettingsStore settingsStore;

    @MockComponent
    private PurgeHistoryStore historyStore;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Test
    void onWikiDeleted() throws Exception
    {
        this.listener.onEvent(new WikiDeletedEvent(WIKI_ID), WIKI_ID, null);

        verify(this.settingsStore).removeWikiSettings(WIKI_ID);
        verify(this.historyStore).deleteHistory(WIKI_ID);
    }

    @Test
    void onRemoteWikiDeleted() throws Exception
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);

        this.listener.onEvent(new WikiDeletedEvent(WIKI_ID), WIKI_ID, null);

        verify(this.settingsStore, never()).removeWikiSettings(anyString());
        verify(this.historyStore, never()).deleteHistory(anyString());
    }

    @Test
    void onWikiDeletedWhenSettingsRemovalFails() throws Exception
    {
        doThrow(new XWikiException(0, 0, "Failed to save")).when(this.settingsStore).removeWikiSettings(WIKI_ID);

        this.listener.onEvent(new WikiDeletedEvent(WIKI_ID), WIKI_ID, null);

        // The history is deleted anyway, since the event is never sent again.
        verify(this.historyStore).deleteHistory(WIKI_ID);
        assertEquals("Failed to remove the automatic purge settings of the deleted wiki [subwiki].",
            this.logCapture.getMessage(0));
    }

    @Test
    void onWikiDeletedWhenHistoryDeletionFails() throws Exception
    {
        doThrow(new XWikiException(0, 0, "Failed to delete")).when(this.historyStore).deleteHistory(WIKI_ID);

        this.listener.onEvent(new WikiDeletedEvent(WIKI_ID), WIKI_ID, null);

        verify(this.settingsStore).removeWikiSettings(WIKI_ID);
        assertEquals("Failed to delete the automatic purge history of the deleted wiki [subwiki].",
            this.logCapture.getMessage(0));
    }
}
