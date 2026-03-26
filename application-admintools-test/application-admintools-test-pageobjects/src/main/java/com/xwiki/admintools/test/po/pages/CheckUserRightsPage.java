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
package com.xwiki.admintools.test.po.pages;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the AdminTools.CheckUserRightsOnPage page.
 *
 * @version $Id$
 */
public class CheckUserRightsPage extends ViewPage
{
    @FindBy(id = "targetWiki-selectized")
    private WebElement wikiSelector;

    @FindBy(id = "rightsTargetPage-selectized")
    private WebElement targetPage;

    @FindBy(id = "rightsTargetUser-selectized")
    private WebElement targetUser;

    @FindBy(css = "#xwikicontent > form > input")
    private WebElement button;

    @FindBy(css = "#xwikicontent > table")
    private WebElement table;

    public void populateWikiSelector(String wiki)
    {
        setSelectizeValue(wikiSelector, wiki);
    }

    public void populateTargetPage(String targetPageRef)
    {
        setSelectizeValue(targetPage, targetPageRef);
    }

    public void populateTargetUser(String targetUserRef)
    {
        setSelectizeValue(targetUser, targetUserRef);
    }

    public WebElement getTable()
    {
        return table;
    }

    public void clickCheckButton()
    {
        button.click();
    }

    private void setSelectizeValue(WebElement element, String value)
    {
        element.click();
        element.sendKeys(value);
        element.sendKeys(Keys.ENTER);
        element.sendKeys(Keys.TAB);
    }
}
