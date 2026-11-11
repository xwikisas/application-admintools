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
package com.xwiki.admintools.purge.internal.rest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xwiki.admintools.purge.internal.PurgeManager;
import com.xwiki.admintools.purge.internal.policy.PurgePolicyResolver;
import com.xwiki.admintools.purge.internal.store.PurgeSettingsStore;
import com.xwiki.admintools.purge.jobs.PurgeJobRequest;
import com.xwiki.admintools.purge.model.PurgeMode;
import com.xwiki.admintools.purge.model.PurgeType;
import com.xwiki.admintools.purge.model.PurgeTypeSettings;
import com.xwiki.admintools.purge.rest.AutoPurgeResource;

/**
 * Default implementation of {@link AutoPurgeResource}.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named("com.xwiki.admintools.purge.internal.rest.DefaultAutoPurgeResource")
@Singleton
public class DefaultAutoPurgeResource extends XWikiResource implements AutoPurgeResource
{
    private static final String FORM_TOKEN_HEADER = "XWiki-Form-Token";

    /**
     * The status code returned when the license is not active (Payment Required), which JAX-RS 1 doesn't name.
     */
    private static final int LICENSE_INACTIVE = 402;

    private static final String MAX_HISTORY_ENTRIES = "maxHistoryEntries";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    private Logger logger;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private CSRFToken csrfToken;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private PurgeManager purgeManager;

    @Inject
    private PurgeRequestReader requestReader;

    @Inject
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgePolicyResolver policyResolver;

    @Override
    public Response saveDefaultSettings()
    {
        return handle(null, true, "Failed to save the automatic purge defaults.", () -> {
            XWikiRequest request = this.xcontextProvider.get().getRequest();
            this.settingsStore.saveGlobalSettings(this.requestReader.readTypeSettings(request),
                Integer.parseInt(request.getParameter(MAX_HISTORY_ENTRIES)));
            return Response.noContent().build();
        });
    }

    @Override
    public Response saveWikiSettings(String wikiId)
    {
        return handle(wikiId, true, String.format("Failed to save the automatic purge settings of wiki [%s].", wikiId),
            () -> {
                this.settingsStore.saveWikiSettings(wikiId,
                    this.requestReader.readTypeSettings(this.xcontextProvider.get().getRequest()));
                return Response.noContent().build();
            });
    }

    @Override
    public Response deleteWikiSettings(String wikiId)
    {
        return handle(wikiId, true,
            String.format("Failed to remove the automatic purge settings of wiki [%s].", wikiId), () -> {
                this.settingsStore.removeWikiSettings(wikiId);
                return Response.noContent().build();
            });
    }

    @Override
    public Response purgeWiki(String wikiId)
    {
        return handle(wikiId, true, String.format("Failed to start the purge of wiki [%s].", wikiId),
            () -> {
                XWikiContext context = this.xcontextProvider.get();
                XWikiRequest request = context.getRequest();
                Set<PurgeType> types = this.requestReader.readTypes(request);
                if (types.isEmpty()) {
                    throw new IllegalArgumentException("No item type to purge.");
                }
                // Check every item type and its limit before starting any job.
                Map<PurgeType, PurgeTypeSettings> limits = new LinkedHashMap<>();
                for (PurgeType type : types) {
                    PurgeTypeSettings limit = this.requestReader.readLimit(request, type).orElseThrow(() ->
                        new IllegalArgumentException(String.format("The limit of [%s] is missing.", type.getId())));
                    this.purgeManager.checkLimit(type, limit);
                    limits.put(type, limit);
                }
                // A type that can't be started doesn't prevent the others, and the started jobs are still returned.
                List<List<String>> jobIds = new ArrayList<>();
                for (Map.Entry<PurgeType, PurgeTypeSettings> limit : limits.entrySet()) {
                    PurgeType type = limit.getKey();
                    try {
                        if (this.purgeManager.startManualPurge(wikiId, type, limit.getValue(),
                            context.getUserReference()))
                        {
                            jobIds.add(PurgeJobRequest.getJobId(wikiId, type));
                        }
                    } catch (Exception e) {
                        this.logger.error("Failed to start the purge of [{}] on wiki [{}].", type.getId(), wikiId, e);
                    }
                }
                return Response.status(Response.Status.ACCEPTED).entity(OBJECT_MAPPER.writeValueAsString(jobIds))
                    .type(MediaType.APPLICATION_JSON_TYPE).build();
            });
    }

    @Override
    public Response previewPurge(String wikiId)
    {
        return handle(wikiId, false,
            String.format("Failed to count the expired items of wiki [%s].", wikiId), () -> {
                XWikiRequest request = this.xcontextProvider.get().getRequest();
                Set<PurgeType> countedTypes = this.requestReader.readTypes(request);
                if (countedTypes.isEmpty()) {
                    countedTypes = EnumSet.allOf(PurgeType.class);
                }
                Map<String, Map<String, Object>> preview = new LinkedHashMap<>();
                for (PurgeType type : PurgeType.values()) {
                    Optional<PurgeTypeSettings> settings = this.policyResolver.getSettings(wikiId, type);
                    // Without a complete stored limit, the item type is counted only for a requested limit.
                    Optional<PurgeTypeSettings> limit = this.requestReader.readLimit(request, type)
                        .or(() -> settings.filter(this.requestReader::hasLimit));
                    Map<String, Object> typePreview = new LinkedHashMap<>();
                    typePreview.put("enabled",
                        settings.filter(typeSettings -> typeSettings.getMode() == PurgeMode.ENABLED).isPresent());
                    if (limit.isPresent()) {
                        PurgeTypeSettings typeLimit = limit.get();
                        typePreview.putAll(this.requestReader.getLimitFields(typeLimit));
                        if (countedTypes.contains(type)) {
                            // The purge manager rejects an invalid limit.
                            typePreview.put("count", this.purgeManager.countExpired(wikiId, type, typeLimit));
                        }
                    }
                    preview.put(type.getId(), typePreview);
                }
                return Response.ok(OBJECT_MAPPER.writeValueAsString(preview)).type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
            });
    }

    /**
     * Run an automatic purge request, once {@link #checkRequest(String, boolean)} accepts it. A missing or invalid
     * parameter is reported with status code 400. The responses of failed requests have no content, so that they expose
     * nothing.
     *
     * @param wikiId the identifier of the wiki the request is about, or {@code null} for the default settings
     * @param modifying {@code true} if the request changes something, so it needs a valid form token
     * @param errorMessage the message logged if the request fails
     * @param request the request to run
     * @return the response of the request
     */
    private Response handle(String wikiId, boolean modifying, String errorMessage, Callable<Response> request)
    {
        try {
            Optional<Response> rejection = checkRequest(wikiId, modifying);
            return rejection.isPresent() ? rejection.get() : request.call();
        } catch (AccessDeniedException e) {
            this.logger.warn("Failed to access the automatic purge due to restricted rights.");
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            this.logger.debug("Rejected an invalid automatic purge request.", e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            this.logger.error(errorMessage, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check that the current user is an admin of the main wiki, that the form token is valid for a request that changes
     * something, that the license is active and that the wiki exists.
     *
     * @param wikiId the identifier of the wiki the request is about, or {@code null} for the default settings
     * @param modifying {@code true} if the request changes something, so it needs a valid form token
     * @return the response rejecting the request, or an empty value if the request can run
     * @throws AccessDeniedException if the current user is not an admin of the main wiki
     * @throws WikiManagerException if the existence of the wiki can't be checked
     */
    private Optional<Response> checkRequest(String wikiId, boolean modifying)
        throws AccessDeniedException, WikiManagerException
    {
        XWikiContext context = this.xcontextProvider.get();
        this.authorizationManager.checkAccess(Right.ADMIN, context.getUserReference(),
            new WikiReference(this.wikiDescriptorManager.getMainWikiId()));
        if (modifying && !this.csrfToken.isTokenValid(context.getRequest().getHeader(FORM_TOKEN_HEADER))) {
            return Optional.of(Response.status(Response.Status.FORBIDDEN).build());
        }
        if (!this.purgeManager.isLicensed()) {
            return Optional.of(Response.status(LICENSE_INACTIVE).build());
        }
        if (wikiId != null && !this.wikiDescriptorManager.exists(wikiId)) {
            return Optional.of(Response.status(Response.Status.NOT_FOUND).build());
        }
        return Optional.empty();
    }
}
