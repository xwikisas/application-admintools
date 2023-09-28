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
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xwiki.admintools.internal.data.identifiers.CurrentServer;

/**
 * Listens to configuration updates.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(AdminToolsConfigEventListener.HINT)
@Singleton
public class AdminToolsConfigEventListener extends AbstractEventListener
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "AdminToolsConfigEventListener";

    private static final List<String> SPACE = Arrays.asList("AdminTools", "Code");

    private static final LocalDocumentReference CONFIG_DOC = new LocalDocumentReference(SPACE, "Configuration");

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private CurrentServer currentServer;

    /**
     * Creates an event-listener filtering for ApplicationReadyEvent and DocumentUpdatedEvent.
     */
    public AdminToolsConfigEventListener()
    {
        super(HINT, new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentUpdatedEvent || event instanceof DocumentDeletedEvent) {
            XWikiDocument document = (XWikiDocument) source;
            if (document != null && isAdminToolsConfigObject(document)) {
                currentServer.updateCurrentServer();
            }
        }
    }

    private boolean isAdminToolsConfigObject(XWikiDocument doc)
    {
        DocumentReference configReference = new DocumentReference(CONFIG_DOC, this.getCurrentWikiReference());
        return Objects.equals(doc.getDocumentReference(), configReference);
    }

    private WikiReference getCurrentWikiReference()
    {
        return new WikiReference(this.wikiManager.getCurrentWikiId());
    }
}
