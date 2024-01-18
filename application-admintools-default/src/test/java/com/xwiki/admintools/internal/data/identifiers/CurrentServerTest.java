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
package com.xwiki.admintools.internal.data.identifiers;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.activeinstalls2.internal.data.ServletContainerPing;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.internal.PingProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CurrentServer}
 *
 * @version $Id$
 */
@ComponentTest
class CurrentServerTest
{
    @InjectMockComponents
    private CurrentServer currentServer;

    @MockComponent
    private Provider<List<ServerIdentifier>> supportedServers;

    @MockComponent
    private ServerIdentifier serverIdentifier;

    @MockComponent
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    @MockComponent
    private PingProvider pingProvider;

    private ServletContainerPing containerPing;

    @BeforeComponent
    void setUp()
    {
        // Mock the list of supported servers.
        List<ServerIdentifier> mockServerIdentifiers = new ArrayList<>();
        mockServerIdentifiers.add(serverIdentifier);
        when(supportedServers.get()).thenReturn(mockServerIdentifiers);
        when(serverIdentifier.foundServerPath()).thenReturn(true);
        when(serverIdentifier.getComponentHint()).thenReturn("tomcat");

        // Mock the behavior of adminToolsConfig.
        when(adminToolsConfig.getServerPath()).thenReturn("exampleServerPath");

        containerPing = new ServletContainerPing();
        containerPing.setName("tomcat");
        containerPing.setVersion("x.y.z");
        when(pingProvider.getServletPing()).thenReturn(containerPing);
    }

    @Test
    void initializeFound() throws InitializationException
    {
        // Call the initialize method.
        currentServer.initialize();

        // Verify that the currentServerIdentifier is set correctly.
        assertEquals(serverIdentifier, currentServer.getCurrentServer());
    }

    @Test
    void initializeWithServerNotFound() throws InitializationException
    {
        when(serverIdentifier.foundServerPath()).thenReturn(false);
        currentServer.initialize();

        assertNull(currentServer.getCurrentServer());
    }

    @Test
    void updateCurrentServer()
    {
        when(serverIdentifier.foundServerPath()).thenReturn(false);
        currentServer.updateCurrentServer();
        assertNull(currentServer.getCurrentServer());

        when(serverIdentifier.foundServerPath()).thenReturn(true);
        currentServer.updateCurrentServer();
        assertEquals(serverIdentifier, currentServer.getCurrentServer());
    }

    @Test
    void getSupportedServers()
    {
        when(serverIdentifier.getComponentHint()).thenReturn("testServer");

        // Create the expected list.
        List<String> testServersList = new ArrayList<>();
        testServersList.add("testServer");

        // Verify if the method returns the expected list.
        assertEquals(testServersList, currentServer.getSupportedServers());
    }
}
