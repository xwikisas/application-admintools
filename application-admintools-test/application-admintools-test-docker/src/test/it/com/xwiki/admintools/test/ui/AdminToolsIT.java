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
package com.xwiki.admintools.test.ui;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.admintools.test.po.AdminToolsHomePage;
import com.xwiki.admintools.test.po.AdminToolsViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UITest
class AdminToolsIT
{
    private final List<String> supportedServers = List.of("TOMCAT");

    @BeforeAll
    static void createUsers(TestUtils setup)
    {
        setup.createUser("JonSnow", "pass", setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name", "Snow");
    }

    @BeforeEach
    void setUp(TestUtils setup)
    {
        setup.login("JonSnow", "pass");
    }

    @Test
    @Order(1)
    void AdminToolsHomePage(TestUtils testUtils, TestConfiguration testConfiguration)
    {
        AdminToolsHomePage page = AdminToolsHomePage.gotoPage();
        testUtils.gotoPage(page.getPageURL());
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        String serverType = testConfiguration.getServletEngine().name();
        System.out.println(serverType);
        if (!supportedServers.contains(serverType)) {
            assertEquals(0, webHomePage.getDashboardElements().size());
        } else {
            assertEquals(2, webHomePage.getDashboardElements().size());
        }
    }
}