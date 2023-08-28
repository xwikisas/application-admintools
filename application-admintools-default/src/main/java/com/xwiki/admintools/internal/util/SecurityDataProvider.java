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
package com.xwiki.admintools.internal.util;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Encapsulates functions used for retrieving security data.
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

    /**
     * XWiki configuration file source.
     */
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    /**
     * Generate the security details.
     *
     * @return the security details of the xwiki
     */
    public String provideData()
    {
        Map<String, String> securityDetails = this.getXwikiSecurityInfo();
        String workDirectory = "PWD";
        String language = "LANG";

        securityDetails.put("fileEncoding", getFileEncoding());
        securityDetails.put(workDirectory, System.getenv(workDirectory));
        securityDetails.put(language, System.getenv(language));

        return templateGenerator(securityDetails, "data/securityTemplate.vm", HINT);
    }

    @Override
    public String getIdentifier()
    {
        return HINT;
    }

    /**
     * Get the encoding used for the files.
     *
     * @return the file encoding
     */
    private String getFileEncoding()
    {
        return System.getProperty("file.encoding");
    }

    /**
     * Get the security info of the current wiki.
     *
     * @return xwiki security info.
     */
    private Map<String, String> getXwikiSecurityInfo()
    {
        Map<String, String> results = new HashMap<>();

        XWikiContext wikiContext = xcontextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        String wikiEncoding = wiki.getEncoding();
        String cfgEncoding = configurationSource.getProperty("xwiki.encoding", String.class);

        results.put("activeEncoding", wikiEncoding);
        results.put("configurationEncoding", cfgEncoding);

        return results;
    }
}
