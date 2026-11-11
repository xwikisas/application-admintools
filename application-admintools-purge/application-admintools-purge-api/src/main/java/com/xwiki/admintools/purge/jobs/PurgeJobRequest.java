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

import java.util.List;
import java.util.Optional;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.purge.model.PurgeType;

/**
 * A request to purge one item type from the recycle bin of a wiki. It holds no limit: the job applies the settings
 * stored when it runs.
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

    /**
     * Create a request to purge an item type of a wiki. The job status is not serialized, since the result of the
     * purge is stored in the purge history, and the job log goes to the server log, since nobody watches the job.
     *
     * @param wikiId the identifier of the wiki
     * @param type the item type to purge
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge
     */
    public PurgeJobRequest(String wikiId, PurgeType type, DocumentReference triggeredBy)
    {
        this.wikiId = wikiId;
        this.type = type;
        this.triggeredBy = triggeredBy;
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
}
