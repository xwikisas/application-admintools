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

public class DownloadArchiveModalView extends ViewPage
{
    @FindBy(xpath = "//div[@id = 'downloadFilesModal']")
    public WebElement content;

    public String getModalText()
    {
        return content.getText();
    }

    public WebElement getXWikiConfigCheck()
    {
        return content.findElement(By.cssSelector("input[value='xwikiConfig']"));
    }

    public WebElement getXWikiPropertiesCheck()
    {
        return content.findElement(By.cssSelector("input[value='xwikiProperties']"));
    }

    public WebElement getProviderCheck()
    {
        return content.findElement(By.cssSelector("input[value='dataProvider']"));
    }

    public WebElement getLogsCheck()
    {
        return content.findElement(By.cssSelector("input[value='logs']"));
    }

    public WebElement getDateFilters()
    {
        return content.findElement(By.className("dateFields"));
    }

    public WebElement getViewButton(String id)
    {
        return content.findElement(By.cssSelector(id + " .btn-primary"));
    }

    public WebElement getCancelButton(String id)
    {
        return content.findElement(By.cssSelector(id + " .btn-default"));
    }
}
