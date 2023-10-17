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
package com.xwiki.admintools.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.event.Event;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class AdminToolsEventListenerTest
{
    private final List<String> SPACE = Arrays.asList("AdminTools", "Code");

    @Mock
    private XWikiDocument xWikiDocument;

    @InjectMockComponents
    private AdminToolsEventListener adminToolsEventListener;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private CurrentServer currentServer;

    @Test
    void onEventDocumentUpdate()
    {
        Event event = new DocumentUpdatedEvent();
        DocumentReference configReference =
            new DocumentReference(new LocalDocumentReference(SPACE, "Configuration"), new WikiReference("mywiki"));
        when(xWikiDocument.getDocumentReference()).thenReturn(configReference);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");

        adminToolsEventListener.onEvent(event, xWikiDocument, null);
        verify(currentServer).updateCurrentServer();
    }

    @Test
    void onEventDocumentDelete()
    {
        Event event = new DocumentDeletedEvent();
        DocumentReference configReference =
            new DocumentReference(new LocalDocumentReference(SPACE, "Configuration"), new WikiReference("mywiki"));
        when(xWikiDocument.getDocumentReference()).thenReturn(configReference);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");

        adminToolsEventListener.onEvent(event, xWikiDocument, null);
        verify(currentServer).updateCurrentServer();
    }

    @Test
    void onEventDifferentAction()
    {
        Event event = new DocumentCreatedEvent();
        DocumentReference configReference =
            new DocumentReference(new LocalDocumentReference(SPACE, "Configuration"), new WikiReference("mywiki"));
        when(xWikiDocument.getDocumentReference()).thenReturn(configReference);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");

        adminToolsEventListener.onEvent(event, xWikiDocument, null);
        verify(currentServer, never()).updateCurrentServer();
    }

    @Test
    void onEventIsNotAdminToolsConfigObject()
    {
        Event event = new DocumentUpdatedEvent();
        List<String> SPACE = Arrays.asList("other_page", "Code");
        DocumentReference configReference =
            new DocumentReference(new LocalDocumentReference(SPACE, "Configuration"), new WikiReference("mywiki"));
        when(xWikiDocument.getDocumentReference()).thenReturn(configReference);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mywiki");

        adminToolsEventListener.onEvent(event, xWikiDocument, null);
        verify(currentServer, never()).updateCurrentServer();
    }
}
