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
package com.xwiki.admintools.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Util class used to check if the instance is an internal XWiki cloud instance, or not.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = CloudDetectorUtil.class)
@Singleton
public class CloudDetectorUtil
{
    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    /**
     * Check if the instance is an internal XWiki cloud instance, by verifying the existence of cloud documents.
     *
     * @return {@code true} if it's a cloud instance, or {@code false} otherwise.
     * @throws XWikiException if an error occurs while checking if the specific cloud documents exist or not.
     */
    public boolean isCloud() throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        XWiki wiki = wikiContext.getWiki();
        return wiki.exists(this.referenceResolver.resolve("XWiki.CloudUIX"), wikiContext);
    }
}
