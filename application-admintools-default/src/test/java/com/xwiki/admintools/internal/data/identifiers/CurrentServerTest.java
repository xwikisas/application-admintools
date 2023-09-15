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
import org.mockito.InjectMocks;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.ServerIdentifier;
import com.xwiki.admintools.configuration.AdminToolsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CurrentServer}
 *
 * @version $Id$
 */
@ComponentTest
public class CurrentServerTest
{
    @InjectMocks
    private CurrentServer currentServer;

    @MockComponent
    private Provider<List<ServerIdentifier>> supportedServers;

    @MockComponent
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    @Test
    public void testInitialize() throws InitializationException
    {
        // Mock the behavior of adminToolsConfig
        when(adminToolsConfig.getServerPath()).thenReturn("exampleServerPath");

        // Create a mock implementation of ServerIdentifier
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        // Mock the list of supported servers
        List<ServerIdentifier> mockServerIdentifiers = new ArrayList<>();
        mockServerIdentifiers.add(mockServerIdentifier);
        when(supportedServers.get()).thenReturn(mockServerIdentifiers);
        when(mockServerIdentifier.isUsed("exampleServerPath")).thenReturn(true);

        // Call the initialize method
        currentServer.initialize();

        // Verify that the currentServerIdentifier is set correctly
        assertEquals(mockServerIdentifier, currentServer.getCurrentServer());
    }

    @Test
    public void testUpdateCurrentServerAfterInitializationNotFound() throws InitializationException
    {
        // Mock the behavior of adminToolsConfig
        when(adminToolsConfig.getServerPath()).thenReturn("exampleServerPath");

        // Create a mock implementation of ServerIdentifier
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        // Mock the list of supported servers
        List<ServerIdentifier> mockServerIdentifiers = new ArrayList<>();
        mockServerIdentifiers.add(mockServerIdentifier);
        when(supportedServers.get()).thenReturn(mockServerIdentifiers);

        // Call the initialize method
        currentServer.initialize();

        // Verify that the currentServerIdentifier was not found
        assertNull(currentServer.getCurrentServer());

        // Mock the behaviour of serverIdentifier
        when(mockServerIdentifier.isUsed("exampleServerPath")).thenReturn(true);

        // Call the updateCurrentServer method
        currentServer.updateCurrentServer();

        // Verify that the currentServerIdentifier is set correctly
        assertEquals(mockServerIdentifier, currentServer.getCurrentServer());
    }

    @Test
    public void testInitializeNotFound() throws InitializationException
    {
        // Mock the behavior of adminToolsConfig
        when(adminToolsConfig.getServerPath()).thenReturn("exampleServerPath");

        // Create a mock implementation of ServerIdentifier
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        // Mock the list of supported servers
        List<ServerIdentifier> mockServerIdentifiers = new ArrayList<>();
        mockServerIdentifiers.add(mockServerIdentifier);
        when(supportedServers.get()).thenReturn(mockServerIdentifiers);

        // Call the initialize method
        currentServer.initialize();

        // Verify that the currentServerIdentifier was not found
        assertNull(currentServer.getCurrentServer());
    }

    @Test
    public void getSupportedServersTest() throws InitializationException
    {
        // Mock the behavior of adminToolsConfig
        when(adminToolsConfig.getServerPath()).thenReturn("exampleServerPath");
        // Create a mock implementation of ServerIdentifier
        ServerIdentifier mockServerIdentifier = mock(ServerIdentifier.class);

        // Mock the list of supported servers
        List<ServerIdentifier> mockServerIdentifiers = new ArrayList<>();
        mockServerIdentifiers.add(mockServerIdentifier);
        when(supportedServers.get()).thenReturn(mockServerIdentifiers);
        when(mockServerIdentifier.getComponentHint()).thenReturn("testServer");

        // Create the expected list
        List<String> testServersList = new ArrayList<>();
        testServersList.add("testServer");

        // Verify if the method returns the expected list
        assertEquals(testServersList, currentServer.getSupportedServers());
    }
}
