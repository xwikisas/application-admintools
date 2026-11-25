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
package com.xwiki.admintools.purge.internal.handler;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Purges the deleted pages of a wiki.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(DeletedDocumentsPurgeHandler.HINT)
@Singleton
public class DeletedDocumentsPurgeHandler extends AbstractRecycleBinPurgeHandler
{
    /**
     * The hint for the component, which is the identifier of {@link PurgeType#DELETED_DOCUMENTS}.
     */
    public static final String HINT = "deletedDocuments";

    @Override
    protected PurgeType getType()
    {
        return PurgeType.DELETED_DOCUMENTS;
    }

    @Override
    protected String getEntityName()
    {
        return "XWikiDeletedDocument";
    }

    @Override
    protected String getItemNameColumns()
    {
        return "item.fullName, item.language";
    }

    @Override
    protected String getItemName(Object[] item)
    {
        return getPageName((String) item[0], (String) item[1]);
    }

    @Override
    protected void delete(long id, XWikiContext wikiContext) throws XWikiException
    {
        wikiContext.getWiki().getRecycleBinStore().deleteFromRecycleBin(id, wikiContext, true);
    }
}
