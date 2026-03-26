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
package com.xwiki.admintools.test.po.sections;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the security section from within the AdminTools application.
 *
 * @version $Id$
 */
public class DashboardSecuritySectionView extends ViewPage
{
    @FindBy(css = ".wiki-security-settings")
    private WebElement healthContent;

    /**
     * Get the content of the security section dashboard.
     */
    public WebElement getSecuritySectionContent()
    {
        return healthContent;
    }

    public void clickGivenGroupsRights()
    {
        healthContent.findElement(By.cssSelector(".security-detailed > ul > li:nth-child(1) > a")).click();
    }

    public void clickGivenUsersRights()
    {
        healthContent.findElement(By.cssSelector(".security-detailed > ul > li:nth-child(2) > a")).click();
    }

    public void clickUsersRightsOnPage()
    {
        healthContent.findElement(By.cssSelector(".security-detailed > ul > li:nth-child(3) > a")).click();
    }

    public void clickChangeUserFields()
    {
        healthContent.findElement(By.cssSelector(".security-detailed > ul > li:nth-child(4) > a")).click();
    }

    public void clickCheckSecurityCache()
    {
        healthContent.findElement(By.cssSelector(".security-detailed > ul > li:nth-child(5) > a")).click();
    }
}
