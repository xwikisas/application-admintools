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
package com.xwiki.admintools.purge.script;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.purge.internal.PurgeManager;
import com.xwiki.admintools.purge.internal.PurgeWikisDataProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AutoPurgeScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class AutoPurgeScriptServiceTest
{
    private static final WikiReference MAIN_WIKI = new WikiReference("xwiki");

    private static final Map<String, String> FILTERS = Map.of("wikiName", "sub");

    @InjectMockComponents
    private AutoPurgeScriptService scriptService;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private PurgeWikisDataProvider purgeWikisDataProvider;

    @MockComponent
    private PurgeManager purgeManager;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        when(this.purgeManager.isLicensed()).thenReturn(true);
    }

    @Test
    void getWikis() throws Exception
    {
        List<Map<String, Object>> rows = List.of(Map.of("wikiId", "subwiki"));
        when(this.purgeWikisDataProvider.getWikis(FILTERS, "asc")).thenReturn(rows);

        assertSame(rows, this.scriptService.getWikis(FILTERS, "asc"));
        verify(this.contextualAuthorizationManager).checkAccess(Right.ADMIN, MAIN_WIKI);
    }

    @Test
    void getWikisWithoutAdminRightsOnTheMainWiki() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.contextualAuthorizationManager)
            .checkAccess(Right.ADMIN, MAIN_WIKI);

        assertThrows(AccessDeniedException.class, () -> this.scriptService.getWikis(FILTERS, "asc"));
        verify(this.purgeWikisDataProvider, never()).getWikis(any(), any());
    }

    @Test
    void getWikisWithoutLicense() throws Exception
    {
        when(this.purgeManager.isLicensed()).thenReturn(false);

        assertEquals(List.of(), this.scriptService.getWikis(FILTERS, "asc"));
        verify(this.purgeWikisDataProvider, never()).getWikis(any(), any());
    }
}
