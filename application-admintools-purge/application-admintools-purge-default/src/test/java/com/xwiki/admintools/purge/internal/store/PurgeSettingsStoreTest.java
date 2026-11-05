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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.admintools.purge.internal.store.mapper.PurgeSettingsMapper;
import com.xwiki.admintools.purge.model.GlobalPurgeSettings;
import com.xwiki.admintools.purge.model.IntervalUnit;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;

import static com.xwiki.admintools.purge.model.PurgeType.DELETED_ATTACHMENTS;
import static com.xwiki.admintools.purge.model.PurgeType.DELETED_DOCUMENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurgeSettingsStore}.
 *
 * @version $Id$
 */
@ComponentTest
class PurgeSettingsStoreTest
{
    private static final String WIKI_ID = "subwiki";

    private static final PurgeTypeSettings INHERIT = new PurgeTypeSettings(PurgeMode.INHERIT, 1, IntervalUnit.MONTHS);

    private static final Map<PurgeType, PurgeTypeSettings> CUSTOM_SETTINGS =
        Map.of(DELETED_DOCUMENTS, new PurgeTypeSettings(PurgeMode.ENABLED, 7, IntervalUnit.DAYS), DELETED_ATTACHMENTS,
            INHERIT);

    private static final Map<PurgeType, PurgeTypeSettings> INHERITED_SETTINGS =
        Map.of(DELETED_DOCUMENTS, INHERIT, DELETED_ATTACHMENTS, INHERIT);

    /**
     * The settings objects of the settings page; a removed object leaves a {@code null} in the list.
     */
    private final List<BaseObject> objects = new ArrayList<>();

    @InjectMockComponents
    private PurgeSettingsStore store;

    @MockComponent
    private PurgePageReferences references;

    @MockComponent
    private PurgePageManager pageManager;

    @MockComponent
    private PurgeSettingsMapper mapper;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Mock
    private DocumentReference settingsReference;

    @Mock
    private LocalDocumentReference settingsClass;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject globalObject;

    @Mock
    private BaseObject wikiObject;

    @Mock
    private BaseObject newObject;

    @Mock
    private Lock lock;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.references.getSettingsPageReference()).thenReturn(this.settingsReference);
        when(this.references.getSettingsClassReference()).thenReturn(this.settingsClass);
        // Reads use the cached page, modifications use a copy; the same mock stands for both.
        when(this.pageManager.getDocument(this.settingsReference)).thenReturn(this.document);
        when(this.pageManager.getDocumentCopy(this.settingsReference)).thenReturn(this.document);
        when(this.pageManager.getGlobalLock()).thenReturn(this.lock);
        when(this.pageManager.createObject(this.document, this.settingsClass)).thenReturn(this.newObject);
        when(this.document.getXObjects(this.settingsClass)).thenReturn(this.objects);
        when(this.mapper.readWiki(any())).thenReturn(Optional.empty());
        when(this.mapper.readWiki(this.wikiObject)).thenReturn(Optional.of(WIKI_ID));
        when(this.wikiDescriptorManager.exists(WIKI_ID)).thenReturn(true);
        this.objects.addAll(Arrays.asList(null, this.globalObject, this.wikiObject));
    }

    @Test
    void getGlobalSettings() throws Exception
    {
        GlobalPurgeSettings settings = mock(GlobalPurgeSettings.class);
        when(this.mapper.readSettings(this.globalObject)).thenReturn(settings);

        assertSame(settings, this.store.getGlobalSettings());
        verify(this.pageManager, never()).getDocumentCopy(any());
    }

    @Test
    void getGlobalSettingsWithoutObject() throws Exception
    {
        this.objects.clear();
        GlobalPurgeSettings settings = mock(GlobalPurgeSettings.class);
        when(this.mapper.readSettings(null)).thenReturn(settings);

        assertSame(settings, this.store.getGlobalSettings());
    }

    @Test
    void saveGlobalSettings() throws Exception
    {
        this.store.saveGlobalSettings(CUSTOM_SETTINGS, 30);

        InOrder inOrder = inOrder(this.lock, this.mapper, this.pageManager);
        inOrder.verify(this.lock).lock();
        inOrder.verify(this.mapper).writeSettings(this.globalObject, CUSTOM_SETTINGS, 30);
        inOrder.verify(this.pageManager).save(this.document, "Updated the automatic purge defaults.");
        inOrder.verify(this.lock).unlock();
        verify(this.pageManager, never()).createObject(any(), any());
    }

    @Test
    void saveGlobalSettingsCreatesObject() throws Exception
    {
        this.objects.remove(this.globalObject);

        this.store.saveGlobalSettings(CUSTOM_SETTINGS, 30);

        verify(this.mapper).writeSettings(this.newObject, CUSTOM_SETTINGS, 30);
    }

    @Test
    void saveGlobalSettingsInvalid() throws Exception
    {
        doThrow(new IllegalArgumentException("invalid")).when(this.mapper)
            .writeSettings(this.globalObject, CUSTOM_SETTINGS, 0);

        assertThrows(IllegalArgumentException.class, () -> this.store.saveGlobalSettings(CUSTOM_SETTINGS, 0));

        verify(this.pageManager, never()).save(any(), anyString());
        verify(this.lock).unlock();
    }

    @Test
    void getWikiSettings() throws Exception
    {
        when(this.mapper.readSettings(this.wikiObject, WIKI_ID)).thenReturn(CUSTOM_SETTINGS);
        when(this.mapper.readSettings(null, "other")).thenReturn(INHERITED_SETTINGS);

        assertSame(CUSTOM_SETTINGS, this.store.getWikiSettings(WIKI_ID));
        assertSame(INHERITED_SETTINGS, this.store.getWikiSettings("other"));
        verify(this.pageManager, never()).getDocumentCopy(any());
    }

    @Test
    void getSettingsOfWikis() throws Exception
    {
        when(this.mapper.readSettings(this.wikiObject, WIKI_ID)).thenReturn(CUSTOM_SETTINGS);
        when(this.mapper.readSettings(null, "other")).thenReturn(INHERITED_SETTINGS);

        assertEquals(Map.of(WIKI_ID, CUSTOM_SETTINGS, "other", INHERITED_SETTINGS),
            this.store.getSettingsOfWikis(List.of(WIKI_ID, "other")));
        // The settings page is read once for all the wikis.
        verify(this.pageManager).getDocument(this.settingsReference);
    }

    @Test
    void saveWikiSettings() throws Exception
    {
        this.store.saveWikiSettings(WIKI_ID, CUSTOM_SETTINGS);

        // The wiki is checked under the lock, so that it can't be deleted before its settings are saved.
        InOrder inOrder = inOrder(this.lock, this.wikiDescriptorManager, this.mapper, this.pageManager);
        inOrder.verify(this.lock).lock();
        inOrder.verify(this.wikiDescriptorManager).exists(WIKI_ID);
        inOrder.verify(this.mapper).writeSettings(this.wikiObject, WIKI_ID, CUSTOM_SETTINGS);
        inOrder.verify(this.pageManager).save(this.document, "Updated the automatic purge settings of wiki [subwiki].");
        inOrder.verify(this.lock).unlock();
        verify(this.pageManager, never()).createObject(any(), any());
    }

    @Test
    void saveWikiSettingsOfMissingWiki() throws Exception
    {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.store.saveWikiSettings("other", CUSTOM_SETTINGS));

        assertEquals("The wiki [other] doesn't exist.", exception.getMessage());
        InOrder inOrder = inOrder(this.lock, this.wikiDescriptorManager);
        inOrder.verify(this.lock).lock();
        inOrder.verify(this.wikiDescriptorManager).exists("other");
        inOrder.verify(this.lock).unlock();
        verify(this.mapper, never()).writeSettings(any(), anyString(), any());
        verify(this.pageManager, never()).save(any(), anyString());
    }

    @Test
    void saveWikiSettingsCreatesObject() throws Exception
    {
        this.objects.remove(this.wikiObject);

        this.store.saveWikiSettings(WIKI_ID, CUSTOM_SETTINGS);

        verify(this.mapper).writeSettings(this.newObject, WIKI_ID, CUSTOM_SETTINGS);
        verify(this.mapper, never()).writeSettings(any(), any(), any(Integer.class));
    }

    @Test
    void saveWikiSettingsAllInheritRemovesObject() throws Exception
    {
        this.store.saveWikiSettings(WIKI_ID, INHERITED_SETTINGS);

        verify(this.document).removeXObject(this.wikiObject);
        verify(this.document, never()).removeXObject(this.globalObject);
        verify(this.pageManager).save(this.document, "Removed the automatic purge settings of wiki [subwiki].");
        verify(this.lock).unlock();
    }

    @Test
    void removeWikiSettingsWithoutSettings() throws Exception
    {
        this.store.removeWikiSettings("other");

        // The page is always checked under the lock, so that a save in progress can't leave the settings behind.
        InOrder inOrder = inOrder(this.lock, this.pageManager);
        inOrder.verify(this.lock).lock();
        inOrder.verify(this.pageManager).getDocumentCopy(this.settingsReference);
        inOrder.verify(this.lock).unlock();
        verify(this.document, never()).removeXObject(any());
        verify(this.pageManager, never()).save(any(), anyString());
    }

    @Test
    void saveWikiSettingsWithMissingTypeIsWrittenAndRejected() throws Exception
    {
        // A missing item type doesn't count as inheriting: the settings go to the mapper, which rejects them.
        Map<PurgeType, PurgeTypeSettings> partialSettings = Map.of(DELETED_DOCUMENTS, INHERIT);
        doThrow(new IllegalArgumentException("missing")).when(this.mapper)
            .writeSettings(this.wikiObject, WIKI_ID, partialSettings);

        assertThrows(IllegalArgumentException.class, () -> this.store.saveWikiSettings(WIKI_ID, partialSettings));

        verify(this.document, never()).removeXObject(any());
        verify(this.pageManager, never()).save(any(), anyString());
        verify(this.lock).unlock();
    }

    @Test
    void saveWikiSettingsWithoutWikiIdentifier() throws Exception
    {
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> this.store.saveWikiSettings(" ", CUSTOM_SETTINGS));

        assertEquals("The wiki identifier is missing.", exception.getMessage());
        verify(this.pageManager, never()).getGlobalLock();
    }
}
