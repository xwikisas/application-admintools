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
package com.xwiki.admintools.purge.jobs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.purge.model.PurgeType;

/**
 * A request to purge one item type from the recycle bin of a wiki. A scheduled purge holds no limit: the job applies
 * the settings stored when it runs. A manual purge holds the expiration date computed from the limit chosen when it
 * was started, so that it removes the items past that limit at that time, whatever the settings.
 *
 * @version $Id$
 * @since 1.5
 */
@Unstable
public class PurgeJobRequest extends AbstractRequest
{
    private static final long serialVersionUID = 1L;

    private final String wikiId;

    private final PurgeType type;

    private final DocumentReference triggeredBy;

    private final Instant expirationDate;

    /**
     * Create a request to purge an item type of a wiki. The job status is not serialized, since the result of the
     * purge is stored in the purge history, and the job log goes to the server log, since nobody watches the job.
     *
     * @param wikiId the identifier of the wiki
     * @param type the item type to purge
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge
     * @param expirationDate the items deleted before this date are purged, or {@code null} to apply the settings
     *     stored when the job runs; a manual purge must pass it, computed from the limit chosen by the user, since
     *     without it the job applies the stored settings and skips a disabled item type, like a scheduled purge
     */
    public PurgeJobRequest(String wikiId, PurgeType type, DocumentReference triggeredBy, Instant expirationDate)
    {
        this.wikiId = wikiId;
        this.type = type;
        this.triggeredBy = triggeredBy;
        this.expirationDate = expirationDate;
        setId(getJobId(wikiId, type));
        setStatusSerialized(false);
        setStatusLogIsolated(false);
    }

    /**
     * @param wikiId the identifier of a wiki
     * @param type an item type
     * @return the identifier of the job purging the item type of the wiki
     */
    public static List<String> getJobId(String wikiId, PurgeType type)
    {
        return List.of("adminTools", "purge", type.getId(), wikiId);
    }

    /**
     * @return the identifier of the wiki
     */
    public String getWikiId()
    {
        return this.wikiId;
    }

    /**
     * @return the item type to purge
     */
    public PurgeType getType()
    {
        return this.type;
    }

    /**
     * @return the user who started the purge manually, or an empty value for a scheduled purge
     */
    public Optional<DocumentReference> getTriggeredBy()
    {
        return Optional.ofNullable(this.triggeredBy);
    }

    /**
     * @return the date before which the deleted items are purged, or an empty value if the job applies the settings
     *     stored when it runs
     */
    public Optional<Instant> getExpirationDate()
    {
        return Optional.ofNullable(this.expirationDate);
    }
}
