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
package com.xwiki.admintools.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the AdminTools.WebHome page.
 *
 * @version $Id$
 */
public class AdminToolsHomePage extends ViewPage
{
    private static final String ADMIN_TOOLS_SPACE = "AdminTools";

    private static final String ADMIN_TOOLS_PAGE = "WebHome";

    /**
     * Opens the home page.
     */
    public static AdminToolsHomePage gotoPage()
    {
        getUtil().gotoPage(ADMIN_TOOLS_SPACE, ADMIN_TOOLS_PAGE);
        return new AdminToolsHomePage();
    }

    /**
     * Open the dashboard configuration section.
     */
    public static DashboardConfigurationSectionView getConfigurationSection()
    {
        gotoPage();
        return new DashboardConfigurationSectionView();
    }

    /**
     * Open the dashboard files section.
     */
    public static DashboardFilesSectionView getFilesSection()
    {
        gotoPage();
        return new DashboardFilesSectionView();
    }

    public static DashboardHealthSectionView getHealthSection()
    {
        gotoPage();
        return new DashboardHealthSectionView();
    }

    public static DashboardUsageSectionView getInstanceUsageSection()
    {
        gotoPage();
        return new DashboardUsageSectionView();
    }

    /**
     * Check if the page is the same as Admin Tools WebHome.
     */
    public static boolean isCurrentPage(ViewPage vp)
    {
        return vp.getMetaDataValue("page").equals(ADMIN_TOOLS_PAGE) && vp.getMetaDataValue("space")
            .equals(ADMIN_TOOLS_SPACE);
    }

    /**
     * Get the view for a user that does not have admin rights.
     */
    public WebElement getNonAdminUserView()
    {
        return this.getDriver().findElement(By.cssSelector(".errormessage"));
    }
}

