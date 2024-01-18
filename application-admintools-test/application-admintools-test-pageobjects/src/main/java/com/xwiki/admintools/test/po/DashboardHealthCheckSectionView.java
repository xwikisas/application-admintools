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
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the health check section from within the AdminTools.WebHome page dashboard.
 *
 * @version $Id$
 */
public class DashboardHealthCheckSectionView extends ViewPage
{
    @FindBy(css = "#healthCheck")
    private WebElement healthCheckContent;

    @FindBy(css = "#healthCheck > p > a")
    private WebElement helpLinksPageHyperlink;

    public void clickHelpLinksHyperlink()
    {
        helpLinksPageHyperlink.click();
    }

    /**
     * Start the health check job.
     */
    public void clickHealthCheckJobStartButton()
    {
        healthCheckContent.findElement(By.id("healthCheckJobStart")).click();
    }

    /**
     * Get the {@link WebElement} representing the time since the last health check.
     */
    public WebElement getHealthCheckTimeElapsed()
    {
        return healthCheckContent.findElement(By.cssSelector("div.health-check-wrapper > p"));
    }

    /**
     * Get the {@link WebElement} representing the result of the health check.
     */
    public WebElement getHealthCheckResult()
    {
        return healthCheckContent.findElement(By.className("health-check-result-message"));
    }

    /**
     * Get the {@link WebElement} representing the progress of the health check.
     */
    public WebElement getJobProgress()
    {
        return healthCheckContent.findElement(By.cssSelector(".ui-progress"));
    }

    /**
     * Get the {@link WebElement} representing the error that a non admin user should see.
     */
    public WebElement getNonAdminUserView()
    {
        return this.getDriver().findElement(By.cssSelector(".xwikirenderingerror"));
    }

    /**
     * Wait until the health check job is done by checking if the progress bar disappeared.
     */
    public void waitUntilJobIsDone()
    {
        this.getDriver().waitUntilElementDisappears(healthCheckContent, By.cssSelector(".ui-progress"));
    }

    /**
     * Click a toggle to view or hide the result logs.
     */
    public void toggleLog()
    {
        healthCheckContent.findElement(By.cssSelector(".collapse-toggle")).click();
    }

    /**
     * Get the result logs.
     */
    public WebElement getResultLog()
    {
        return healthCheckContent.findElement(By.cssSelector(".log"));
    }

    /**
     * Get the number of logs stored inside the health check job.
     */
    public int getNumberOfLogs()
    {
        return healthCheckContent.findElements(By.cssSelector(".log-item")).size();
    }
}
