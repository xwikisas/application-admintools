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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Listens to extension uninstall events and if the Admin Tools extension is being uninstalled, attempt to remove the
 * remaining modules.
 *
 * @version $Id$
 */
@Component
@Named(AdminToolsUninstallListener.HINT)
@Singleton
public class AdminToolsUninstallListener extends AbstractEventListener
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "AdminToolsUninstallListener";

    @Inject
    protected Logger logger;

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private InstalledExtensionRepository installedRepository;

    /**
     * Creates an event-listener filtering for ExtensionUninstalledEvent.
     */
    public AdminToolsUninstallListener()
    {
        super(HINT, new ExtensionUninstalledEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ExtensionUninstalledEvent) {
            try {
                ExtensionId uninstalledExtension = ((ExtensionUninstalledEvent) event).getExtensionId();
                if (uninstalledExtension.getId().equals(getExtensionId("ui"))) {
                    ExtensionId apiExtensionId =
                        new ExtensionId(getExtensionId("api"), uninstalledExtension.getVersion());
                    ExtensionId defaultExtensionId =
                        new ExtensionId(getExtensionId("default"), uninstalledExtension.getVersion());

                    InstalledExtension apiExtension = installedRepository.getInstalledExtension(apiExtensionId);
                    InstalledExtension defaultExtension = installedRepository.getInstalledExtension(defaultExtensionId);
                    String namespace = new WikiNamespace(wikiContextProvider.get().getWikiId()).serialize();
                    installedRepository.uninstallExtension(defaultExtension, namespace);
                    installedRepository.uninstallExtension(apiExtension, namespace);
                    logger.info("Successfully uninstalled all Admin Tools modules.");
                }
            } catch (UninstallException e) {
                logger.error("There was an error while uninstalling Admin Tools modules.");
                throw new RuntimeException(e);
            }
        }
    }

    private String getExtensionId(String module)
    {
        return String.format("com.xwiki.admintools:application-admintools-%s", module);
    }
}
