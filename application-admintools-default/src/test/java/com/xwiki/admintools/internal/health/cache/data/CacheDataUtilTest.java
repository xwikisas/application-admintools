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
package com.xwiki.admintools.internal.health.cache.data;

import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xwiki.admintools.internal.health.cache.GroovyMBeanUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CacheDataUtil}
 *
 * @version $Id$
 */
@ComponentTest
class CacheDataUtilTest
{
    @InjectMockComponents
    private CacheDataUtil cacheDataUtil;

    @MockComponent
    private GroovyMBeanUtil mBeanUtil;

    @Mock
    private MBeanServer mBeanServer;

    @Mock
    private ObjectName objectName;

    @Test
    void getCacheSet() throws MalformedObjectNameException
    {
        when(mBeanUtil.getMBeanServer()).thenReturn(mBeanServer);
        Set<ObjectName> objectNameSet = Set.of(objectName);

        when(mBeanServer.queryNames(any(ObjectName.class), eq(null))).thenReturn(objectNameSet);

        assertEquals(objectNameSet, cacheDataUtil.getCacheSet("test"));
    }
}
