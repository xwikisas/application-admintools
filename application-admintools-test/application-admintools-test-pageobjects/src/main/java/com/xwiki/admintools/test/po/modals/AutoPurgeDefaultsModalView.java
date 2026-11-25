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
 * Represents the modal editing the default settings of the automatic purge.
 *
 * @version $Id$
 */
public class AutoPurgeDefaultsModalView extends AbstractAutoPurgeModalView
{
    /**
     * Create the modal before opening it.
     */
    public AutoPurgeDefaultsModalView()
    {
        super("purgeDefaultsModal");
    }

    /**
     * Enable or disable the automatic purge of an item type.
     *
     * @param type the identifier of the item type
     * @param enabled {@code true} to enable it
     */
    public void setEnabled(String type, boolean enabled)
    {
        WebElement checkbox = getTypeContainer(type).findElement(By.className("purge-enable"));
        if (checkbox.isSelected() != enabled) {
            checkbox.click();
        }
    }

    /**
     * @param type the identifier of an item type
     * @return {@code true} if the automatic purge of the item type is ticked
     */
    public boolean isEnabled(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-enable")).isSelected();
    }

    /**
     * @param type the identifier of an item type
     * @return {@code true} if the interval fields of the item type can be edited
     */
    public boolean isIntervalEnabled(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-interval")).isEnabled();
    }

    /**
     * Set the number of purges kept in the history.
     *
     * @param size the history size, typed as is
     */
    public void setHistorySize(String size)
    {
        WebElement input = this.container.findElement(By.name("maxHistoryEntries"));
        input.clear();
        input.sendKeys(size);
    }
}
