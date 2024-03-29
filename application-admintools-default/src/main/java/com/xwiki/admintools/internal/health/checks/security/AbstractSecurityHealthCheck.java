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
package com.xwiki.admintools.internal.health.checks.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;

import com.xwiki.admintools.DataProvider;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.internal.data.SecurityDataProvider;

/**
 * Base class for security related health checks.
 *
 * @version $Id$
 */
public abstract class AbstractSecurityHealthCheck implements HealthCheck
{
    protected final List<String> acceptedEncodings = new ArrayList<>(List.of("UTF8", "UTF-8", "utf8", "utf-8"));

    @Inject
    protected Logger logger;

    @Inject
    @Named(SecurityDataProvider.HINT)
    private DataProvider securityDataProvider;

    protected boolean isSafeEncoding(String encoding, String type)
    {
        if (acceptedEncodings.contains(encoding)) {
            return true;
        }
        logger.warn("[{}] encoding is [{}], but should be UTF-8!", type, encoding);
        return false;
    }

    protected Map<String, String> getSecurityProviderJSON()
    {
        try {
            return securityDataProvider.getDataAsJSON();
        } catch (Exception e) {
            logger.warn("Failed to generate the instance security data. Root cause is: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return new HashMap<>();
        }
    }
}
