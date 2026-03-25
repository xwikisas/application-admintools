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
package com.xwiki.admintools.internal.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.licensing.Licensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class CheckSecurityCacheTest
{
    private static final String ERROR_TEMPLATE = "licenseError.vm";

    private static final DocumentReference MAIN_REF =
        new DocumentReference("testWiki", Arrays.asList("AdminTools", "Code"), "ConfigurationClass");

    private static final String SECURITY_CACHE_TEMPLATE = "securityCacheViewTemplate.vm";

    @InjectMockComponents
    private CheckSecurityCache checkSecurityCache;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @Mock
    private ScriptContext scriptContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Provider<Licensor> licensorProvider;

    @Mock
    private Licensor licensor;

    @MockComponent
    private SecurityReferenceFactory securityReferenceFactory;

    @MockComponent
    private SecurityEntryReader securityEntryReader;

    @MockComponent
    private SecurityCache securityCache;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private XWikiContext context;

    @Mock
    private DocumentReference userRef;

    @Mock
    private DocumentReference docRef;

    @Mock
    private UserSecurityReference userSecurityReference;

    @Mock
    private SecurityReference docSecurityReference;

    @Mock
    private SecurityReference spaceSecurityReference;

    @Mock
    private SecurityReference wikiSecurityReference;

    @Mock
    private SecurityRuleEntry cachedRuleEntry;

    @Mock
    private SecurityRuleEntry securityRuleEntry;

    @Mock
    private SecurityAccessEntry cachedAccessEntry;

    @Mock
    private SecurityRuleEntry spaceCachedRuleEntry;

    @Mock
    private SecurityRuleEntry spaceSecurityRuleEntry;

    @Mock
    private SecurityAccessEntry spaceCachedAccessEntry;

    @Mock
    private SecurityRuleEntry wikiCachedRuleEntry;

    @Mock
    private SecurityRuleEntry wikiSecurityRuleEntry;

    @Mock
    private SecurityAccessEntry wikiCachedAccessEntry;

    @Mock
    private SpaceReference spaceReference;

    @Mock
    private WikiReference wikiReference;

    @Mock
    private SecurityRule securityRule;

    @BeforeEach
    void setUp() throws XWikiException
    {
        when(xcontextProvider.get()).thenReturn(context);
        when(context.getWikiId()).thenReturn("testWiki");
        when(licensorProvider.get()).thenReturn(licensor);
        when(licensor.hasLicensure(MAIN_REF)).thenReturn(true);
        when(scriptContextManager.getScriptContext()).thenReturn(scriptContext);
        when(docRef.getSpaceReferences()).thenReturn(List.of(spaceReference));
        when(docRef.getWikiReference()).thenReturn(wikiReference);
    }

    @Test
    void displaySecurityCheck() throws Exception
    {
        when(securityReferenceFactory.newUserReference(userRef)).thenReturn(userSecurityReference);
        when(securityReferenceFactory.newEntityReference(docRef)).thenReturn(docSecurityReference);
        when(securityReferenceFactory.newEntityReference(spaceReference)).thenReturn(spaceSecurityReference);
        when(securityReferenceFactory.newEntityReference(wikiReference)).thenReturn(wikiSecurityReference);

        when(securityCache.get(docSecurityReference)).thenReturn(cachedRuleEntry);
        when(securityCache.get(userSecurityReference, docSecurityReference)).thenReturn(cachedAccessEntry);
        when(securityEntryReader.read(docSecurityReference)).thenReturn(securityRuleEntry);

        when(securityCache.get(spaceSecurityReference)).thenReturn(spaceCachedRuleEntry);
        when(securityCache.get(userSecurityReference, spaceSecurityReference)).thenReturn(spaceCachedAccessEntry);
        when(securityEntryReader.read(spaceSecurityReference)).thenReturn(spaceSecurityRuleEntry);

        when(securityCache.get(wikiSecurityReference)).thenReturn(wikiCachedRuleEntry);
        when(securityCache.get(userSecurityReference, wikiSecurityReference)).thenReturn(wikiCachedAccessEntry);
        when(securityEntryReader.read(wikiSecurityReference)).thenReturn(wikiSecurityRuleEntry);

        when(templateManager.render(SECURITY_CACHE_TEMPLATE)).thenReturn("Rendered Template");

        String result = checkSecurityCache.displaySecurityCheck(userRef, docRef);

        assertEquals("Rendered Template", result);
        verify(scriptContext).setAttribute("cachedRuleEntries",
            List.of(cachedRuleEntry, spaceCachedRuleEntry, wikiCachedRuleEntry), scriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("cachedAccessEntries",
            List.of(cachedAccessEntry, spaceCachedAccessEntry, wikiCachedAccessEntry), scriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute("ruleEntries",
            List.of(securityRuleEntry, spaceSecurityRuleEntry, wikiSecurityRuleEntry), scriptContext.ENGINE_SCOPE);
    }

    @Test
    void displaySecurityCheck_nullLicense() throws Exception
    {
        when(securityReferenceFactory.newUserReference(userRef)).thenReturn(userSecurityReference);
        when(securityReferenceFactory.newEntityReference(docRef)).thenReturn(docSecurityReference);
        when(securityReferenceFactory.newEntityReference(spaceReference)).thenReturn(spaceSecurityReference);
        when(securityReferenceFactory.newEntityReference(wikiReference)).thenReturn(wikiSecurityReference);
        when(licensor.hasLicensure(MAIN_REF)).thenReturn(false);
        when(templateManager.render(ERROR_TEMPLATE)).thenReturn("render error");

        assertEquals("render error", checkSecurityCache.displaySecurityCheck(userRef, docRef));
    }

    @Test
    void displaySecurityCheck_renderError() throws Exception
    {
        when(securityReferenceFactory.newUserReference(userRef)).thenReturn(userSecurityReference);
        when(securityReferenceFactory.newEntityReference(docRef)).thenReturn(docSecurityReference);
        when(securityReferenceFactory.newEntityReference(spaceReference)).thenReturn(spaceSecurityReference);
        when(securityReferenceFactory.newEntityReference(wikiReference)).thenReturn(wikiSecurityReference);
        when(licensor.hasLicensure(MAIN_REF)).thenReturn(true);
        when(templateManager.render(SECURITY_CACHE_TEMPLATE)).thenThrow(new RuntimeException("render error"));

        assertNull(checkSecurityCache.displaySecurityCheck(userRef, docRef));
        assertEquals("Failed to render custom template. Root cause is: [RuntimeException: render error]",
            logCapture.getMessage(0));
    }

    @Test
    void displaySecurityCheck_noRights() throws AuthorizationException
    {
        when(securityReferenceFactory.newUserReference(userRef)).thenReturn(userSecurityReference);
        when(securityReferenceFactory.newEntityReference(docRef)).thenReturn(docSecurityReference);
        when(securityEntryReader.read(docSecurityReference)).thenThrow(new AuthorizationException("Error"));

        assertThrows(AuthorizationException.class, () -> checkSecurityCache.displaySecurityCheck(userRef, docRef));
    }

    @Test
    void extractRuleUsersGroups()
    {
        when(securityRule.toString()).thenReturn("Users=[XWiki.Admin,null], Groups=[XWiki.AllGroup]");
        Map<String, String> expectedRes = Map.of("Users", "XWiki.Admin,XWikiGuest", "Groups", "XWiki.AllGroup");
        Map<String, String> res = checkSecurityCache.extractRuleUsersGroups(securityRule);
        assertEquals(expectedRes.get("Users"), res.get("Users"));
        assertEquals(expectedRes.get("Groups"), res.get("Groups"));
    }
}