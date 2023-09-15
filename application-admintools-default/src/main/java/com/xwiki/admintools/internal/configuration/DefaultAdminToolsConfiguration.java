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
package com.xwiki.admintools.internal.configuration;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.stability.Unstable;

import com.xwiki.admintools.configuration.AdminToolsConfiguration;

/**
 * Default implementation of {@link AdminToolsConfiguration}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Unstable
public class DefaultAdminToolsConfiguration implements AdminToolsConfiguration
{
    private static final String SERVER_LOCATION = "serverLocation";

    @Inject
    @Named(AdminToolsConfigurationSource.HINT)
    private ConfigurationSource mainConfiguration;

    @Override
    public String getServerPath()
    {
        return this.getProperty(SERVER_LOCATION, "");
    }

    private <T> T getProperty(String key, T defaultValue)
    {
        T value = this.mainConfiguration.getProperty(key, defaultValue);
        if (value == null || value.equals(defaultValue)) {
            throw new RuntimeException(MessageFormat.format("The {0} is missing.", key));
        }
        return value;
    }
}
