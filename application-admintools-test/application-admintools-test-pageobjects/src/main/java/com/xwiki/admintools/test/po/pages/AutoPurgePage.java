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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.admintools.test.po.modals.AutoPurgeDefaultsModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeFailuresModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeHistoryModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeNowModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeWikiModalView;

/**
 * Represents the page configuring the automatic purge of the recycle bins.
 *
 * @version $Id$
 */
public class AutoPurgePage extends ViewPage
{
    /**
     * The label of the column holding the wiki names.
     */
    public static final String WIKI_COLUMN = "Wiki";

    private static final DocumentReference PAGE_REFERENCE =
        new DocumentReference("xwiki", List.of("AdminTools", "Purge"), "WebHome");

    private static final String WIKIS_LIVE_DATA = "purgeWikis";

    /**
     * Open the page.
     *
     * @return the page
     */
    public static AutoPurgePage gotoPage()
    {
        getUtil().gotoPage(PAGE_REFERENCE);
        return new AutoPurgePage();
    }

    /**
     * @return {@code true} if the current page is the automatic purge page
     */
    public boolean isCurrentPage()
    {
        return "AdminTools.Purge".equals(getMetaDataValue("space")) && "WebHome".equals(getMetaDataValue("page"));
    }

    /**
     * @return the text of the summary of the default settings
     */
    public String getDefaultsSummary()
    {
        return getDriver().findElement(By.cssSelector(".purge-defaults ul")).getText();
    }

    /**
     * @param type the identifier of an item type
     * @return {@code true} if the default limit of the item type is highlighted as invalid in the summary
     */
    public boolean isDefaultsSummaryWarning(String type)
    {
        return getDriver().findElement(By.cssSelector(".purge-defaults-" + type)).getAttribute("class")
            .contains("text-warning");
    }

    /**
     * Wait until the summary of the default settings, refreshed after they are saved, holds a text.
     *
     * @param text the expected text
     */
    public void waitUntilDefaultsSummaryContains(String text)
    {
        getDriver().waitUntilCondition(driver -> getDefaultsSummary().contains(text));
    }

    /**
     * Open the modal editing the default settings.
     *
     * @return the modal
     */
    public AutoPurgeDefaultsModalView openDefaultsModal()
    {
        AutoPurgeDefaultsModalView modal = new AutoPurgeDefaultsModalView();
        getDriver().findElement(By.cssSelector(".purge-defaults button")).click();
        modal.waitUntilDisplayed();
        return modal;
    }

    /**
     * @return the table listing the wikis, once loaded
     */
    public TableLayoutElement getWikisTable()
    {
        TableLayoutElement table = new LiveDataElement(WIKIS_LIVE_DATA).getTableLayout();
        table.waitUntilReady();
        return table;
    }

    /**
     * @param wikiName the name of a wiki
     * @param column the label of a column
     * @return the text of the cell of the wiki in the column
     */
    public String getWikiCell(String wikiName, String column)
    {
        return getWikisTable().getCell(column, getRowNumber(wikiName)).getText();
    }

    /**
     * @param wikiName the name of a wiki
     * @param column the label of a column
     * @return {@code true} if the limit shown in the cell of the wiki is highlighted as invalid
     */
    public boolean isWikiCellWarning(String wikiName, String column)
    {
        return !getWikisTable().getCell(column, getRowNumber(wikiName)).findElements(By.className("text-warning"))
            .isEmpty();
    }

    /**
     * Wait until the cell of a wiki, refreshed after a change, holds a text.
     *
     * @param wikiName the name of a wiki
     * @param column the label of a column
     * @param text the expected text
     */
    public void waitUntilWikiCellContains(String wikiName, String column, String text)
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return getWikiCell(wikiName, column).contains(text);
            } catch (RuntimeException e) {
                // The table is being refreshed.
                return false;
            }
        });
    }

    /**
     * Open the modal editing the settings of a wiki.
     *
     * @param wikiName the name of the wiki
     * @return the modal
     */
    public AutoPurgeWikiModalView openWikiModal(String wikiName)
    {
        AutoPurgeWikiModalView modal = new AutoPurgeWikiModalView();
        clickInRow(wikiName, ".purge-edit");
        modal.waitUntilDisplayed();
        return modal;
    }

    /**
     * Open the modal purging a wiki now.
     *
     * @param wikiName the name of the wiki
     * @return the modal
     */
    public AutoPurgeNowModalView openPurgeNowModal(String wikiName)
    {
        AutoPurgeNowModalView modal = new AutoPurgeNowModalView();
        clickInRow(wikiName, ".purge-now");
        modal.waitUntilDisplayed();
        return modal;
    }

    /**
     * @param wikiName the name of a wiki
     * @return {@code true} if the button purging the wiki now is red, like the other deletion buttons
     */
    public boolean isPurgeNowButtonDangerous(String wikiName)
    {
        return findInRow(wikiName, ".purge-now").getAttribute("class").contains("btn-danger");
    }

    /**
     * Open the modal listing the purge history of a wiki.
     *
     * @param wikiName the name of the wiki
     * @return the modal, once its history is loaded
     */
    public AutoPurgeHistoryModalView openHistoryModal(String wikiName)
    {
        AutoPurgeHistoryModalView modal = new AutoPurgeHistoryModalView();
        clickInRow(wikiName, ".purge-history");
        modal.waitUntilDisplayed();
        return modal;
    }

    /**
     * Open the failures of the newest purge of an item type of a wiki, from the link shown under its status.
     *
     * @param wikiName the name of the wiki
     * @param column the label of the column of the item type
     * @return the modal, once the failures are loaded
     */
    public AutoPurgeFailuresModalView openFailuresModal(String wikiName, String column)
    {
        AutoPurgeFailuresModalView modal = new AutoPurgeFailuresModalView();
        getWikisTable().getCell(column, getRowNumber(wikiName)).findElement(By.cssSelector(".purge-failures"))
            .click();
        modal.waitUntilLoaded();
        return modal;
    }

    /**
     * @return the message shown to the users who can't view the page
     */
    public String getAccessDeniedMessage()
    {
        return getDriver().findElement(By.cssSelector(".panel-body")).getText();
    }

    /**
     * @param wikiName the name of a wiki
     * @return the number of the row of the wiki in the table, starting at 1
     */
    private int getRowNumber(String wikiName)
    {
        List<WebElement> cells = getWikisTable().getAllCells(WIKI_COLUMN);
        for (int i = 0; i < cells.size(); i++) {
            if (wikiName.equals(cells.get(i).getText())) {
                return i + 1;
            }
        }
        throw new IllegalArgumentException(String.format("No row for wiki [%s].", wikiName));
    }

    /**
     * @param wikiName the name of a wiki
     * @param selector the CSS selector of an element of the row of the wiki
     * @return the element
     */
    private WebElement findInRow(String wikiName, String selector)
    {
        return getWikisTable().findElementInRow(getRowNumber(wikiName), By.cssSelector(selector));
    }

    /**
     * Click an element of the row of a wiki.
     *
     * @param wikiName the name of a wiki
     * @param selector the CSS selector of the element
     */
    private void clickInRow(String wikiName, String selector)
    {
        findInRow(wikiName, selector).click();
    }
}
