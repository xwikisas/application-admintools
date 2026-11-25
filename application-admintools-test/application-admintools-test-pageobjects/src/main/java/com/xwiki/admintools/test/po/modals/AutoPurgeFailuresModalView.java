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

/**
 * Represents the modal listing the failures of the newest purge of an item type of a wiki.
 *
 * @version $Id$
 */
public class AutoPurgeFailuresModalView extends AbstractAutoPurgeModalView
{
    /**
     * Create the modal before opening it.
     */
    public AutoPurgeFailuresModalView()
    {
        super("purgeFailuresModal");
    }

    /**
     * Wait until the modal is displayed and its failures are loaded.
     */
    public void waitUntilLoaded()
    {
        waitUntilDisplayed();
        getDriver().waitUntilCondition(driver -> !getSubtitle().isEmpty());
    }

    /**
     * @return the subtitle, naming the wiki, the item type and the date of the purge
     */
    public String getSubtitle()
    {
        return this.container.findElement(By.className("purge-failures-subtitle")).getText();
    }

    /**
     * @return the summary of the failures, saying how many are shown
     */
    public String getSummary()
    {
        return this.container.findElement(By.className("purge-failures-summary")).getText();
    }

    /**
     * @return the failures, each one as its item and its error
     */
    public List<List<String>> getFailures()
    {
        return this.container.findElements(By.cssSelector("tbody tr")).stream()
            .map(row -> row.findElements(By.tagName("td")).stream().map(WebElement::getText)
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
    }

    /**
     * @return the number of HTML elements inside the cells of the failures, which hold only text when the failures
     *     are escaped
     */
    public int countElementsInFailures()
    {
        return this.container.findElements(By.cssSelector("tbody td *")).size();
    }
}
