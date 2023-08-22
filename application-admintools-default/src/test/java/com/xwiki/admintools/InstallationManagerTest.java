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
//package com.xwiki.admintools;
//
//import java.io.IOException;
//
//import com.xwiki.admintools.internal.util.SecurityInfo;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.xwiki.test.annotation.AllComponents;
//import org.xwiki.test.junit5.mockito.ComponentTest;
//
//import static org.junit.Assert.assertEquals;
//import static org.mockito.Mockito.*;
//
//@ComponentTest
//@AllComponents
//public class InstallationManagerTest {
//
//    private SecurityInfo securityInfo;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//        securityInfo = new SecurityInfo();
//    }
//
//    @Test
//    public void testGenerateResults() throws IOException
//    {
//        String result = String.valueOf(securityInfo.getEnvVars());
//        assertEquals("mockedPath", result);
//    }
//}
