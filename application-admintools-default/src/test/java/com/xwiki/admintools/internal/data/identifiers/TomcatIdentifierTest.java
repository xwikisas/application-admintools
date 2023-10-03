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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.configuration.AdminToolsConfiguration;

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
    @Mock
    File file;

    @Mock
    BufferedReader bufferedReader;

    @InjectMockComponents
    private TomcatIdentifier tomcatIdentifier;

    @MockComponent
    @Named("default")
    private AdminToolsConfiguration adminToolsConfig;

    @XWikiTempDir
    private File tmpDir;

    @Test
    void isUsedFound() throws IOException
    {
        when(adminToolsConfig.getServerPath()).thenReturn(tmpDir.getAbsolutePath());

        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();

        File testFile = new File(configDirectory, "catalina.properties");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        // Test with a valid providedConfigServerPath
        assertTrue(tomcatIdentifier.isUsed());
    }

    // Test with no providedConfigServerPath but catalina.base property set
    @Test
    void isUsedFoundSystemProperty() throws IOException
    {
        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();

        File testFile = new File(configDirectory, "catalina.properties");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        System.setProperty("catalina.base", tmpDir.getAbsolutePath());
        assertTrue(tomcatIdentifier.isUsed());
        System.clearProperty("catalina.base");
    }

    // Test with no providedConfigServerPath but catalina.base property set
    @Test
    void isUsedFoundSystemPropertyFail()
    {
        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();

        System.setProperty("catalina.base", tmpDir.getAbsolutePath());
        assertFalse(tomcatIdentifier.isUsed());
        System.clearProperty("catalina.base");
        configDirectory.delete();
    }

    // Test with neither providedConfigServerPath nor catalina.base/CATALINA_HOME set
    @Test
    void isUsedNotFound()
    {
        when(adminToolsConfig.getServerPath()).thenReturn(null);
        assertFalse(tomcatIdentifier.isUsed());
    }

    @Test
    void isUsedWrongPath()
    {
        when(adminToolsConfig.getServerPath()).thenReturn(tmpDir.getAbsolutePath());

        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();

        // Test with a valid providedConfigServerPath
        assertFalse(tomcatIdentifier.isUsed());
        configDirectory.delete();
    }

    @Test
    void getIdentifier()
    {
        assertEquals("Tomcat", tomcatIdentifier.getComponentHint());
    }
}
