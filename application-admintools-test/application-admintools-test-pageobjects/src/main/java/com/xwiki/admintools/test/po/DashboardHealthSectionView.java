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
 * Represents actions that can be done on the health section from within the AdminTools application.
 *
 * @version $Id$
 */
public class DashboardHealthSectionView extends ViewPage
{
    @FindBy(css = "#healthCheck")
    private WebElement healthContent;

    @FindBy(css = "a[href='#confirmCacheFlushModal']")
    private WebElement flushCacheModalHyperlink;

    /**
     * Get the health check job start button.
     */
    public WebElement getHealthJobStartButton()
    {
        return healthContent.findElement(By.id("healthCheckJobStart"));
    }

    /**
     * Get the health check job result.
     */
    public WebElement getResult()
    {
        return healthContent.findElement(By.className("health-check-result-message"));
    }

    /**
     * Toggle the results logs.
     */
    public void clickResultsToggle()
    {
        healthContent.findElement(By.className("collapse-toggle")).click();
    }

    /**
     * Get the health check job logs.
     */
    public WebElement getLogs()
    {
        return healthContent.findElement(By.className("log"));
    }

    /**
     * Open the flush cache modal.
     */
    public FlushCacheModalView clickFlushCacheHyperlink()
    {
        flushCacheModalHyperlink.click();
        return new FlushCacheModalView(By.id("confirmCacheFlushModal"));
    }
}
