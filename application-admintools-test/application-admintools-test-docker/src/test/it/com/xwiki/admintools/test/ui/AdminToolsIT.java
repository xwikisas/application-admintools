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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.admintools.test.po.modals.CommentsSpamModalView;
import com.xwiki.admintools.test.po.modals.DownloadArchiveModalView;
import com.xwiki.admintools.test.po.modals.EmptyPagesModalView;
import com.xwiki.admintools.test.po.modals.FlushCacheModalView;
import com.xwiki.admintools.test.po.modals.LastNLinesModalView;
import com.xwiki.admintools.test.po.modals.RecycleBinsModalView;
import com.xwiki.admintools.test.po.modals.WikisSizeModalView;
import com.xwiki.admintools.test.po.pages.AdminToolsHomePage;
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

@UITest(servletEngine = ServletEngine.TOMCAT, servletEngineTag = "8")
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
    @Order(9)
    void groupsRightsPage(TestUtils testUtils)
    {
        DashboardSecuritySectionView securitySection = AdminToolsHomePage.getSecuritySection();
        securitySection.clickGivenGroupsRights();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        switchToNewTab(testUtils, mainWindowHandle);

        // Check the groups rights livedata page for the "admin" space. The view right given to the XWikiAdminGroup
        // should be displayed.
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
        assertEquals("view", levels.getText());
        assertEquals("Allowed", policy.getText());
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    @Test
    @Order(10)
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
    @Order(11)
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
    @Order(12)
    void notAdminTest(TestUtils testUtils)
    {
        // We check that a non-admin user doesn't have access to the admin tools home page.
        testUtils.login(USER_NAME, PASSWORD);

        WebElement filesSectionNonAdminView = AdminToolsHomePage.gotoPage().getNonAdminUserView();
        assertTrue(filesSectionNonAdminView.getText()
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