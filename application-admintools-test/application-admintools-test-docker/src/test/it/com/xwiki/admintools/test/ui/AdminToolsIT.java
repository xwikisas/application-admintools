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
package com.xwiki.admintools.test.ui;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.admintools.test.po.modals.AutoPurgeDefaultsModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeFailuresModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeHistoryModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeNowModalView;
import com.xwiki.admintools.test.po.modals.AutoPurgeWikiModalView;
import com.xwiki.admintools.test.po.modals.CommentsSpamModalView;
import com.xwiki.admintools.test.po.modals.DownloadArchiveModalView;
import com.xwiki.admintools.test.po.modals.EmptyPagesModalView;
import com.xwiki.admintools.test.po.modals.FlushCacheModalView;
import com.xwiki.admintools.test.po.modals.LastNLinesModalView;
import com.xwiki.admintools.test.po.modals.RecycleBinsModalView;
import com.xwiki.admintools.test.po.modals.WikisSizeModalView;
import com.xwiki.admintools.test.po.pages.AdminToolsHomePage;
import com.xwiki.admintools.test.po.pages.AutoPurgePage;
import com.xwiki.admintools.test.po.pages.CheckUserRightsPage;
import com.xwiki.admintools.test.po.sections.DashboardConfigurationSectionView;
import com.xwiki.admintools.test.po.sections.DashboardFilesSectionView;
import com.xwiki.admintools.test.po.sections.DashboardHealthSectionView;
import com.xwiki.admintools.test.po.sections.DashboardSecuritySectionView;
import com.xwiki.admintools.test.po.sections.DashboardUsageSectionView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest(servletEngine = ServletEngine.TOMCAT, servletEngineTag = "8", properties = {
    // The functional tests deny programming right to wiki content, except to the pages matching this pattern: the
    // automatic purge test needs it to move recycle bin items back in time.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:TestSpace\\.AutoPurgeFixture" })
class AdminToolsIT
{
    private static final List<String> supportedServers = List.of("TOMCAT");

    private static final String USER_NAME = "JonSnow";

    private static final DocumentReference ADMINTOOLS_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("AdminTools", "Code"), "Configuration");

    private static final DocumentReference ADMINTOOLS_WEBHOME_REFERENCE =
        new DocumentReference("xwiki", "AdminTools", "WebHome");

    private static final DocumentReference ADMINTOOLS_DELETE_PAGE_REF =
        new DocumentReference("xwiki", "TestSpace", "WebHome");

    private static final DocumentReference EMPTY_PAGE_REF = new DocumentReference("xwiki", "TestSpace", "emptyPage");

    private static final DocumentReference EMPTY_PAGE_WITH_COMM_REF =
        new DocumentReference("xwiki", "TestSpace", "emptyPageWithComm");

    private static final String ADMINTOOLS_CONFIGURATION_CLASSNAME = "AdminTools.Code.ConfigurationClass";

    private static final String MAIN_WIKI_NAME = "Home";

    private static final String DELETED_DOCUMENTS = "deletedDocuments";

    private static final String DELETED_ATTACHMENTS = "deletedAttachments";

    private static final String DELETED_DOCUMENTS_COLUMN = "Deleted pages";

    private static final String DELETED_ATTACHMENTS_COLUMN = "Deleted attachments";

    private static final DocumentReference PURGE_HISTORY_REF =
        new DocumentReference("xwiki", Arrays.asList("AdminTools", "Purge", "Wikis"), "xwiki");

    private static final String PURGE_HISTORY_CLASSNAME = "AdminTools.Purge.Code.HistoryClass";

    private static final DocumentReference OLD_DELETED_PAGE_REF =
        new DocumentReference("xwiki", "TestSpace", "AutoPurgePage");

    private static final AttachmentReference OLD_DELETED_ATTACHMENT_REF =
        new AttachmentReference("autoPurge.txt", new DocumentReference("xwiki", "TestSpace", "AutoPurgeAttachments"));

    private static final AttachmentReference RECENT_DELETED_ATTACHMENT_REF =
        new AttachmentReference("recent.txt", OLD_DELETED_ATTACHMENT_REF.getDocumentReference());

    private static final DocumentReference BACKDATE_FIXTURE_REF =
        new DocumentReference("xwiki", "TestSpace", "AutoPurgeFixture");

    /**
     * Moves the recycle bin entries of the test items 20 years back, so that a purge with a long limit removes them and
     * nothing else.
     */
    private static final String BACKDATE_SCRIPT =
        String.join("\n", "{{groovy}}", "import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback", "",
            "def oldDate = new java.sql.Timestamp(java.time.Instant.parse('2006-01-01T00:00:00Z').toEpochMilli())",
            // The context is a map, so its getters are called explicitly: '.wiki' would read the 'wiki' entry.
            "xcontext.context.getWiki().getHibernateStore().executeWrite(xcontext.context, { session ->",
            "  session.createNativeQuery('update xwikirecyclebin set xdd_date = :date where xdd_fullname = :name')",
            "    .setParameter('date', oldDate).setParameter('name', 'TestSpace.AutoPurgePage').executeUpdate()",
            "  session.createNativeQuery('update xwikiattrecyclebin set xda_date = :date where xda_filename = :name')",
            "    .setParameter('date', oldDate).setParameter('name', 'autoPurge.txt').executeUpdate()",
            "} as HibernateCallback)", "println 'backdated'", "{{/groovy}}");

    /**
     * The failures of the newest purge of the deleted attachments in the predefined history. They hold HTML and wiki
     * syntax, which must be shown as text.
     */
    private static final List<List<String>> PURGE_FAILURES =
        List.of(List.of("Sandbox.<img src=x onerror=alert(1)>", "java.lang.Exception: <script>alert(2)</script>"),
            List.of("Sandbox.Page **bold** [[link>>https://example.com]]", "{{html}}<b>x</b>{{/html}}"),
            List.of("Sandbox.Plain", ""));

    private static final String PASSWORD = "pass";

    private final List<String> supportedDatabases =
        List.of("mysql", "hsql", "hsqldb", "mariadb", "postgresql", "oracle");

    private final List<String> excludedLines =
        List.of("environment.permanentDirectory", "rendering.linkLabelFormat", "core.defaultDocumentSyntax", "MISC");

    @BeforeAll
    static void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser(USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name",
            "Snow");

        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin,programming", true);
        setup.createAdminUser();
        setup.loginAsAdmin();
    }

    @BeforeEach
    void goToPage()
    {
        AdminToolsHomePage.gotoPage();
    }

    @Test
    @Order(1)
    void appEntryRedirectsToHomePage()
    {
        // Check if the application entry point redirects to the home page when accessed directly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Admin Tools");
        Assertions.assertTrue(AdminToolsHomePage.isCurrentPage(vp));
    }

    @Test
    @Order(2)
    void backendSectionTest(TestConfiguration testConfiguration)
    {
        // We check that the retrieved backend information is correct.
        DashboardConfigurationSectionView configurationSectionView = AdminToolsHomePage.getConfigurationSection();
        String backendText = configurationSectionView.getText();
        assertTrue(supportedServers.stream().anyMatch(s -> backendText.toLowerCase().contains(s.toLowerCase())));

        // Depending on the database used for the test, we check if the right warning message is displayed in the UI.
        String configurationDatabase = testConfiguration.getDatabase().name().toLowerCase();
        List<WebElement> warningMessages = configurationSectionView.getErrorMessages();
        if (supportedDatabases.stream().anyMatch(configurationDatabase::contains)) {
            assertEquals(0, warningMessages.size());
            assertTrue(supportedDatabases.stream().anyMatch(d -> backendText.toLowerCase().contains(d)));
        } else {
            assertEquals(1, warningMessages.size());
        }
        assertTrue(supportedDatabases.stream().anyMatch(d -> backendText.toLowerCase().contains(d)));
        // We check if the backend information doesn't contain any "null" value, which would mean that some
        // information was not properly retrieved.
        assertFalse(backendText.toLowerCase().contains("null"));
    }

    @Test
    @Order(3)
    void viewLastLogLinesModalTest(TestUtils testUtils)
    {
        // We open and test the view last log lines modal, which should open a new tab with the logs when clicking on
        // the view button.
        DashboardConfigurationSectionView configurationSectionView = AdminToolsHomePage.getConfigurationSection();
        LastNLinesModalView lastLogsModal = configurationSectionView.clickViewLastLogsModal();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        lastLogsModal.clickViewButton();
        // We only check if the right tab was opened, as there are no logs in the docker tomcat server.
        switchToNewTab(testUtils, mainWindowHandle);
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
        lastLogsModal.clickCancelButton();
        assertFalse(lastLogsModal.isDisplayed());
    }

    @Test
    @Order(4)
    void xwikiFilesTest(TestUtils testUtils)
    {
        // In administration, we add some lines from the properties and configuration files to be excluded, to test
        // if they are correctly removed from the xwiki.cfg and xwiki.properties files content when retrieving them.
        excludeContent(testUtils, excludedLines);

        DashboardFilesSectionView filesSectionView = AdminToolsHomePage.getFilesSection();
        filesSectionView.clickPropertiesHyperlink();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        checkXWikiFileOpen(testUtils, mainWindowHandle);

        filesSectionView.clickConfigurationHyperlink();
        checkXWikiFileOpen(testUtils, mainWindowHandle);
    }

    @Test
    @Order(5)
    void archiveModalTest()
    {
        DashboardFilesSectionView filesSectionView = AdminToolsHomePage.getFilesSection();
        DownloadArchiveModalView archiveModalView = filesSectionView.clickDownloadModalHyperlink();
        WebElement configCheck = archiveModalView.getXWikiConfigCheckbox();
        WebElement propertiesCheck = archiveModalView.getXWikiPropertiesCheckBox();
        WebElement providerCheck = archiveModalView.getProviderCheckBox();
        WebElement logsCheck = archiveModalView.getLogsCheckBox();
        WebElement dateFilters = archiveModalView.getDateFilters();

        // We check the different options of the download archive modal, default display, interactions between the
        // options and the display of the date filters when the logs option is selected.
        assertTrue(configCheck.isSelected());
        assertTrue(propertiesCheck.isSelected());
        assertTrue(providerCheck.isSelected());
        assertTrue(logsCheck.isSelected());
        assertTrue(dateFilters.isDisplayed());
        logsCheck.click();
        assertFalse(logsCheck.isSelected());
        assertFalse(dateFilters.isDisplayed());
        configCheck.click();
        assertFalse(configCheck.isSelected());

        archiveModalView.clickDownloadButton();
        assertFalse(archiveModalView.isDisplayed());

        archiveModalView = filesSectionView.clickDownloadModalHyperlink();
        archiveModalView.clickCancelButton();
        assertFalse(archiveModalView.isDisplayed());
    }

    @Test
    @Order(6)
    void healthSectionTest(TestUtils testUtils)
    {
        DashboardHealthSectionView healthSectionView = AdminToolsHomePage.getHealthSection();
        // We run the health check job and check if the button is indeed disabled during the execution and enabled
        // again after.
        WebElement healthJobButton = healthSectionView.getHealthJobStartButton();
        healthJobButton.click();
        assertFalse(healthJobButton.isEnabled());
        testUtils.getDriver().waitUntilElementIsEnabled(healthJobButton);
        AdminToolsHomePage.gotoPage();

        // Because the health check result is inserted at runtime by a velocity script, the testUtils fails to select
        // the result message element. Therefore, it's necessary to select the text from the entire content. We then
        // check if the result message is correctly displayed.
        WebElement healthCheckResult = healthSectionView.getHealthContent();
        List<String> messages = List.of("Critical issues were found, please consult the results below!",
            "Some issues have been found, for more details please see the results below.", "No issue found!");

        boolean rightResult = messages.stream().anyMatch(healthCheckResult.getText()::contains);
        assertTrue(rightResult);

        // We check if the logs toggle works properly, and if all the check have run by counting the number of the log
        // items displayed in the logs section, which should be 13.
        WebElement logs = healthSectionView.getLogs();
        assertFalse(logs.isDisplayed());
        healthSectionView.clickResultsToggle();
        assertTrue(logs.isDisplayed());
        assertEquals(13, logs.findElements(By.className("log-item")).size());

        // We test the flush cache modal, checking if it is displayed when clicking on the hyperlink.
        // We then check if the cache flush action works properly when clicking on the confirm button and checking
        // if the success message is displayed. We also check if the cancel button properly closes the modal.
        FlushCacheModalView flushCacheModalView = healthSectionView.clickFlushCacheHyperlink();
        assertTrue(flushCacheModalView.isDisplayed());
        flushCacheModalView.clickConfirmButton();
        testUtils.getDriver()
            .waitUntilCondition(ExpectedConditions.visibilityOfElementLocated(By.className("xnotification-container")));
        WebElement alert = testUtils.getDriver().findElement(By.className("xnotification-container"));
        assertEquals("Cache flushed successfully.", alert.getText());
        assertFalse(flushCacheModalView.isDisplayed());

        flushCacheModalView = healthSectionView.clickFlushCacheHyperlink();
        flushCacheModalView.clickCancelButton();
        assertFalse(flushCacheModalView.isDisplayed());
    }

    @Test
    @Order(7)
    void usageSectionTest(TestUtils testUtils)
    {
        // Prepare prerequisites.
        setSpamCount(testUtils);
        addComments(testUtils);
        createEmptyPage(testUtils);
        AdminToolsHomePage.gotoPage();

        // Test the usage info for all wikis modal.
        DashboardUsageSectionView usageSectionView = AdminToolsHomePage.getInstanceUsageSection();
        WikisSizeModalView sizeModalView = usageSectionView.getWikisSizeModal();
        assertTrue(sizeModalView.isDisplayed());
        List<WebElement> sizeRows = sizeModalView.getTableRows();
        for (WebElement row : sizeRows) {
            assertFalse(row.getText().contains("null"));
        }
        assertEquals(1, Integer.parseInt(sizeModalView.getUserCount()));
        sizeModalView.clickCancelButton();
        assertFalse(sizeModalView.isDisplayed());

        // Test the spammed pages for all wikis modal. There should be one entry, the admin tools homepage with 4
        // comments.
        CommentsSpamModalView spamModalView = usageSectionView.getWikiSpamModal();
        assertTrue(spamModalView.isDisplayed());
        assertEquals("Pages with more than 2 comments", spamModalView.getTableTitle());
        WebElement spamRow = spamModalView.getTableRow();
        assertEquals("Home", spamRow.findElement(By.cssSelector("td:nth-child(1)")).getText());
        assertEquals("Admin Tools", spamRow.findElement(By.cssSelector("td:nth-child(2)")).getText());
        assertEquals("4", spamRow.findElement(By.cssSelector("td:nth-child(3)")).getText());
        spamModalView.clickCancelButton();
        assertFalse(spamModalView.isDisplayed());

        // Test the recycle bins view for all wikis modal. At first it should be empty and after creating and deleting
        // a page, it should contain one entry.
        RecycleBinsModalView recycleBinsModalView = usageSectionView.getRecycleBinsModalView();
        assertTrue(recycleBinsModalView.isDisplayed());
        WebElement recycleBinsTableRow = recycleBinsModalView.getTableRow();
        assertEquals("Home", recycleBinsTableRow.findElement(By.cssSelector("td:nth-child(1)")).getText());
        assertEquals("0", recycleBinsTableRow.findElement(By.cssSelector("td:nth-child(2)")).getText());
        assertEquals("0", recycleBinsTableRow.findElement(By.cssSelector("td:nth-child(3)")).getText());
        // The footer links to the deleted pages of the current wiki.
        assertTrue(recycleBinsModalView.getViewTrashBinURL().endsWith("/Main/AllDocs?view=deletedDocs"));
        createAndDeletePage(testUtils);
        AdminToolsHomePage.gotoPage();
        recycleBinsModalView = usageSectionView.getRecycleBinsModalView();
        assertTrue(recycleBinsModalView.isDisplayed());
        recycleBinsTableRow = recycleBinsModalView.getTableRow();
        assertEquals("1", recycleBinsTableRow.findElement(By.cssSelector("td:nth-child(2)")).getText());
        recycleBinsModalView.clickCancelButton();
        assertFalse(recycleBinsModalView.isDisplayed());

        // Test the empty pages view for all wikis modal.
        EmptyPagesModalView emptyPagesModalView = usageSectionView.getEmptyPagesModalView();
        assertTrue(emptyPagesModalView.isDisplayed());
        List<WebElement> emptyPagesRows = emptyPagesModalView.getTableRows();
        assertEquals(1, emptyPagesRows.size());
        WebElement emptyPageRow = emptyPagesRows.get(0);
        assertEquals("Home", emptyPageRow.findElement(By.cssSelector("td:nth-child(1)")).getText());
        assertEquals(EMPTY_PAGE_REF.toString(), emptyPageRow.findElement(By.cssSelector("td:nth-child(2)")).getText());
        emptyPagesModalView.clickCancelButton();
        assertFalse(emptyPagesModalView.isDisplayed());
    }

    @Test
    @Order(8)
    void scheduledPurgeTest(TestUtils testUtils) throws Exception
    {
        addPurgeHistory(testUtils);

        // The dashboard links to the automatic purge page.
        AutoPurgePage purgePage = AdminToolsHomePage.getInstanceUsageSection().clickAutoPurgeLink();
        assertTrue(purgePage.isCurrentPage());

        // The predefined history: the newest purge of the deleted attachments has failures.
        assertTrue(purgePage.getDefaultsSummary().contains("Deleted pages: not purged automatically"));
        assertTrue(purgePage.getWikiCell(MAIN_WIKI_NAME, DELETED_DOCUMENTS_COLUMN).contains("Default · disabled"));
        assertTrue(purgePage.getWikiCell(MAIN_WIKI_NAME, DELETED_ATTACHMENTS_COLUMN).contains("View the 120 failures"));
        assertTrue(purgePage.isPurgeNowButtonDangerous(MAIN_WIKI_NAME));
        AutoPurgeFailuresModalView failuresModal =
            purgePage.openFailuresModal(MAIN_WIKI_NAME, DELETED_ATTACHMENTS_COLUMN);
        assertEquals("Home · Deleted attachments · 2024/01/02 01:00", failuresModal.getSubtitle());
        assertEquals("Showing 3 of 120 failures. Failed items are retried at the next daily run.",
            failuresModal.getSummary());
        assertEquals(PURGE_FAILURES, failuresModal.getFailures());
        assertEquals(0, failuresModal.countElementsInFailures());
        failuresModal.close();

        AutoPurgeHistoryModalView historyModal = purgePage.openHistoryModal(MAIN_WIKI_NAME);
        assertEquals(MAIN_WIKI_NAME, historyModal.getWikiName());
        TableLayoutElement historyTable = historyModal.getHistoryTable();
        assertEquals(3, historyTable.countRows());
        // Newest first; a manual purge that removed nothing found no expired items.
        assertEquals("Deleted pages", historyTable.getCell("Item", 1).getText());
        assertEquals("No expired items", historyTable.getCell("Status", 1).getText());
        assertTrue(historyTable.getCell("Triggered by", 1).getText().startsWith("Manual"));
        assertEquals("Partial", historyTable.getCell("Status", 2).getText());
        assertEquals("120", historyTable.getCell("Failed", 2).getText());
        assertEquals("Scheduled", historyTable.getCell("Triggered by", 3).getText());
        historyTable.filterColumn("Status", "Partial", true,
            Map.of(TableLayoutElement.FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        historyTable.waitUntilRowCountEqualsTo(1);
        failuresModal = historyModal.openFailures(1);
        assertEquals(PURGE_FAILURES, failuresModal.getFailures());
        failuresModal.close();
        historyModal.close();
        // The filters are reset when the history is opened again.
        historyModal = purgePage.openHistoryModal(MAIN_WIKI_NAME);
        historyModal.getHistoryTable().waitUntilRowCountEqualsTo(3);
        historyModal.close();

        // Enable the purge of the deleted pages by default. The history size is checked with a translated message.
        AutoPurgeDefaultsModalView defaultsModal = purgePage.openDefaultsModal();
        assertFalse(defaultsModal.isIntervalEnabled(DELETED_DOCUMENTS));
        defaultsModal.setEnabled(DELETED_DOCUMENTS, true);
        assertTrue(defaultsModal.isIntervalEnabled(DELETED_DOCUMENTS));
        defaultsModal.setInterval(DELETED_DOCUMENTS, "10", "years");
        defaultsModal.setHistorySize("500");
        defaultsModal.submit();
        assertEquals(List.of("Enter a whole number between 1 and 200."), defaultsModal.getErrorMessages());
        assertTrue(defaultsModal.isDisplayed());
        defaultsModal.setHistorySize("20");
        defaultsModal.submit();
        defaultsModal.waitUntilClosed();
        purgePage.waitForNotificationSuccessMessage("The settings were saved.");
        purgePage.waitUntilDefaultsSummaryContains("Deleted pages: purged after 10 years");
        purgePage.waitUntilWikiCellContains(MAIN_WIKI_NAME, DELETED_DOCUMENTS_COLUMN, "Default · 10 years");
        // The changes of a cancelled edit are undone when the modal opens again.
        defaultsModal = purgePage.openDefaultsModal();
        defaultsModal.setEnabled(DELETED_DOCUMENTS, false);
        defaultsModal.close();
        defaultsModal = purgePage.openDefaultsModal();
        assertTrue(defaultsModal.isEnabled(DELETED_DOCUMENTS));
        assertTrue(defaultsModal.isIntervalEnabled(DELETED_DOCUMENTS));
        assertEquals("10", defaultsModal.getInterval(DELETED_DOCUMENTS));
        defaultsModal.close();

        // Customize the deleted attachments of the wiki, then go back to the defaults.
        AutoPurgeWikiModalView wikiModal = purgePage.openWikiModal(MAIN_WIKI_NAME);
        assertEquals(MAIN_WIKI_NAME, wikiModal.getWikiName());
        assertEquals("inherit", wikiModal.getSelectedMode(DELETED_ATTACHMENTS));
        assertEquals("10 years", wikiModal.getDefaultLabel(DELETED_DOCUMENTS));
        assertEquals("disabled", wikiModal.getDefaultLabel(DELETED_ATTACHMENTS));
        wikiModal.selectMode(DELETED_ATTACHMENTS, "enabled");
        wikiModal.setInterval(DELETED_ATTACHMENTS, "2", "weeks");
        wikiModal.submit();
        wikiModal.waitUntilClosed();
        purgePage.waitUntilWikiCellContains(MAIN_WIKI_NAME, DELETED_ATTACHMENTS_COLUMN, "Custom · 2 weeks");
        // The columns filter the wikis by their own configuration: the deleted pages of the wiki use the default limit,
        // so they are not custom.
        TableLayoutElement wikisTable = purgePage.getWikisTable();
        wikisTable.filterColumn(DELETED_ATTACHMENTS_COLUMN, "Custom", true,
            Map.of(TableLayoutElement.FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        wikisTable.waitUntilRowCountEqualsTo(1);
        // The filter waits until the wikis are loaded; waitUntilRowCountEqualsTo can't wait for an empty table.
        wikisTable.filterColumn(DELETED_DOCUMENTS_COLUMN, "Custom", true,
            Map.of(TableLayoutElement.FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
        assertEquals(0, wikisTable.countRows());
        purgePage = AutoPurgePage.gotoPage();
        wikiModal = purgePage.openWikiModal(MAIN_WIKI_NAME);
        assertEquals("enabled", wikiModal.getSelectedMode(DELETED_ATTACHMENTS));
        assertEquals("2", wikiModal.getInterval(DELETED_ATTACHMENTS));
        assertEquals("weeks", wikiModal.getUnit(DELETED_ATTACHMENTS));
        wikiModal.resetToDefaults();
        wikiModal.waitUntilClosed();
        purgePage.waitUntilWikiCellContains(MAIN_WIKI_NAME, DELETED_ATTACHMENTS_COLUMN, "Default · disabled");
    }

    @Test
    @Order(9)
    void manualPurgeTest(TestUtils testUtils) throws Exception
    {
        // Relies on the state left by the scheduled purge test: the predefined history, and the deleted pages purged
        // after 10 years by default.
        addOldRecycleBinItems(testUtils);
        AutoPurgePage purgePage = AutoPurgePage.gotoPage();

        // Purge the old items now, even the deleted attachments, whose automatic purge is disabled.
        AutoPurgeNowModalView purgeNowModal = purgePage.openPurgeNowModal(MAIN_WIKI_NAME);
        purgeNowModal.waitUntilCount(DELETED_DOCUMENTS, "(1 item)");
        assertEquals("10", purgeNowModal.getInterval(DELETED_DOCUMENTS));
        assertEquals("years", purgeNowModal.getUnit(DELETED_DOCUMENTS));
        assertFalse(purgeNowModal.isAutoPurgeDisabledNoteDisplayed(DELETED_DOCUMENTS));
        assertTrue(purgeNowModal.isAutoPurgeDisabledNoteDisplayed(DELETED_ATTACHMENTS));
        assertFalse(purgeNowModal.isPurgeEnabled());
        // The limit of an item type can be changed once the item type is selected.
        assertFalse(purgeNowModal.isIntervalEnabled(DELETED_ATTACHMENTS));
        purgeNowModal.select(DELETED_ATTACHMENTS);
        assertTrue(purgeNowModal.isIntervalEnabled(DELETED_ATTACHMENTS));
        // An invalid limit is explained, and its item type can't be purged.
        purgeNowModal.setInterval(DELETED_ATTACHMENTS, "5000", "years");
        purgeNowModal.waitUntilCount(DELETED_ATTACHMENTS, "");
        assertEquals(List.of("Enter a whole number between 0 and 1000."), purgeNowModal.getErrorMessages());
        assertFalse(purgeNowModal.isPurgeEnabled());
        // A limit of 0 empties the recycle bin: it also counts the attachment deleted just now. Only the old items are
        // purged, though.
        purgeNowModal.setInterval(DELETED_ATTACHMENTS, "0", "days");
        purgeNowModal.waitUntilCount(DELETED_ATTACHMENTS, "(2 items)");
        assertEquals(List.of(), purgeNowModal.getErrorMessages());
        purgeNowModal.setInterval(DELETED_ATTACHMENTS, "10", "years");
        purgeNowModal.waitUntilCount(DELETED_ATTACHMENTS, "(1 item)");
        assertEquals(List.of(), purgeNowModal.getErrorMessages());
        assertTrue(purgeNowModal.isPurgeEnabled());
        purgeNowModal.select(DELETED_DOCUMENTS);

        // The history of the wiki opens, showing the purges while they run, then their outcome.
        AutoPurgeHistoryModalView historyModal = purgeNowModal.purge();
        TableLayoutElement historyTable = historyModal.getHistoryTable();
        historyTable.waitUntilRowCountEqualsTo(5);
        historyModal.waitUntilNoPurgeInProgress();
        historyTable = historyModal.getHistoryTable();
        assertEquals(5, historyTable.countRows());
        for (int row = 1; row <= 2; row++) {
            assertEquals("Success", historyTable.getCell("Status", row).getText());
            assertEquals("1", historyTable.getCell("Removed", row).getText());
            assertTrue(historyTable.getCell("Triggered by", row).getText().startsWith("Manual"));
        }
        historyModal.close();

        // The failures of the older purge are no longer shown, and nothing older is left.
        purgePage = AutoPurgePage.gotoPage();
        assertFalse(purgePage.getWikiCell(MAIN_WIKI_NAME, DELETED_DOCUMENTS_COLUMN).contains("2024/01/03"));
        assertFalse(purgePage.getWikiCell(MAIN_WIKI_NAME, DELETED_ATTACHMENTS_COLUMN).contains("View the"));
        purgeNowModal = purgePage.openPurgeNowModal(MAIN_WIKI_NAME);
        purgeNowModal.waitUntilCount(DELETED_DOCUMENTS, "(nothing older)");
        purgeNowModal.close();
    }

    @Test
    @Order(10)
    void securitySectionTest()
    {
        // We check the security section content to see if the encoding information is correctly retrieved and
        // displayed.
        DashboardSecuritySectionView securitySection = AdminToolsHomePage.getSecuritySection();
        String content = securitySection.getContent();
        assertTrue(content.contains("Active encoding: UTF-8"));
        assertTrue(content.contains("Configuration encoding: UTF-8"));
        assertTrue(content.contains("File encoding: UTF-8"));
    }

    @Test
    @Order(11)
    void groupsRightsPage(TestUtils testUtils)
    {
        DashboardSecuritySectionView securitySection = AdminToolsHomePage.getSecuritySection();
        securitySection.clickGivenGroupsRights();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        switchToNewTab(testUtils, mainWindowHandle);

        // Check the groups rights livedata page for the "admin" space. The rights given to the XWikiAdminGroup on the
        // AdminTools space should be displayed.
        TableLayoutElement groupsTable = new LiveDataElement("viewGroupsRights").getTableLayout();
        assertTrue(groupsTable.countRows() > 0);
        groupsTable.filterColumn("Space", "admin", true);
        assertEquals(1, groupsTable.countRows());
        WebElement space = groupsTable.getCell("Space", 1);
        WebElement right = groupsTable.getCell("Type of rights", 1);
        WebElement group = groupsTable.getCell("Group", 1);
        WebElement levels = groupsTable.getCell("Security levels", 1);
        WebElement policy = groupsTable.getCell("Security policy", 1);

        assertEquals("AdminTools", space.getText());
        assertEquals("Space", right.getText());
        assertEquals("XWikiAdminGroup", group.getText());
        assertEquals("view,edit,delete", levels.getText());
        assertEquals("Allowed", policy.getText());
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    @Test
    @Order(12)
    void usersRightsPage(TestUtils testUtils)
    {
        DashboardSecuritySectionView securitySection = AdminToolsHomePage.getSecuritySection();
        securitySection.clickGivenUsersRights();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        switchToNewTab(testUtils, mainWindowHandle);

        // Check the users rights livedata page for the simple user "Jon", by setting the "User" filter. The user
        // should have edit rights on it's own user page.
        TableLayoutElement groupsTable = new LiveDataElement("ViewUsersRights").getTableLayout();
        groupsTable.filterColumn("User", "jon", true);
        assertEquals(1, groupsTable.countRows());
        WebElement space = groupsTable.getCell("Space", 1);
        WebElement right = groupsTable.getCell("Type of rights", 1);
        WebElement document = groupsTable.getCell("Document name", 1);
        WebElement user = groupsTable.getCell("User", 1);
        WebElement levels = groupsTable.getCell("Security levels", 1);
        WebElement policy = groupsTable.getCell("Security policy", 1);

        assertEquals("XWiki", space.getText());
        assertEquals("Page", right.getText());
        assertEquals("JonSnow", document.getText());
        assertEquals("Jon Snow", user.getText());
        assertEquals("edit", levels.getText());
        assertEquals("Allowed", policy.getText());
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    @Test
    @Order(13)
    void checkUserRightsPage(TestUtils testUtils)
    {
        // We check the functionality of the "Check user rights on page" document.
        DashboardSecuritySectionView securitySection = AdminToolsHomePage.getSecuritySection();
        securitySection.clickUsersRightsOnPage();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        switchToNewTab(testUtils, mainWindowHandle);

        // We set the target fields to check the rights of the "Jon Snow" user on the admin tools homepage.
        CheckUserRightsPage userRightsPage = new CheckUserRightsPage();
        userRightsPage.populateTargetPage("AdminTools.WebHome");
        userRightsPage.populateTargetUser("XWiki.JonSnow");

        testUtils.getDriver().addPageNotYetReloadedMarker();
        userRightsPage.clickCheckButton();
        testUtils.getDriver().waitUntilPageIsReloaded();

        // We check that the user has no admin rights on the admin tools homepage.
        userRightsPage = new CheckUserRightsPage();
        WebElement table = userRightsPage.getTable();
        verifyNonAdminUserRightsOnAdminPage(table);

        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    @Test
    @Order(14)
    void notAdminTest(TestUtils testUtils)
    {
        // We check that a non-admin user doesn't have access to the admin tools home page.
        testUtils.login(USER_NAME, PASSWORD);

        WebElement filesSectionNonAdminView = AdminToolsHomePage.gotoPage().getNonAdminUserView();
        assertTrue(filesSectionNonAdminView.getText()
            .contains("You are not allowed to view this page or perform this action."));

        // Nor to the automatic purge page.
        assertTrue(AutoPurgePage.gotoPage().getAccessDeniedMessage()
            .contains("You are not allowed to view this page or perform this action."));
    }

    private void verifyNonAdminUserRightsOnAdminPage(WebElement table)
    {
        List<WebElement> rows = table.findElements(By.cssSelector("tr"));
        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            if (!cells.isEmpty()) {
                assertEquals("Jon Snow", cells.get(0).getText());
                assertTrue(cells.get(1).getText().contains("admin"));
                // Check the view rights.
                assertEquals("false", cells.get(2).getText().trim());
                break;
            }
        }
    }

    /**
     * Switch to the new tab as Selenium does not change focus when a tab is opened.
     */
    private void switchToNewTab(TestUtils testUtils, String mainWindowHandle)
    {
        for (String windowHandle : testUtils.getDriver().getWindowHandles()) {
            if (!windowHandle.equals(mainWindowHandle)) {
                testUtils.getDriver().switchTo().window(windowHandle);
                break;
            }
        }
        assertNotEquals(mainWindowHandle, testUtils.getDriver().getWindowHandle());
    }

    private void checkXWikiFileOpen(TestUtils testUtils, String mainWindowHandle)
    {
        switchToNewTab(testUtils, mainWindowHandle);
        String fileContent = testUtils.getDriver().findElement(By.tagName("pre")).getText();
        for (String excludedLine : excludedLines) {
            assertFalse(fileContent.contains(excludedLine));
        }
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    /**
     * Add the given lines to the excluded lines configuration property.
     */
    private void excludeContent(TestUtils testUtils, List<String> lines)
    {
        testUtils.updateObject(ADMINTOOLS_CONFIGURATION_REFERENCE, ADMINTOOLS_CONFIGURATION_CLASSNAME, 0,
            "excludedLines", String.join(",", lines));
    }

    /**
     * Set the minimum number of comments to be considered as spam to 2, to be able to test the comments spam modal.
     */
    private void setSpamCount(TestUtils testUtils)
    {
        testUtils.updateObject(ADMINTOOLS_CONFIGURATION_REFERENCE, ADMINTOOLS_CONFIGURATION_CLASSNAME, 0, "spamSize",
            2);
    }

    /**
     * Add 4 comments to the Admin tools homepage.
     */
    private void addComments(TestUtils testUtils)
    {
        for (int i = 0; i < 4; i++) {
            Map<String, Object> parameters =
                Map.of("author", USER_NAME, "date", new Date(), "comment", String.format("test_%d", i));
            testUtils.addObject(ADMINTOOLS_WEBHOME_REFERENCE, "XWiki.XWikiComments", parameters);
        }
    }

    /**
     * Add a predefined purge history to the main wiki: a scheduled purge of the deleted attachments, a newer one that
     * failed to remove some items, and a manual purge of the deleted pages that found nothing to remove.
     */
    private void addPurgeHistory(TestUtils testUtils) throws Exception
    {
        testUtils.rest().savePage(PURGE_HISTORY_REF);
        addPurgeHistoryEntry(testUtils,
            Map.of("type", DELETED_ATTACHMENTS, "date", "2024-01-01 01:00:00", "triggeredBy", "scheduled", "status",
                "success", "removedCount", "4", "failedCount", "0", "latest", "0"));
        String failures = PURGE_FAILURES.stream().map(failure -> String.join("\t", failure).strip())
            .collect(Collectors.joining("\n"));
        addPurgeHistoryEntry(testUtils,
            Map.of("type", DELETED_ATTACHMENTS, "date", "2024-01-02 01:00:00", "triggeredBy", "scheduled", "status",
                "partial", "removedCount", "1", "failedCount", "120", "failures", failures, "latest", "1"));
        addPurgeHistoryEntry(testUtils,
            Map.of("type", DELETED_DOCUMENTS, "date", "2024-01-03 01:00:00", "triggeredBy", "xwiki:XWiki.Admin",
                "status", "success", "removedCount", "0", "failedCount", "0", "latest", "1"));
    }

    /**
     * Add an entry to the purge history of the main wiki.
     */
    private void addPurgeHistoryEntry(TestUtils testUtils, Map<String, String> properties) throws Exception
    {
        org.xwiki.rest.model.jaxb.Object entry = testUtils.rest().object(PURGE_HISTORY_REF, PURGE_HISTORY_CLASSNAME);
        properties.forEach((name, value) -> entry.getProperties().add(TestUtils.RestTestUtils.property(name, value)));
        testUtils.rest().add(entry);
    }

    /**
     * Delete a page and an attachment, then move their recycle bin entries 20 years back. Also delete a second
     * attachment, which stays recent.
     */
    private void addOldRecycleBinItems(TestUtils testUtils) throws Exception
    {
        testUtils.createPage(OLD_DELETED_PAGE_REF, "Old deleted page");
        testUtils.deletePage(OLD_DELETED_PAGE_REF);
        testUtils.createPage(OLD_DELETED_ATTACHMENT_REF.getDocumentReference(), "Page with an old deleted attachment");
        testUtils.rest().attachFile(OLD_DELETED_ATTACHMENT_REF, "content".getBytes(), true);
        testUtils.rest().deleteAttachement(OLD_DELETED_ATTACHMENT_REF);
        testUtils.rest().attachFile(RECENT_DELETED_ATTACHMENT_REF, "content".getBytes(), true);
        testUtils.rest().deleteAttachement(RECENT_DELETED_ATTACHMENT_REF);
        assertEquals("backdated", testUtils.createPage(BACKDATE_FIXTURE_REF, BACKDATE_SCRIPT).getContent());
    }

    /**
     * Create and delete a page to be able to test the recycle bin and empty pages modals.
     */
    private void createAndDeletePage(TestUtils testUtils)
    {
        testUtils.createPage(ADMINTOOLS_DELETE_PAGE_REF, "testContent");
        testUtils.deletePage(ADMINTOOLS_DELETE_PAGE_REF);
    }

    /**
     * Create an empty page and a page with only comments, to be able to test the empty pages modal.
     */
    private void createEmptyPage(TestUtils testUtils)
    {
        testUtils.createPage(EMPTY_PAGE_REF, "");
        testUtils.createPage(EMPTY_PAGE_WITH_COMM_REF, "");
        Map<String, Object> parameters = Map.of("author", USER_NAME, "date", new Date(), "comment", "test");
        testUtils.addObject(EMPTY_PAGE_WITH_COMM_REF, "XWiki.XWikiComments", parameters);
    }
}
