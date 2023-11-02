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
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.DataProvider;

/**
 * {@link DataProvider} implementations to simplify the code.
 *
 * @version $Id$
 */
public abstract class AbstractDataProvider implements DataProvider
{
    protected static final String SERVER_FOUND = "serverFound";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected Logger logger;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * Get the data in a format given by the associated template.
     *
     * @param data {@link Map} with info to be shown in the template.
     * @param template name of the template.
     * @param hint {@link String} component name.
     * @return the rendered template as a {@link String}.
     */
    protected String renderTemplate(String template, Map<String, String> data, String hint)
    {
        try {
            // Binds the data provided to the template.
            ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
            scriptContext.setAttribute(hint, data, ScriptContext.ENGINE_SCOPE);
            return this.templateManager.render(template);
        } catch (Exception e) {
            this.logger.warn("Failed to render custom template. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
