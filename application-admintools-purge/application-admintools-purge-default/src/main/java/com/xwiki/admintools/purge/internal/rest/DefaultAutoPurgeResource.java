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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
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
import com.xwiki.admintools.purge.model.IntervalUnit;
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

    private static final String TYPE = "type";

    private static final String MAX_HISTORY_ENTRIES = "maxHistoryEntries";

    private static final String INTERVAL_SUFFIX = "Interval";

    private static final String UNIT_SUFFIX = "Unit";

    private static final String INVALID_VALUE = "Invalid value [%s] for [%s].";

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
    private PurgeSettingsStore settingsStore;

    @Inject
    private PurgePolicyResolver policyResolver;

    @Override
    public Response saveDefaultSettings()
    {
        return handle(null, true, "Failed to save the automatic purge defaults.", () -> {
            XWikiRequest request = this.xcontextProvider.get().getRequest();
            this.settingsStore.saveGlobalSettings(readTypeSettings(request),
                Integer.parseInt(request.getParameter(MAX_HISTORY_ENTRIES)));
            return Response.noContent().build();
        });
    }

    @Override
    public Response saveWikiSettings(String wikiId)
    {
        return handle(wikiId, true, String.format("Failed to save the automatic purge settings of wiki [%s].", wikiId),
            () -> {
                this.settingsStore.saveWikiSettings(wikiId, readTypeSettings(this.xcontextProvider.get().getRequest()));
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
        return handle(wikiId, true, String.format("Failed to start the purge of the recycle bin of wiki [%s].", wikiId),
            () -> {
                XWikiContext context = this.xcontextProvider.get();
                XWikiRequest request = context.getRequest();
                String[] typeIds = request.getParameterValues(TYPE);
                if (typeIds == null || typeIds.length == 0) {
                    throw new IllegalArgumentException("No item type to purge.");
                }
                // Check every item type and its limit before starting any job, and purge each one once.
                Map<PurgeType, PurgeTypeSettings> limits = new EnumMap<>(PurgeType.class);
                for (String typeId : typeIds) {
                    PurgeType type = PurgeType.fromId(typeId)
                        .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, typeId, TYPE)));
                    PurgeTypeSettings limit = readLimit(request, type).orElseThrow(() -> new IllegalArgumentException(
                        String.format("The limit of [%s] is missing.", typeId)));
                    this.purgeManager.checkLimit(limit.getInterval().orElseThrow(), limit.getUnit().orElseThrow());
                    limits.put(type, limit);
                }
                // A type that can't be started doesn't prevent the others, and the started jobs are still returned.
                List<List<String>> jobIds = new ArrayList<>();
                for (Map.Entry<PurgeType, PurgeTypeSettings> limit : limits.entrySet()) {
                    PurgeType type = limit.getKey();
                    try {
                        if (this.purgeManager.startManualPurge(wikiId, type,
                            limit.getValue().getInterval().orElseThrow(), limit.getValue().getUnit().orElseThrow(),
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
            String.format("Failed to count the expired items of the recycle bin of wiki [%s].", wikiId), () -> {
                XWikiRequest request = this.xcontextProvider.get().getRequest();
                Map<String, Map<String, Object>> preview = new LinkedHashMap<>();
                for (PurgeType type : PurgeType.values()) {
                    Optional<PurgeTypeSettings> settings = this.policyResolver.getSettings(wikiId, type);
                    // Without a stored interval and unit, the item type is counted only for a requested limit.
                    Optional<PurgeTypeSettings> limit = readLimit(request, type).or(() -> settings.filter(
                        typeSettings -> typeSettings.getInterval().isPresent() && typeSettings.getUnit().isPresent()));
                    Map<String, Object> typePreview = new LinkedHashMap<>();
                    typePreview.put("enabled",
                        settings.filter(typeSettings -> typeSettings.getMode() == PurgeMode.ENABLED).isPresent());
                    if (limit.isPresent()) {
                        PurgeTypeSettings typeLimit = limit.get();
                        typePreview.put("interval", typeLimit.getInterval().orElseThrow());
                        typePreview.put("unit", typeLimit.getUnit().orElseThrow().getId());
                        // The purge manager rejects an invalid limit.
                        typePreview.put("count", this.purgeManager.countExpired(wikiId, type,
                            typeLimit.getInterval().orElseThrow(), typeLimit.getUnit().orElseThrow()));
                    }
                    preview.put(type.getId(), typePreview);
                }
                return Response.ok(OBJECT_MAPPER.writeValueAsString(preview)).type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
            });
    }

    /**
     * Run an automatic purge request, once {@link #checkRequest(String, boolean)} accepts it. A missing or invalid
     * parameter is reported with status code 400. Missing rights are reported with status code 401 to a guest, who
     * might get them by logging in, and with 403 to a logged-in user. The responses of failed requests have no content,
     * so that they expose nothing.
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
            // A guest has no user reference: 401 tells them to log in, while 403 tells a logged-in user that logging in
            // again won't help.
            return Response.status(this.xcontextProvider.get().getUserReference() == null
                ? Response.Status.UNAUTHORIZED : Response.Status.FORBIDDEN).build();
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

    /**
     * Read the automatic purge settings of every item type from the form fields {@code <typeId>Mode},
     * {@code <typeId>Interval} and {@code <typeId>Unit}. The mode is required; the interval and unit can be left out
     * for an item type that is not enabled, which keeps its stored ones. The settings store rejects an enabled item
     * type without them.
     *
     * @param request the request holding the form fields
     * @return the settings of every item type
     * @throws IllegalArgumentException if a mode is missing, or if a field has an invalid value
     */
    private Map<PurgeType, PurgeTypeSettings> readTypeSettings(XWikiRequest request)
    {
        Map<PurgeType, PurgeTypeSettings> typeSettings = new EnumMap<>(PurgeType.class);
        for (PurgeType type : PurgeType.values()) {
            String mode = type.getId() + "Mode";
            String modeValue = request.getParameter(mode);
            typeSettings.put(type, new PurgeTypeSettings(PurgeMode.fromId(modeValue)
                .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, modeValue, mode))),
                readInterval(request, type).orElse(null), readUnit(request, type).orElse(null)));
        }
        return typeSettings;
    }

    /**
     * Read the limit of a manual purge of an item type from the fields {@code <typeId>Interval} and
     * {@code <typeId>Unit}, which must be both set or both missing.
     *
     * @param request the request holding the fields
     * @param type the item type
     * @return the limit, as enabled settings, or an empty value if both fields are missing
     * @throws IllegalArgumentException if only one of the fields is set, or if a field has an invalid value
     */
    private Optional<PurgeTypeSettings> readLimit(XWikiRequest request, PurgeType type)
    {
        Optional<Integer> interval = readInterval(request, type);
        Optional<IntervalUnit> unit = readUnit(request, type);
        if (interval.isEmpty() && unit.isEmpty()) {
            return Optional.empty();
        }
        if (interval.isEmpty() || unit.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("The limit of [%s] needs both an interval and a unit.", type.getId()));
        }
        return Optional.of(new PurgeTypeSettings(PurgeMode.ENABLED, interval.get(), unit.get()));
    }

    /**
     * @param request the request holding the fields
     * @param type the item type
     * @return the value of the field {@code <typeId>Interval}, or an empty value if it is missing
     * @throws IllegalArgumentException if the value is not a number
     */
    private Optional<Integer> readInterval(XWikiRequest request, PurgeType type)
    {
        String value = request.getParameter(type.getId() + INTERVAL_SUFFIX);
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(Integer.parseInt(value));
    }

    /**
     * @param request the request holding the fields
     * @param type the item type
     * @return the value of the field {@code <typeId>Unit}, or an empty value if it is missing
     * @throws IllegalArgumentException if the unit is unknown
     */
    private Optional<IntervalUnit> readUnit(XWikiRequest request, PurgeType type)
    {
        String field = type.getId() + UNIT_SUFFIX;
        String value = request.getParameter(field);
        return StringUtils.isBlank(value) ? Optional.empty() : Optional.of(IntervalUnit.fromId(value)
            .orElseThrow(() -> new IllegalArgumentException(String.format(INVALID_VALUE, value, field))));
    }
}
