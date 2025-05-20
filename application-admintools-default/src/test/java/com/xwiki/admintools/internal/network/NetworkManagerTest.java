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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.limits.LimitsConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class NetworkManagerTest
{
    @InjectMockComponents
    private NetworkManager networkManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private XWikiContext xwikiContext;

    @MockComponent
    private XWikiRequest wikiRequest;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private LimitsConfiguration limitsConfiguration;

    @Mock
    private HttpSession httpSession;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private HttpClient httpClient;

    @MockComponent
    private HttpClientBuilderFactory httpClientBuilderFactory;

    @BeforeComponent
    void setUp() throws ParseException
    {
        when(contextProvider.get()).thenReturn(xwikiContext);
        when(xwikiContext.getRequest()).thenReturn(wikiRequest);
        StringBuffer stringBuffer = new StringBuffer("https://test-staging.devxwiki.com/some/xwiki/page/");
        when(wikiRequest.getRequestURL()).thenReturn(stringBuffer);
        when(wikiRequest.getRequestURI()).thenReturn("/some/xwiki/page/");
        Map<String, Object> limits = Map.of("instanceReference", "instance.reference", "expirationDate",
            new SimpleDateFormat("yyyy-MM-dd").parse("2024-04-16"));
        when(limitsConfiguration.getCustomLimits()).thenReturn(limits);
    }

    @BeforeEach
    void beforeEach()
    {
        when(httpClientBuilderFactory.getHttpClient()).thenReturn(httpClient);
    }

    @Test
    void initialize()
    {
        verify(limitsConfiguration).getCustomLimits();
    }

    @Test
    void testGetJSONFromNetwork() throws IOException, InterruptedException
    {
        when(httpClientBuilderFactory.getHttpClient()).thenReturn(httpClient);
        when(wikiRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getAttribute("xnngSessionId")).thenReturn("mockedSessionId");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"key\":\"value\"}");

        Map<String, Object> result = networkManager.getJSONFromNetwork("target", Map.of("param", "value"));

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }

    @Test
    void testGetJSONFromNetworkNoStoredCookieError() throws IOException, InterruptedException
    {
        when(httpClientBuilderFactory.getHttpClient()).thenReturn(httpClient);
        when(wikiRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getAttribute("xnngSessionId")).thenReturn(null);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(404);

        Map<String, Object> result = networkManager.getJSONFromNetwork("target", Map.of("param", "value"));

        assertNull(result);
    }

    @Test
    void testGetLimits() throws IOException, InterruptedException
    {
        when(httpClientBuilderFactory.getHttpClient()).thenReturn(httpClient);
        when(wikiRequest.getSession()).thenReturn(httpSession);
        when(httpSession.getAttribute("xnngSessionId")).thenReturn(null);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"key\":\"value\"}");

        Map<String, Object> result = networkManager.getLimits();

        assertNotNull(result);
        assertEquals("value", result.get("key"));
    }
}
