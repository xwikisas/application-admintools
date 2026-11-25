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
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;

/**
 * Represents the modal listing the purge history of a wiki.
 *
 * @version $Id$
 */
public class AutoPurgeHistoryModalView extends AbstractAutoPurgeModalView
{
    /**
     * Create the modal before opening it.
     */
    public AutoPurgeHistoryModalView()
    {
        super("purgeHistoryModal");
    }

    /**
     * @return the table of the history, once loaded
     */
    public TableLayoutElement getHistoryTable()
    {
        TableLayoutElement table = new LiveDataElement("purgeHistory").getTableLayout();
        table.waitUntilReady();
        return table;
    }

    /**
     * Wait until the history shows no purge in progress: it refreshes itself while some are.
     */
    public void waitUntilNoPurgeInProgress()
    {
        getDriver().waitUntilCondition(
            driver -> this.container.findElements(By.cssSelector("#purgeHistory tbody .label-info")).isEmpty(), 60);
    }

    /**
     * Open the failures of an entry of the history.
     *
     * @param rowNumber the number of the row of the entry, starting at 1
     * @return the modal, once the failures are loaded
     */
    public AutoPurgeFailuresModalView openFailures(int rowNumber)
    {
        AutoPurgeFailuresModalView modal = new AutoPurgeFailuresModalView();
        getHistoryTable().findElementInRow(rowNumber, By.className("purge-failures")).click();
        modal.waitUntilLoaded();
        return modal;
    }
}
