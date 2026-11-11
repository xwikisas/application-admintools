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
package com.xwiki.admintools.purge.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xwiki.admintools.purge.PurgeHandler;
import com.xwiki.admintools.purge.internal.job.PurgeJob;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeHistoryStore;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.internal.store.mapper.PurgeSettingsMapper;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;
import com.xwiki.licensing.Licensor;

/**
 * Starts the purge jobs and counts the items they would remove. No rights are checked: the callers
 * must do it.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = PurgeManager.class)
@Singleton
public class PurgeManager
{
    /**
     * The space of the page whose license covers Admin Tools.
     */
    private static final List<String> LICENSED_PAGE_SPACE = List.of("AdminTools", "Code");

    /**
     * The context entries a purge job runs with: the user who started it and the current wiki. The purge itself doesn't
     * depend on that wiki, since the handlers switch to the purged wiki.
     */
    private static final List<String> JOB_CONTEXT = List.of("user", "wiki");

    @Inject
    private Provider<Licensor> licensorProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private PurgePolicyResolver policyResolver;

    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgeHistoryStore historyStore;

    @Inject
    private PurgeSettingsMapper settingsMapper;

    @Inject
    private Map<String, PurgeHandler> handlers;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ContextStoreManager contextStoreManager;

    @Inject
    private Logger logger;

    /**
     * Start a purge job for each wiki and enabled item type that has items past its limit. A wiki and item type whose
     * previous purge job is still queued or running is skipped. The settings and the purge history left behind by
     * deleted wikis are removed first.
     *
     * @throws JobException if the license of Admin Tools is not active, in which case nothing is purged
     * @throws WikiManagerException if the wikis can't be listed
     */
    public void dispatchDuePurges() throws JobException, WikiManagerException
    {
        if (!isLicensed()) {
            throw new JobException("The automatic purge was not started, since the Admin Tools license is not active.");
        }
        Collection<String> wikiIds = this.wikiDescriptorManager.getAllIds();
        removeDataOfDeletedWikis(wikiIds);
        for (String wikiId : wikiIds) {
            for (PurgeType type : PurgeType.values()) {
                try {
                    startScheduledPurge(wikiId, type);
                } catch (Exception e) {
                    this.logger.error("Failed to start the automatic purge of [{}] on wiki [{}].", type.getId(), wikiId,
                        e);
                }
            }
        }
    }

    /**
     * Start a job purging the items of an item type of a wiki that are past a limit chosen by the user, whatever the
     * settings of the wiki, even if the item type is disabled. The items are not counted first: the user already saw
     * how many items the purge removes. No job is started if the previous purge job of the same wiki and item type is
     * still queued or running. The limit is applied at the time the purge is started, so that the job removes the items
     * past the limit at that time, whatever the time the job runs.
     *
     * @param wikiId the identifier of the wiki to purge
     * @param type the item type to purge
     * @param limit the limit chosen by the user, whose mode is ignored
     * @param triggeredBy the user who started the purge
     * @return {@code true} if the job was started
     * @throws IllegalArgumentException if the limit is invalid
     * @throws ComponentLookupException if the context can't be saved for the job
     * @throws JobException if the job can't be started
     */
    public boolean startManualPurge(String wikiId, PurgeType type, PurgeTypeSettings limit,
        DocumentReference triggeredBy) throws ComponentLookupException, JobException
    {
        checkLimit(type, limit);
        return start(wikiId, type, triggeredBy, limit, Instant.now());
    }

    /**
     * Check the limit of a manual purge, with the rules of the settings of an enabled item type, except that the
     * interval can be 0, which purges every item deleted until now.
     *
     * @param type the item type
     * @param limit the limit chosen by the user, whose mode is ignored
     * @throws IllegalArgumentException if the limit is invalid
     */
    public void checkLimit(PurgeType type, PurgeTypeSettings limit)
    {
        this.settingsMapper.checkLimit(type, limit);
    }

    /**
     * @return {@code true} if the license of Admin Tools is active
     */
    public boolean isLicensed()
    {
        Licensor licensor = this.licensorProvider.get();
        return licensor != null && licensor.hasLicensure(
            new DocumentReference(this.wikiDescriptorManager.getMainWikiId(), LICENSED_PAGE_SPACE,
                "ConfigurationClass"));
    }

    /**
     * Remove the settings and the purge history of the wikis that no longer exist, left behind when their removal
     * failed after the wikis were deleted, or when the wikis were deleted while Admin Tools was not installed. A
     * failure is logged, without stopping the purge.
     *
     * @param wikiIds the identifiers of the existing wikis
     */
    private void removeDataOfDeletedWikis(Collection<String> wikiIds)
    {
        try {
            for (String wikiId : this.settingsStore.getWikiIdsWithSettings()) {
                if (!wikiIds.contains(wikiId)) {
                    this.settingsStore.removeWikiSettings(wikiId);
                }
            }
            for (String wikiId : this.historyStore.getWikiIdsWithHistory()) {
                if (!wikiIds.contains(wikiId)) {
                    this.historyStore.deleteHistory(wikiId);
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to remove the automatic purge data of the deleted wikis.", e);
        }
    }

    /**
     * Count the items of an item type of a wiki that are past a limit, which a manual purge with the same limit would
     * remove.
     *
     * @param wikiId the identifier of the wiki
     * @param type the item type
     * @param limit the limit chosen by the user, whose mode is ignored
     * @return the number of items past the limit
     * @throws IllegalArgumentException if the limit is invalid
     * @throws QueryException if the items can't be counted
     */
    public long countExpired(String wikiId, PurgeType type, PurgeTypeSettings limit) throws QueryException
    {
        checkLimit(type, limit);
        return this.handlers.get(type.getId()).countExpired(wikiId, limit, now());
    }

    /**
     * Start a job purging an item type of a wiki with the stored settings. No job is started if the item type is
     * disabled or has no items past its limit. The job applies the settings stored when it runs.
     *
     * @param wikiId the identifier of the wiki to purge
     * @param type the item type to purge
     * @throws XWikiException if the settings can't be loaded
     * @throws QueryException if the expired items can't be counted
     * @throws ComponentLookupException if the context can't be saved for the job
     * @throws JobException if the job can't be started
     */
    private void startScheduledPurge(String wikiId, PurgeType type)
        throws XWikiException, QueryException, ComponentLookupException, JobException
    {
        Optional<PurgeTypeSettings> settings = this.policyResolver.getSettings(wikiId, type)
            .filter(typeSettings -> typeSettings.getMode() == PurgeMode.ENABLED);
        if (settings.isEmpty() || this.handlers.get(type.getId()).countExpired(wikiId, settings.get(), now()) == 0) {
            this.logger.debug("Skipped the purge of [{}] on wiki [{}], since it has no expired items.", type.getId(),
                wikiId);
            return;
        }
        start(wikiId, type, null, null, null);
    }

    /**
     * @return the current time in the time zone of the server, which the scheduler fires at, so that the limits are
     *     applied in the same time zone
     */
    private ZonedDateTime now()
    {
        return ZonedDateTime.now(ZoneId.systemDefault());
    }

    /**
     * Start a job purging an item type of a wiki, unless the previous purge job of the same wiki and item type is still
     * queued or running. It is synchronized, so that two purges started at the same time can't both pass the check.
     *
     * @param wikiId the identifier of the wiki to purge
     * @param type the item type to purge
     * @param triggeredBy the user who started the purge manually, or {@code null} for a scheduled purge
     * @param limit the limit chosen by the user, or {@code null} to apply the settings stored when the job runs
     * @param startDate when the manual purge was started, or {@code null} for a scheduled purge
     * @return {@code true} if the job was started
     * @throws ComponentLookupException if the context can't be saved for the job
     * @throws JobException if the job can't be started
     */
    private synchronized boolean start(String wikiId, PurgeType type, DocumentReference triggeredBy,
        PurgeTypeSettings limit, Instant startDate) throws ComponentLookupException, JobException
    {
        Job job = this.jobExecutor.getJob(PurgeJobRequest.getJobId(wikiId, type));
        if (job != null && job.getStatus().getState() != JobStatus.State.FINISHED) {
            this.logger.debug("Skipped the purge of [{}] on wiki [{}], since its previous purge is not finished.",
                type.getId(), wikiId);
            return false;
        }
        PurgeJobRequest request = new PurgeJobRequest(wikiId, type, triggeredBy, limit, startDate);
        request.setContext(this.contextStoreManager.save(JOB_CONTEXT));
        this.jobExecutor.execute(PurgeJob.JOB_TYPE, request);
        return true;
    }
}
