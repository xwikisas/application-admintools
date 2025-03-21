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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.licensing.Licensor;

/**
 * Retrieve data about the security cache rules and access.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = CheckSecurityCache.class)
@Singleton
public class CheckSecurityCache
{
    private static final String ERROR_TEMPLATE = "licenseError.vm";

    private static final String NULL_VALUE = "null";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected Logger logger;

    @Inject
    private SecurityCache securityCache;

    @Inject
    private SecurityEntryReader securityEntryReader;

    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Provider<Licensor> licensorProvider;

    /**
     * Retrieve the cached and live security rules in a table format given by the associated template.
     *
     * @param userRef the user for which to check the access cache rules.
     * @param docRef the document on which to check the security rules.
     * @return the cached and live security rules in a table format given by the associated template.
     * @throws AuthorizationException if any error occurs while accessing the security entry.
     */
    public String displaySecurityCheck(DocumentReference userRef, DocumentReference docRef)
        throws AuthorizationException
    {
        List<SecurityRuleEntry> cachedRuleEntries = new ArrayList<>();
        List<SecurityAccessEntry> cachedAccessEntries = new ArrayList<>();
        List<SecurityRuleEntry> ruleEntries = new ArrayList<>();

        UserSecurityReference userSecurityRef = securityReferenceFactory.newUserReference(userRef);
        addSecurityRules(docRef, userSecurityRef, cachedAccessEntries, cachedRuleEntries, ruleEntries);
        for (SpaceReference spaceRef : docRef.getSpaceReferences()) {
            addSecurityRules(spaceRef, userSecurityRef, cachedAccessEntries, cachedRuleEntries, ruleEntries);
        }
        addSecurityRules(docRef.getWikiReference(), userSecurityRef, cachedAccessEntries, cachedRuleEntries,
            ruleEntries);

        return renderTemplate(cachedRuleEntries, cachedAccessEntries, ruleEntries);
    }

    /**
     * Extract the users and groups from the given {@link SecurityRule}.
     *
     * @param input the rule from which to extract the data.
     * @return a {@link Map} with the 'Users' and 'Groups' as keys and the extracted info from the rule as values.
     */
    public Map<String, String> extractRuleUsersGroups(SecurityRule input)
    {
        Map<String, String> keyValuePairs = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("(Users|Groups)=\\[(.*?)\\](?=,[A-Z]|$)");
        Matcher matcher = pattern.matcher(input.toString().replace(" ", ""));
        while (matcher.find()) {
            String key = matcher.group(1);
            String rawValue = matcher.group(2);
            String value = !rawValue.contains(NULL_VALUE) ? rawValue : rawValue.replace(NULL_VALUE, "XWikiGuest");
            keyValuePairs.put(key, value);
        }
        return keyValuePairs;
    }

    private void addSecurityRules(EntityReference docRef, UserSecurityReference userSecurityRef,
        List<SecurityAccessEntry> cachedAccessEntries, List<SecurityRuleEntry> cachedRuleEntries,
        List<SecurityRuleEntry> ruleEntries) throws AuthorizationException
    {
        SecurityReference docSecureRef = securityReferenceFactory.newEntityReference(docRef);

        SecurityRuleEntry cachedRuleEntry = securityCache.get(docSecureRef);
        SecurityAccessEntry cachedAccessEntry = securityCache.get(userSecurityRef, docSecureRef);
        SecurityRuleEntry securityRuleEntry = securityEntryReader.read(docSecureRef);
        cachedAccessEntries.add(cachedAccessEntry);
        cachedRuleEntries.add(cachedRuleEntry);
        ruleEntries.add(securityRuleEntry);
    }

    private String renderTemplate(List<SecurityRuleEntry> cachedRuleEntries,
        List<SecurityAccessEntry> cachedAccessEntries, List<SecurityRuleEntry> ruleEntries)
    {
        try {
            Licensor licensor = licensorProvider.get();
            String wiki = xcontextProvider.get().getWikiId();
            DocumentReference mainRef =
                new DocumentReference(wiki, Arrays.asList("AdminTools", "Code"), "ConfigurationClass");
            if (licensor == null || !licensor.hasLicensure(mainRef)) {
                return this.templateManager.render(ERROR_TEMPLATE);
            }

            // Binds the data provided to the template.
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            scriptContext.setAttribute("cachedRuleEntries", cachedRuleEntries, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("cachedAccessEntries", cachedAccessEntries, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("ruleEntries", ruleEntries, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute("rights", Right.getStandardRights(), ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render("securityCacheViewTemplate.vm");
        } catch (Exception e) {
            this.logger.warn("Failed to render custom template. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
