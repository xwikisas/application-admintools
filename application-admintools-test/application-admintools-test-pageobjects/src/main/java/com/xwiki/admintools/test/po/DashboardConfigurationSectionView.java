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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the configuration section from within the AdminTools.WebHome page dashboard.
 *
 * @version $Id$
 */
public class DashboardConfigurationSectionView extends ViewPage
{
    private static final String BACKEND_SECTION_VIEW_LAST_LOGS_MODAL_ID = "#configurationViewLastNLinesModal";

    @FindBy(css = "a[data-target='#configurationViewLastNLinesModal']")
    public WebElement backendLogsHyperlink;

    @FindBy(css = ".backend-section")
    public WebElement backendContent;

    public String getText()
    {
        return backendContent.findElement(By.tagName("ul")).getText();
    }

    public List<WebElement> getErrorMessages()
    {
        return backendContent.findElements(By.className("warningmessage"));
    }

    /**
     * Click the hyperlink inside the configuration section to open the view for last n lines of log modal.
     */
    public LastNLinesModalView clickViewLastLogsModal()
    {
        backendLogsHyperlink.click();
        return new LastNLinesModalView(By.id("configurationViewLastNLinesModal"),
            BACKEND_SECTION_VIEW_LAST_LOGS_MODAL_ID);
    }

    /**
     * Get the {@link WebElement} representing the error that a non admin user should see.
     */
    public WebElement getNonAdminUserView()
    {
        return this.getDriver().findElement(By.cssSelector(".xwikirenderingerror"));
    }
}
