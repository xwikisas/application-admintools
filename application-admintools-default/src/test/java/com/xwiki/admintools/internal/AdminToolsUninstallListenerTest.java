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

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.event.ExtensionUninstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.version.Version;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ComponentTest
class AdminToolsUninstallListenerTest
{
    private final List<String> SPACE = Arrays.asList("AdminTools", "Code");

    @InjectMockComponents
    private AdminToolsUninstallListener uninstallListener;

    @MockComponent
    private Provider<XWikiContext> wikiContextProvider;

    @MockComponent
    private InstalledExtensionRepository installedRepository;

    @Mock
    private ExtensionId unistalledMockExtensionId;

    @Mock
    private Version uninstallVersion;

    @Mock
    private InstalledExtension apiMockExtension;

    @Mock
    private InstalledExtension defaultMockExtension;

    @Mock
    private XWikiContext wikiContext;

    private ExtensionUninstalledEvent event;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeEach
    void setUp()
    {
        event = new ExtensionUninstalledEvent(unistalledMockExtensionId, "");
        when(unistalledMockExtensionId.getVersion()).thenReturn(uninstallVersion);
        when(unistalledMockExtensionId.getId()).thenReturn("com.xwiki.admintools:application-admintools-ui");

        ExtensionId apiExtId = new ExtensionId("com.xwiki.admintools:application-admintools-api", uninstallVersion);
        ExtensionId defaultExtId =
            new ExtensionId("com.xwiki.admintools:application-admintools-default", uninstallVersion);
        when(installedRepository.getInstalledExtension(apiExtId)).thenReturn(apiMockExtension);
        when(installedRepository.getInstalledExtension(defaultExtId)).thenReturn(defaultMockExtension);

        when(wikiContextProvider.get()).thenReturn(wikiContext);
        when(wikiContext.getWikiId()).thenReturn("test_wiki");
    }

    @Test
    @Order(1)
    void onEventSuccess()
    {
        uninstallListener.onEvent(event, null, null);

        assertEquals("Attempting to uninstall Admin Tools default module...", logCapture.getMessage(0));
        assertEquals("Attempting to uninstall Admin Tools API module...", logCapture.getMessage(1));
        assertEquals("Successfully uninstalled all Admin Tools modules.", logCapture.getMessage(2));
    }

    @Test
    @Order(2)
    void onEventFail() throws UninstallException
    {
        String namespace = new WikiNamespace(wikiContextProvider.get().getWikiId()).serialize();
        doThrow(new UninstallException("Mock test exception.")).when(installedRepository)
            .uninstallExtension(apiMockExtension, namespace);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            uninstallListener.onEvent(event, null, null);
        });

        assertEquals("org.xwiki.extension.UninstallException: Mock test exception.", exception.getMessage());
        assertEquals("Attempting to uninstall Admin Tools default module...", logCapture.getMessage(0));
        assertEquals("Attempting to uninstall Admin Tools API module...", logCapture.getMessage(1));
        assertEquals(
            "There was an error while uninstalling Admin Tools modules. Root cause is: [UninstallException: Mock test exception.]",
            logCapture.getMessage(2));
    }

    @Test
    @Order(3)
    void onEventIsNotAdminToolsModule()
    {
        when(unistalledMockExtensionId.getId()).thenReturn("some-other:module");

        uninstallListener.onEvent(event, null, null);
        assertEquals(0, logCapture.size());
    }
}

