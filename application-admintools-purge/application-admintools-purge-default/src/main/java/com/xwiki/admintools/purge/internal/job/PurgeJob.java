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
package com.xwiki.admintools.purge.internal.job;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobException;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.admintools.purge.model.PurgeHistoryEntry;
import com.xwiki.admintools.purge.model.PurgeStatus;
import com.xwiki.admintools.purge.model.PurgeType;

/**
 * Purges an item type from the recycle bin of a wiki, applying the limit stored when the job runs, and records the
 * result in the purge history. The jobs of an item type share one group, so the wikis are purged one after another,
 * while the item types are purged in parallel.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(PurgeJob.JOB_TYPE)
public class PurgeJob extends AbstractJob<PurgeJobRequest, DefaultJobStatus<PurgeJobRequest>> implements GroupedJob
{
    /**
     * The type of the purge job.
     */
    public static final String JOB_TYPE = "admintools.purge";

    @Inject
    private PurgePolicyResolver policyResolver;

    @Inject
    private PurgeHistoryStore historyStore;

    @Inject
    private Map<String, PurgeHandler> handlers;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return new JobGroupPath(List.of("adminTools", "purge", this.request.getType().getId()));
    }

    /**
     * Purge the item type if it is enabled for the wiki, and record the purge in the history. Nothing is done for a
     * wiki deleted after the job was queued. A scheduled purge is recorded only when it removed items, or when it
     * didn't succeed.
     *
     * @throws JobException if the item type can't be purged
     */
    @Override
    protected void runInternal() throws JobException
    {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        PurgeType type = this.request.getType();
        String wikiId = this.request.getWikiId();
        DocumentReference triggeredBy = this.request.getTriggeredBy().orElse(null);
        try {
            if (!this.wikiDescriptorManager.exists(wikiId)) {
                // The wiki was deleted while the job was queued: its purge history was deleted too.
                this.logger.debug("Skipped the purge of [{}] on wiki [{}], since the wiki no longer exists.",
                    type.getId(), wikiId);
                return;
            }
            Optional<Instant> expirationDate = this.policyResolver.getExpirationDate(wikiId, type, now);
            if (expirationDate.isEmpty()) {
                this.logger.debug("Skipped the purge of [{}] on wiki [{}], since it is disabled.", type.getId(),
                    wikiId);
                return;
            }
            PurgeHistoryEntry entry =
                this.handlers.get(type.getId()).purge(wikiId, expirationDate.get(), now.toInstant(), triggeredBy);
            // The history store skips the entry if the wiki was deleted in the meantime.
            if (triggeredBy != null || entry.getRemovedCount() > 0 || entry.getStatus() != PurgeStatus.SUCCESS) {
                this.historyStore.addEntry(wikiId, entry);
            }
        } catch (Exception e) {
            throw new JobException(String.format("Failed to purge [%s] on wiki [%s].", type.getId(), wikiId), e);
        }
    }
}
