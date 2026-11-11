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
package com.xwiki.admintools.purge.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Configures and starts the automatic purge. Every endpoint requires admin rights on the main wiki and an active
 * license, and the {@code POST} endpoints also require a valid {@code XWiki-Form-Token} header.
 *
 * @version $Id$
 * @since 1.5
 */
@Path("/admintools/autopurge")
@Unstable
public interface AutoPurgeResource extends XWikiRestComponent
{
    /**
     * Save the default automatic purge settings, used by every wiki that doesn't customize an item type. The form
     * fields are, for each item type: {@code <typeId>Mode} ({@code enabled} or {@code disabled}),
     * {@code <typeId>Interval} and {@code <typeId>Unit} ({@code days}, {@code weeks}, {@code months} or {@code years}),
     * {@code <typeId>MaxVersions} for a history item type (the number of versions kept, between 1 and 1000), plus
     * {@code maxHistoryEntries}, the number of purge history entries kept for each item type of each wiki, between 1
     * and 200. An enabled item type needs its interval, between 0 and 1000 (0 purges every item deleted until the purge
     * runs), and its unit; an enabled history item type needs them, the number of versions, or both, and the ones left
     * out are removed. They can all be left out for an item type that is not enabled, which then keeps its stored
     * ones.
     *
     * @return status code 204 if the settings were saved; 400 if a setting is missing or invalid; 401 if the user is
     *     not an admin of the main wiki; 402 if the license is not active; 403 if the form token is invalid
     * @throws XWikiRestException if an error occurred while saving the settings
     */
    @POST
    @Path("/settings")
    default Response saveDefaultSettings() throws XWikiRestException
    {
        throw new WebApplicationException(501);
    }

    /**
     * Save the automatic purge settings of a wiki. The form fields are the same as for {@link #saveDefaultSettings()},
     * without {@code maxHistoryEntries}, and the mode can also be {@code inherit}, to use the default settings of the
     * item type.
     *
     * @param wikiId the identifier of the wiki
     * @return status code 204 if the settings were saved; 400 if a setting is missing or invalid; 401 if the user is
     *     not an admin of the main wiki; 402 if the license is not active; 403 if the form token is invalid; 404 if the
     *     wiki doesn't exist
     * @throws XWikiRestException if an error occurred while saving the settings
     */
    @POST
    @Path("/wikis/{wikiId}/settings")
    default Response saveWikiSettings(@PathParam("wikiId") String wikiId) throws XWikiRestException
    {
        throw new WebApplicationException(501);
    }

    /**
     * Remove the automatic purge settings of a wiki, so that it uses the default settings for every item type.
     *
     * @param wikiId the identifier of the wiki
     * @return status code 204 if the wiki has no settings of its own anymore; 401 if the user is not an admin of the
     *     main wiki; 402 if the license is not active; 403 if the form token is invalid; 404 if the wiki doesn't exist
     * @throws XWikiRestException if an error occurred while removing the settings
     */
    @DELETE
    @Path("/wikis/{wikiId}/settings")
    default Response deleteWikiSettings(@PathParam("wikiId") String wikiId) throws XWikiRestException
    {
        throw new WebApplicationException(501);
    }

    /**
     * Start purging a wiki now, with a limit chosen for each item type, whatever the settings of the wiki, even for a
     * disabled item type. The form field {@code type}, which can be repeated, holds the identifiers of the item types
     * to purge, and the form fields {@code <typeId>Interval} and {@code <typeId>Unit} the limit of each of them, an
     * interval between 0 and 1000: the items deleted before it are purged, and 0 purges every item deleted until now.
     * A history item type can have {@code <typeId>MaxVersions} instead, or as well: the number of versions kept,
     * between 1 and 1000; the current version is never purged.
     * The items are not counted first: the caller is expected to show the count of {@link #previewPurge(String)}
     * before. An item type is not purged if its previous purge is still queued or running.
     *
     * @param wikiId the identifier of the wiki
     * @return status code 202 with the JSON list of the identifiers of the started jobs; 400 if no item type is given,
     *     if one is unknown, or if a limit is missing or invalid; 401 if the user is not an admin of the main wiki; 402
     *     if the license is not active; 403 if the form token is invalid; 404 if the wiki doesn't exist
     * @throws XWikiRestException if an error occurred while starting the purge
     */
    @POST
    @Path("/wikis/{wikiId}/purge")
    default Response purgeWiki(@PathParam("wikiId") String wikiId) throws XWikiRestException
    {
        throw new WebApplicationException(501);
    }

    /**
     * Count the items of a wiki that a manual purge would remove. The optional query parameters
     * {@code <typeId>Interval} (between 0 and 1000), {@code <typeId>Unit} and, for a history item type,
     * {@code <typeId>MaxVersions} (between 1 and 1000) set the limit of an item type; by default, the limit stored for
     * the item type applies, even if it is disabled. The optional query parameter {@code type}, which can be repeated,
     * restricts the counted item types, since counting a history can be slow; by default, every item type is
     * counted.
     *
     * @param wikiId the identifier of the wiki
     * @return status code 200 with a JSON object holding, by item type identifier: {@code enabled}, whether the
     *     automatic purge of the item type is enabled for the wiki; {@code interval}, {@code unit} and
     *     {@code maxVersions}, the parts of the limit used; {@code count}, the number of items past the limit, only for
     *     a counted item type; 400 if a requested limit or item type is invalid; 401 if the user is not an admin of the
     *     main wiki; 402 if the license is not active; 404 if the wiki doesn't exist
     * @throws XWikiRestException if an error occurred while counting the items
     */
    @GET
    @Path("/wikis/{wikiId}/preview")
    default Response previewPurge(@PathParam("wikiId") String wikiId) throws XWikiRestException
    {
        throw new WebApplicationException(501);
    }
}
