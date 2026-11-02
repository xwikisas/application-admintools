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
package com.xwiki.admintools.purge.internal.store;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgePageReferences}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgePageReferencesTest
{
    @InjectMockComponents
    private PurgePageReferences references;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    void getPageReferences()
    {
        assertEquals("xwiki", this.references.getMainWikiId());
        assertEquals(new DocumentReference("xwiki", Arrays.asList("AdminTools", "Purge", "Code"), "Settings"),
            this.references.getSettingsPageReference());
        assertEquals(new DocumentReference("xwiki", Arrays.asList("AdminTools", "Purge", "Wikis"), "subwiki"),
            this.references.getHistoryPageReference("subwiki"));
        assertEquals("AdminTools.Purge.Wikis", this.references.getWikisSpaceReference());
    }

    @Test
    void getSettingsClass()
    {
        assertEquals(new LocalDocumentReference(Arrays.asList("AdminTools", "Purge", "Code"), "SettingsClass"),
            this.references.getSettingsClassReference());
    }

    @Test
    void getHistoryClass()
    {
        assertEquals(new LocalDocumentReference(Arrays.asList("AdminTools", "Purge", "Code"), "HistoryClass"),
            this.references.getHistoryClassReference());
        assertEquals("AdminTools.Purge.Code.HistoryClass", this.references.getHistoryClassName());
    }
}
