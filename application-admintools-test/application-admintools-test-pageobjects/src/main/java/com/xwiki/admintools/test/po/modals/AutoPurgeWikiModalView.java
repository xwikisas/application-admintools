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

/**
 * Represents the modal editing the automatic purge settings of a wiki.
 *
 * @version $Id$
 */
public class AutoPurgeWikiModalView extends AbstractAutoPurgeModalView
{
    /**
     * Create the modal before opening it.
     */
    public AutoPurgeWikiModalView()
    {
        super("purgeWikiModal");
    }

    /**
     * Select the mode of an item type.
     *
     * @param type the identifier of the item type
     * @param mode the identifier of the mode: {@code inherit}, {@code enabled} or {@code disabled}
     */
    public void selectMode(String type, String mode)
    {
        getTypeContainer(type).findElement(By.cssSelector(String.format(".purge-mode[value='%s']", mode))).click();
    }

    /**
     * @param type the identifier of an item type
     * @return the identifier of the selected mode of the item type
     */
    public String getSelectedMode(String type)
    {
        return getTypeContainer(type).findElement(By.cssSelector(".purge-mode:checked")).getAttribute("value");
    }

    /**
     * @param type the identifier of an item type
     * @return the label of the default settings of the item type, e.g. "1 month" or "disabled"
     */
    public String getDefaultLabel(String type)
    {
        return getTypeContainer(type).findElement(By.className("purge-default-label")).getText();
    }

    /**
     * Click the button resetting the settings of the wiki to the defaults.
     */
    public void resetToDefaults()
    {
        this.container.findElement(By.className("purge-reset")).click();
    }
}
