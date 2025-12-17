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
package com.xwiki.admintools.internal.health.cache;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.inject.Singleton;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.xwiki.component.annotation.Component;

import groovy.jmx.GroovyMBean;

/**
 * Utility class used to access JMX MBeans.
 *
 * @version $Id$
 * @since 1.3
 */
@Component(roles = GroovyMBeanUtil.class)
@Singleton
public class GroovyMBeanUtil
{
    private MBeanServer mBeanServer;

    /**
     * Return {@link MBeanServer}.
     *
     * @return the platform MBean server
     */
    public MBeanServer getMBeanServer()
    {
        if (mBeanServer == null) {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return mBeanServer;
    }

    /**
     * Create a {@link GroovyMBean} wrapper for a given {@link ObjectName}.
     *
     * @param objectName the JMX object name identifying the MBean
     * @return a {@link GroovyMBean} instance for the given object name
     */
    public GroovyMBean getGroovyMBean(ObjectName objectName) throws JMException, IOException
    {
        return new GroovyMBean(getMBeanServer(), objectName);
    }
}
