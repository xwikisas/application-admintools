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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.admintools.internal.DataProvider;

/**
 * To be extended by {@link DataProvider} implementations to simplify the code.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractDataProvider implements DataProvider
{
    @Inject
    protected TemplateManager templateManager;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    protected Logger logger;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public void initialize() throws InitializationException
    {
        // Overwrite to initialize a component
    }

    /**
     * Generates the template of a data provider.
     *
     * @param data contains the data to be shown in the template.
     * @param template path to the template.
     * @param hint data provider identifier.
     * @return String containing the generated template.
     */
    protected String templateGenerator(Map<String, String> data, String template, String hint)
    {
        Writer writer = new StringWriter();
        Template customTemplate = this.templateManager.getTemplate(template);
        try {
            // Set a document in the context to act as the current document when the template is rendered.
            this.bindData(hint, data);
            this.templateManager.render(customTemplate, writer);
            return writer.toString();
        } catch (Exception e) {
            logger.warn("Failed to render custom template. Root cause is: [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
        return null;
    }

    /**
     * Binds the data provided by the DataProvider to the template.
     *
     * @param key
     * @param data
     */
    private void bindData(String key, Map<String, String> data)
    {
        ScriptContext scriptContext = scriptContextManager.getScriptContext();

        scriptContext.setAttribute(key, data, ScriptContext.ENGINE_SCOPE);
    }
}
