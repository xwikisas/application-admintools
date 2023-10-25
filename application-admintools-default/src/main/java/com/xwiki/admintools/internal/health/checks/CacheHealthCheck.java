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
package com.xwiki.admintools.internal.health.checks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xwiki.admintools.health.HealthCheck;
import com.xwiki.admintools.health.HealthCheckResult;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import groovy.jmx.GroovyMBean;

@Component
@Named(CacheHealthCheck.HINT)
@Singleton
public class CacheHealthCheck implements HealthCheck
{
    public final static String HINT = "CACHE_HEALTH_CHECK";

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Inject
    CacheManagerConfiguration cacheManagerConfiguration;

    @Inject
    private Logger logger;
    @Override
    public HealthCheckResult check()
    {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName query = new ObjectName("org.xwiki.infinispan:component=Statistics,*");
            Set<ObjectName> names = server.queryNames(query, null);
            for (ObjectName objectName : names){
                GroovyMBean mbean = new GroovyMBean(server, objectName, true);
                for(String attribute : mbean.listAttributeNames()) {
                    logger.warn(String.format("%s - %s", attribute, mbean.getProperty(attribute)));
                    String b = "";
                }
            }
            logger.error("dummy err");
            return new HealthCheckResult();
        } catch (Exception ignored){}
        return new HealthCheckResult();
    }
}
