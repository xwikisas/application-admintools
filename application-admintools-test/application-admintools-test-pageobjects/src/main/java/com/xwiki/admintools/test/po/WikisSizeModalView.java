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
import org.xwiki.test.ui.po.BaseModal;

/**
 * Represents actions that can be done on the wikis size view modal.
 *
 * @version $Id$
 */
public class WikisSizeModalView extends BaseModal
{
    @FindBy(css = "div#viewWikisSizeModal")
    public WebElement content;

    public WikisSizeModalView(By selector)
    {
        super(selector);
    }

    public void clickCancelButton()
    {
        content.findElement(By.cssSelector("div.modal-footer > .btn-default")).click();
    }

    public String getUserCount()
    {
        return content.findElement(By.cssSelector("tr:nth-child(1) > td:nth-child(2)")).getText();
    }

    public List<WebElement> getTableRows()
    {
        return content.findElements(By.cssSelector("table > tbody > tr"));
    }
}