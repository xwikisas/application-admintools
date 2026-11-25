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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the modal purging the recycle bin of a wiki now.
 *
 * @version $Id$
 */
public class AutoPurgeNowModalView extends AbstractAutoPurgeModalView
{
    /**
     * Create the modal before opening it.
     */
    public AutoPurgeNowModalView()
    {
        super("purgeNowModal");
    }

    /**
     * Wait until the items of an item type older than its limit are counted.
     *
     * @param type the identifier of the item type
     * @param count the expected text of the count, e.g. "(1 item)"
     */
    public void waitUntilCount(String type, String count)
    {
        getDriver().waitUntilCondition(driver -> count.equals(getCount(type)));
    }

    /**
     * @param type the identifier of an item type
     * @return the text of the count of the item type, e.g. "(1 item)" or "(nothing older)"
     */
    public String getCount(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-count")).getText();
    }

    /**
     * @param type the identifier of an item type
     * @return {@code true} if the limit of the item type can be changed, because the item type is selected
     */
    public boolean isIntervalEnabled(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-interval")).isEnabled();
    }

    /**
     * Select an item type to purge.
     *
     * @param type the identifier of the item type
     */
    public void select(String type)
    {
        WebElement checkbox = getTypeCheckbox(type);
        if (!checkbox.isSelected()) {
            checkbox.click();
        }
    }

    /**
     * @param type the identifier of an item type
     * @return {@code true} if the note saying that the automatic purge of the item type is disabled is shown
     */
    public boolean isAutoPurgeDisabledNoteDisplayed(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-disabled-note")).isDisplayed();
    }

    /**
     * Start purging the selected item types. The modal closes and the purge history of the wiki opens.
     *
     * @return the history modal
     */
    public AutoPurgeHistoryModalView purge()
    {
        // Created before it opens, so that it opens without the fade effect.
        AutoPurgeHistoryModalView historyModal = new AutoPurgeHistoryModalView();
        submit();
        waitForClosed();
        historyModal.waitUntilDisplayed();
        return historyModal;
    }

    /**
     * @return {@code true} if the purge button can be clicked
     */
    public boolean isPurgeEnabled()
    {
        return this.container.findElement(By.cssSelector("button[type='submit']")).isEnabled();
    }

    /**
     * @param type the identifier of an item type
     * @return the check box selecting the item type
     */
    private WebElement getTypeCheckbox(String type)
    {
        return getTypeContainer(type).findElement(By.name("type"));
    }
}
