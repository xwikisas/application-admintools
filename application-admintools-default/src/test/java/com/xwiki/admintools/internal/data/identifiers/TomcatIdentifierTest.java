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

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
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
 * Unit test for {@link TomcatInfo}
 *
 * @version $Id$
 */
@ComponentTest
class TomcatIdentifierTest
{
    @InjectMockComponents
    private TomcatInfo tomcatIdentifier;

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

    @Test
    void isUsedWithValidCatalinaBase() throws IOException
    {
        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();
        configDirectory.deleteOnExit();

        File testFile = new File(configDirectory, "catalina.properties");
        testFile.createNewFile();
        assertTrue(testFile.exists());

        when(adminToolsConfig.getServerPath()).thenReturn(null);
        System.setProperty("catalina.base", tmpDir.getAbsolutePath());
        assertTrue(tomcatIdentifier.isUsed());
        System.clearProperty("catalina.base");
    }

    @Test
    void isUsedValidSystemPathMissingCatalinaProperties()
    {
        File configDirectory = new File(tmpDir, "conf");
        configDirectory.mkdir();
        when(adminToolsConfig.getServerPath()).thenReturn(null);
        System.setProperty("catalina.base", tmpDir.getAbsolutePath());
        assertFalse(tomcatIdentifier.isUsed());
        System.clearProperty("catalina.base");
        configDirectory.delete();
    }

    @Test
    void getIdentifier()
    {
        assertEquals("Tomcat", tomcatIdentifier.getComponentHint());
    }
}
