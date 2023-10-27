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
package com.xwiki.admintools.internal.data;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiContext;

/**
 * Extension of {@link AbstractDataProvider} for retrieving security data, like XWiki encoding, file encoding, system
 * info.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(SecurityDataProvider.HINT)
@Singleton
public class SecurityDataProvider extends AbstractDataProvider
{
    /**
     * The hint for the component.
     */
    public static final String HINT = "security";

    private static final String WORK_DIRECTORY = "PWD";

    private static final String LANGUAGE = "LANG";

    /**
     * XWiki configuration file source.
     */
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @Override
    public String getRenderedData()
    {
        Map<String, String> securityDetails = new HashMap<>();
        try {
            securityDetails = getDataAsJSON();
            securityDetails.put(SERVER_FOUND, "true");
        } catch (Exception e) {
            securityDetails.put(SERVER_FOUND, "false");
        }
        return renderTemplate("securityTemplate.vm", securityDetails, HINT);
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    @Override
    public Map<String, String> getDataAsJSON() throws Exception
    {
        try {
            Map<String, String> securityDetails = this.getXwikiSecurityInfo();
            securityDetails.putAll(getEnvironmentInfo());
            securityDetails.put("fileEncoding", System.getProperty("file.encoding"));
            return securityDetails;
        } catch (Exception e) {
            logger.warn("Failed to generate the instance security data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            throw new Exception("Failed to generate the instance security data.", e);
        }
    }

    /**
     * Get the security info of the current wiki.
     *
     * @return a {@link Map} with XWiki security info regarding used and active encodings.
     */
    private Map<String, String> getXwikiSecurityInfo() throws Exception
    {
        try {
            Map<String, String> results = new HashMap<>();

            XWikiContext wikiContext = this.xcontextProvider.get();
            results.put("activeEncoding", wikiContext.getWiki().getEncoding());
            results.put("configurationEncoding", this.configurationSource.getProperty("xwiki.encoding", String.class));
            return results;
        } catch (Exception e) {
            throw new Exception("Failed to generate xwiki security info.", e);
        }
    }

    /**
     * Get the security info regarding the environment.
     *
     * @return {@link Map} with environment info regarding working directory and system language.
     */
    private Map<String, String> getEnvironmentInfo()
    {
        Map<String, String> results = new HashMap<>();
        String workDirectory = System.getenv(WORK_DIRECTORY);
        String language = System.getenv(LANGUAGE);
        results.put(WORK_DIRECTORY, System.getenv(WORK_DIRECTORY));
        results.put(LANGUAGE, System.getenv(LANGUAGE));
        if (workDirectory == null || language == null) {
            this.logger.warn("Failed to access language or work directory environment variables.");
        }
        return results;
    }
}
