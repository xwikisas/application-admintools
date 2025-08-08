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
package com.xwiki.admintools.internal.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriBuilder;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.limits.LimitsConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Manages the communication between the application and the XWiki networking server.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = NetworkManager.class)
@Singleton
public class NetworkManager implements Initializable
{
    private static final String COOKIE_ID = "xnngSessionId";

    private static final String COOKIE_KEY = "Cookie";

    private static final String INSTANCE_KEY = "instance";

    @Inject
    private Provider<XWikiContext> wikiContextProvider;

    @Inject
    private LimitsConfiguration limitsConfiguration;

    private String requestDomain;

    private String instanceReference = "";

    private String account;

    private String accountRef;

    private String instance;

    private long detail;

    @Inject
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @Override
    public void initialize() throws InitializationException
    {
        XWikiRequest wikiRequest = wikiContextProvider.get().getRequest();
        String instanceDomain = wikiRequest.getRequestURL().toString().replace(wikiRequest.getRequestURI(), "");
        requestDomain = "xnng.xwiki.com";
        if (instanceDomain.endsWith("devxwiki.com")) {
            requestDomain = "xnng-staging.devxwiki.com";
        }
        Map<String, Object> customLimits = limitsConfiguration.getCustomLimits();
        if (!customLimits.isEmpty()) {
            instanceReference = customLimits.get("instanceReference").toString();
            detail = ((Date) customLimits.get("expirationDate")).getTime();
            getSpecificFields(instanceReference);
        }
    }

    /**
     * Retrieve JSON data from the given network endpoint.
     *
     * @param target the target endpoint.
     * @param parameters parameters to be sent with the request.
     * @param useRef if {@code true}, the account reference will be added to the received parameters. If
     *     {@code false}, the account and instance number will be added.
     * @return the JSON retrieved from the network, or null if the user has no access.
     * @throws IOException if an I/O error occurs when sending the request or receiving the response.
     * @throws InterruptedException if the operation is interrupted.
     */
    public Map<String, Object> getJSONFromNetwork(String target, Map<String, String> parameters, boolean useRef)
        throws IOException, InterruptedException
    {
        HttpClient client = httpClientBuilderFactory.getHttpClient();
        XWikiContext wikiContext = wikiContextProvider.get();
        boolean hasSession = wikiContext.getRequest().getSession().getAttribute(COOKIE_ID) != null;
        boolean hasAccess = hasSession ? checkAccess(client) : tryGetAccess(client);

        return hasAccess ? getJSON(target, parameters, client, useRef) : null;
    }

    /**
     * Get network limits for the current instance.
     *
     * @return A JSON with the instance limits.
     * @throws IOException if an I/O error occurs when sending the request or receiving the response.
     * @throws InterruptedException if the operation is interrupted.
     */
    public Map<String, Object> getLimits() throws IOException, InterruptedException
    {
        HttpClient client = httpClientBuilderFactory.getHttpClient();
        String targetPath = "xwiki/rest/instance/limits";
        URI uri = getURI(targetPath, Map.of(INSTANCE_KEY, instanceReference, "detail", String.valueOf(detail)));
        HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>()
            {
            });
        }
        return Collections.emptyMap();
    }

    /**
     * Extract the instance number, account number and account reference from the instance reference.
     */
    private void getSpecificFields(String instanceReference)
    {
        Pattern pattern = Pattern.compile("Account_\\d+|Instance_\\d+");
        Matcher matcher = pattern.matcher(instanceReference);

        while (matcher.find()) {
            String match = matcher.group();
            if (match.startsWith("Account_")) {
                account = match;
                accountRef = String.format("Accounts.%s.WebHome", match);
            } else if (match.startsWith("Instance_")) {
                instance = match;
            }
        }
    }

    private boolean checkAccess(HttpClient client) throws IOException, InterruptedException
    {
        XWikiContext wikiContext = wikiContextProvider.get();
        String targetURL =
            String.format("https://%s/xwiki/bin/view/%s", requestDomain, instanceReference.replace(".", "/"));
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(targetURL))
            .header(COOKIE_KEY, (String) wikiContext.getRequest().getSession().getAttribute(COOKIE_ID)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return tryGetAccess(client);
        }
        return true;
    }

    private Map<String, Object> getJSON(String target, Map<String, String> parameters, HttpClient client,
        boolean useRef) throws IOException, InterruptedException
    {
        Map<String, String> completeParameters = new HashMap<>(parameters);
        if (useRef) {
            completeParameters.put("accountReference", accountRef);
        } else {
            completeParameters.put("account", account);
            completeParameters.put(INSTANCE_KEY, instance);
        }
        XWikiContext wikiContext = wikiContextProvider.get();
        URI dataUri = getURI(target, completeParameters);
        HttpRequest dataRequest = HttpRequest.newBuilder().uri(dataUri)
            .header(COOKIE_KEY, (String) wikiContext.getRequest().getSession().getAttribute(COOKIE_ID)).GET().build();
        HttpResponse<String> response = client.send(dataRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>()
            {
            });
        }
        return Collections.emptyMap();
    }

    private URI getURI(String target, Map<String, String> parameters)
    {
        String uri = String.format("https://%s/%s", requestDomain, target);
        UriBuilder uriBuilder = UriBuilder.fromUri(uri);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty() && !entry.getValue().equals("-")) {
                uriBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return uriBuilder.build();
    }

    private boolean tryGetAccess(HttpClient client) throws IOException, InterruptedException
    {
        String targetURL =
            String.format("https://%s/xwiki/rest/user/instance/access?instance=%s&detail=%d", requestDomain,
                instanceReference, detail);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(targetURL)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            XWikiContext wikiContext = wikiContextProvider.get();
            List<String> setCookieHeaders = response.headers().map().get("set-cookie");
            String sessionCookie = "";
            if (setCookieHeaders != null) {
                for (String cookie : setCookieHeaders) {
                    if (cookie.startsWith("JSESSIONID=")) {
                        sessionCookie = cookie.split(";", 2)[0];
                        break;
                    }
                }
            }
            wikiContext.getRequest().getSession().setAttribute(COOKIE_ID, sessionCookie);
            return true;
        }
        return false;
    }
}
