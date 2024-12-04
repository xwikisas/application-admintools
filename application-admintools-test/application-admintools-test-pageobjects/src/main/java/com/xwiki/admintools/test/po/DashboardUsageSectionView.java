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

/**
 * Represents actions that can be done on the usage section from within the AdminTools application.
 *
 * @version $Id$
 */
public class DashboardUsageSectionView extends ViewPage
{
    @FindBy(css = ".wiki-size-section")
    private WebElement instanceUsageContent;

    @FindBy(css = "a[href='#viewWikisSizeModal']")
    private WebElement wikiSizeModalHyperlink;

    @FindBy(css = "a[href='#pagesOverNumberOfComments']")
    private WebElement wikiSpamModalHyperLink;

    @FindBy(css = "a[href='#checkRecycleBinsModal']")
    private WebElement wikisRecycleBinsModalHyperLink;

    @FindBy(css = "a[href='#emptyPagesData']")
    private WebElement emptyPagesModalHyperLink;

    /**
     * Open the wikis size modal.
     */
    public WikisSizeModalView getWikisSizeModal()
    {
        wikiSizeModalHyperlink.click();
        return new WikisSizeModalView(By.id("viewWikisSizeModal"));
    }

    /**
     * Open the wikis spam modal.
     */
    public CommentsSpamModalView getWikiSpamModal()
    {
        wikiSpamModalHyperLink.click();
        return new CommentsSpamModalView(By.id("pagesOverNumberOfComments"));
    }

    /**
     * Open the wikis recycle bins modal.
     */
    public RecycleBinsModalView getRecycleBinsModalView()
    {
        wikisRecycleBinsModalHyperLink.click();
        return new RecycleBinsModalView(By.id("checkRecycleBinsModal"));
    }

    /**
     * Open the wikis empty pages modal.
     */
    public EmptyPagesModalView getEmptyPagesModalView()
    {
        emptyPagesModalHyperLink.click();
        return new EmptyPagesModalView(By.id("emptyPagesData"));
    }
}
