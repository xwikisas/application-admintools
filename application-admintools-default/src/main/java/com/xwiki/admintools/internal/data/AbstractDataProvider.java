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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.internal.util.DefaultTemplateRender;

/**
 * {@link DataProvider} implementations to simplify the code.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractDataProvider implements DataProvider, Initializable
{
    protected final String serverFound = "serverFound";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected Logger logger;

    @Inject
    private DefaultTemplateRender defaultTemplateRender;

    @Override
    public void initialize() throws InitializationException
    {
        // Overwrite to initialize a component
    }

    @Override
    public String renderTemplate(String template, Map<String, String> data, String hint)
    {
        return defaultTemplateRender.getRenderedTemplate(template, data, hint);
    }
}
