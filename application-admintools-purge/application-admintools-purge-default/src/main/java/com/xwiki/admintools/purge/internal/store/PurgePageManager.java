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
package com.xwiki.admintools.purge.internal.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Loads, saves and deletes the automatic purge pages, and provides the locks that serialize their modifications.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgePageManager.class)
@Singleton
public class PurgePageManager
{
    private final Map<String, Lock> wikiLocks = new ConcurrentHashMap<>();

    private final Lock globalLock = new ReentrantLock();

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    /**
     * Load a page. The returned document is the cached one, so it is meant for reading only; use
     * {@link #loadDocument(DocumentReference)} to modify a page. It is a new document if the page doesn't exist
     * yet.
     *
     * @param reference the reference of the page to load
     * @return the cached page
     * @throws XWikiException if the page can't be loaded
     */
    public XWikiDocument getDocument(DocumentReference reference) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        return wikiContext.getWiki().getDocument(reference, wikiContext);
    }

    /**
     * Load a page from the database, in order to modify it without affecting the cached page, or to read it while it
     * may be modified. It is a new document if the page doesn't exist yet.
     * <p>
     * The cache is skipped since it can hold an outdated version of a page that is modified concurrently: a request
     * that loads the page from the database just before it is saved puts that version in the cache just after the save
     * cleared it. Modifying that version would lose the changes saved in the meantime.
     *
     * @param reference the reference of the page to load
     * @return the page, not shared with the cache
     * @throws XWikiException if the page can't be loaded
     */
    public XWikiDocument loadDocument(DocumentReference reference) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        String currentWiki = wikiContext.getWikiId();
        try {
            // The store loads the page from the database of the current wiki.
            wikiContext.setWikiId(reference.getWikiReference().getName());
            return wikiContext.getWiki().getNotCacheStore().loadXWikiDoc(new XWikiDocument(reference), wikiContext);
        } finally {
            wikiContext.setWikiId(currentWiki);
        }
    }

    /**
     * @param document a page
     * @param classReference the reference of a class
     * @return a new object of the given class, added to the page
     * @throws XWikiException if the object can't be created
     */
    public BaseObject createObject(XWikiDocument document, EntityReference classReference) throws XWikiException
    {
        return document.newXObject(classReference, this.wikiContextProvider.get());
    }

    /**
     * Save a page with a new revision, for changes that must be traceable in the page history, like configuration
     * changes. A new page is created hidden.
     *
     * @param document the page to save
     * @param comment the comment of the new revision
     * @throws XWikiException if the page can't be saved
     */
    public void save(XWikiDocument document, String comment) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        UserReference currentUser = this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE);
        DocumentAuthors authors = document.getAuthors();
        authors.setEffectiveMetadataAuthor(currentUser);
        authors.setOriginalMetadataAuthor(currentUser);
        if (document.isNew()) {
            authors.setContentAuthor(currentUser);
            authors.setCreator(currentUser);
            document.setHidden(true);
        }
        wikiContext.getWiki().saveDocument(document, comment, false, wikiContext);
    }

    /**
     * Save a page without creating a new revision, for data written by the system that is updated often and doesn't
     * need to be traced, like the purge history. Objects removed from the page are deleted too. A new page is still
     * saved with a first revision.
     *
     * @param document the page to save
     * @param comment the comment of the first revision, used only when the page is new
     * @throws XWikiException if the page can't be saved
     */
    public void saveWithoutRevision(XWikiDocument document, String comment) throws XWikiException
    {
        if (document.isNew()) {
            save(document, comment);
        } else {
            // XWiki#saveDocument keeps the dirty flags when they are false, and the store then saves the objects
            // without incrementing the version or updating the page history. This relies on the behavior of
            // XWiki#saveDocument and XWikiHibernateStore#saveXWikiDoc in XWiki 15.10: check it again when moving to
            // a newer platform version.
            document.setContentDirty(false);
            document.setMetaDataDirty(false);
            XWikiContext wikiContext = this.wikiContextProvider.get();
            wikiContext.getWiki().saveDocument(document, comment, true, wikiContext);
        }
    }

    /**
     * Delete a page permanently, without sending it to the recycle bin.
     *
     * @param document the page to delete
     * @throws XWikiException if the page can't be deleted
     */
    public void delete(XWikiDocument document) throws XWikiException
    {
        XWikiContext wikiContext = this.wikiContextProvider.get();
        wikiContext.getWiki().deleteDocument(document, false, wikiContext);
    }

    /**
     * @return the lock to hold while loading, modifying and saving the settings page
     */
    public Lock getGlobalLock()
    {
        return this.globalLock;
    }

    /**
     * @param wikiId the identifier of a wiki
     * @return the lock to hold while loading, modifying and saving the purge history page of the given wiki
     */
    public Lock getWikiLock(String wikiId)
    {
        return this.wikiLocks.computeIfAbsent(wikiId, key -> new ReentrantLock());
    }
}
