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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.configuration.AdminToolsConfiguration;
import com.xwiki.admintools.internal.util.DefaultFileOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link TomcatIdentifier}
 *
 * @version $Id$
 */
@ComponentTest
public class TomcatIdentifierTest
{
    @InjectMockComponents
    private TomcatIdentifier tomcatIdentifier;

    @MockComponent
    private DefaultFileOperations fileOperations;

    @MockComponent
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    @Test
    void isUsedFound()
    {
        // Mock the behavior of the File object
        when(fileOperations.fileExists()).thenReturn(true);

        when(adminToolsConfig.getServerPath()).thenReturn("user_inserted_path");

        // Test with a valid providedConfigServerPath
        assertTrue(tomcatIdentifier.isUsed());

        // Test with no providedConfigServerPath but catalina.base property set
        System.setProperty("catalina.base", "found");
        assertTrue(tomcatIdentifier.isUsed());
        System.clearProperty("catalina.base");
    }

    @Test
    void isUsedNotFound()
    {
        // Test with neither providedConfigServerPath nor catalina.base/CATALINA_HOME set
        assertFalse(tomcatIdentifier.isUsed());
    }

    @Test
    void isUsedWrongPath()
    {
        // Test with incorrect providedConfigServerPath
        assertFalse(tomcatIdentifier.isUsed());
    }

    @Test
    void getIdentifier()
    {
        assertEquals("Tomcat", tomcatIdentifier.getComponentHint());
    }
}
