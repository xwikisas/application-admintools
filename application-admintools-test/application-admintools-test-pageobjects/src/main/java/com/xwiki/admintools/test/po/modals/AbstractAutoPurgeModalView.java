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
package com.xwiki.admintools.test.po.modals;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.BaseModal;

/**
 * The base of the modals of the automatic purge page: they open with the JavaScript, and validate their number fields
 * with translated messages.
 *
 * @version $Id$
 */
public abstract class AbstractAutoPurgeModalView extends BaseModal
{
    /**
     * Create the modal before opening it, so that it opens without the fade effect.
     *
     * @param id the identifier of the modal
     */
    protected AbstractAutoPurgeModalView(String id)
    {
        super(By.id(id));
    }

    /**
     * Wait until the modal is displayed.
     */
    public void waitUntilDisplayed()
    {
        getDriver().waitUntilCondition(driver -> isDisplayed());
    }

    /**
     * Wait until the modal is closed, for example after a successful save.
     */
    public void waitUntilClosed()
    {
        waitForClosed();
    }

    /**
     * @return the error messages shown under the invalid fields
     */
    public List<String> getErrorMessages()
    {
        return this.container.findElements(By.cssSelector(".purge-error")).stream().map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Wait until error messages are shown, for example for the invalid stored settings checked when the modal opens.
     */
    public void waitUntilErrorMessagesShown()
    {
        getDriver().waitUntilCondition(driver -> !getErrorMessages().isEmpty());
    }

    /**
     * @return the name of the wiki shown by the modal
     */
    public String getWikiName()
    {
        return this.container.findElement(By.className("purge-wiki-name")).getText();
    }

    /**
     * Set the interval of an item type.
     *
     * @param type the identifier of the item type
     * @param interval the interval, typed as is
     * @param unit the identifier of the unit
     */
    public void setInterval(String type, String interval, String unit)
    {
        WebElement intervalInput = getTypeContainer(type).findElement(By.className("purge-interval"));
        intervalInput.clear();
        intervalInput.sendKeys(interval);
        new Select(getTypeContainer(type).findElement(By.className("purge-unit"))).selectByValue(unit);
    }

    /**
     * @param type the identifier of an item type
     * @return the interval of the item type, as typed
     */
    public String getInterval(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-interval")).getAttribute("value");
    }

    /**
     * @param type the identifier of an item type
     * @return the identifier of the selected unit of the item type
     */
    public String getUnit(String type)
    {
        return new Select(getTypeContainer(type).findElement(By.className("purge-unit"))).getFirstSelectedOption()
            .getAttribute("value");
    }

    /**
     * Click the submit button of the modal.
     */
    public void submit()
    {
        this.container.findElement(By.cssSelector("button[type='submit']")).click();
    }

    /**
     * @param type the identifier of an item type
     * @return the element holding the fields of the item type
     */
    protected WebElement getTypeContainer(String type)
    {
        return this.container.findElement(By.cssSelector(String.format(".purge-type[data-type='%s']", type)));
    }
}
