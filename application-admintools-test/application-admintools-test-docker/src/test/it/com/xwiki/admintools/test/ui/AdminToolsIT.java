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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import com.xwiki.admintools.test.po.AdminToolsHomePage;
import com.xwiki.admintools.test.po.CommentsSpamModalView;
import com.xwiki.admintools.test.po.DashboardConfigurationSectionView;
import com.xwiki.admintools.test.po.DashboardFilesSectionView;
import com.xwiki.admintools.test.po.DashboardHealthSectionView;
import com.xwiki.admintools.test.po.DashboardUsageSectionView;
import com.xwiki.admintools.test.po.DownloadArchiveModalView;
import com.xwiki.admintools.test.po.EmptyPagesModalView;
import com.xwiki.admintools.test.po.FlushCacheModalView;
import com.xwiki.admintools.test.po.LastNLinesModalView;
import com.xwiki.admintools.test.po.RecycleBinsModalView;
import com.xwiki.admintools.test.po.WikisSizeModalView;

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
        setup.createUser(USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name",
            "Snow");

        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.loginAsSuperAdmin();
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
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Admin Tools");
        Assertions.assertTrue(AdminToolsHomePage.isCurrentPage(vp));
    }

    @Test
    @Order(2)
    void adminToolsHomePageBackend(TestConfiguration testConfiguration)
    {
        DashboardConfigurationSectionView configurationSectionView = AdminToolsHomePage.getConfigurationSection();
        String backendText = configurationSectionView.getText();
        assertTrue(supportedServers.stream().anyMatch(s -> backendText.toLowerCase().contains(s.toLowerCase())));

        String configurationDatabase = testConfiguration.getDatabase().name().toLowerCase();
        List<WebElement> warningMessages = configurationSectionView.getErrorMessages();
        if (supportedDatabases.stream().anyMatch(configurationDatabase::contains)) {
            assertEquals(0, warningMessages.size());
        } else {
            assertEquals(1, warningMessages.size());
        }
        assertTrue(supportedDatabases.stream().anyMatch(d -> backendText.toLowerCase().contains(d)));
        assertFalse(backendText.toLowerCase().contains("null"));
    }

    @Test
    @Order(3)
    void adminToolViewLastLogLinesModal(TestUtils testUtils)
    {
        DashboardConfigurationSectionView configurationSectionView = AdminToolsHomePage.getConfigurationSection();
        LastNLinesModalView lastLogsModal = configurationSectionView.clickViewLastLogsModal();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        lastLogsModal.clickViewButton();
        switchToNewTab(testUtils, mainWindowHandle);
        testUtils.getDriver().switchTo().window(mainWindowHandle);
        lastLogsModal.clickCancelButton();
        assertFalse(lastLogsModal.isDisplayed());
    }

    @Test
    @Order(4)
    void adminToolsHomePageFiles(TestUtils testUtils)
    {
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
    void adminToolDownloadArchiveModal()
    {
        DashboardFilesSectionView filesSectionView = AdminToolsHomePage.getFilesSection();

        DownloadArchiveModalView archiveModalView = filesSectionView.clickDownloadModalHyperlink();
        WebElement configCheck = archiveModalView.getXWikiConfigCheckbox();
        WebElement propertiesCheck = archiveModalView.getXWikiPropertiesCheckBox();
        WebElement providerCheck = archiveModalView.getProviderCheckBox();
        WebElement logsCheck = archiveModalView.getLogsCheckBox();
        WebElement dateFilters = archiveModalView.getDateFilters();

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
    void adminToolsHealthSection(TestUtils testUtils)
    {
        DashboardHealthSectionView healthSectionView = AdminToolsHomePage.getHealthSection();

        WebElement healthJobButton = healthSectionView.getHealthJobStartButton();
        healthJobButton.click();
        assertFalse(healthJobButton.isEnabled());
        testUtils.getDriver().waitUntilElementIsEnabled(healthJobButton);
        AdminToolsHomePage.gotoPage();

        // Because the health check result is inserted at runtime by a velocity script, the testUtils fails to select
        // the result message element. Therefore, it's necessary to select the text from the entire content.
        WebElement healthCheckResult = healthSectionView.getResult();
        List<String> messages = List.of("Critical issues were found, please consult the results below!",
            "Some issues have been found, for more details please see the results below.", "No issue found!");

        boolean rightResult = messages.stream().anyMatch(healthCheckResult.getText()::equals);
        assertTrue(rightResult);

        WebElement logs = healthSectionView.getLogs();
        assertFalse(logs.isDisplayed());
        healthSectionView.clickResultsToggle();
        assertTrue(logs.isDisplayed());
        assertEquals(12, logs.findElements(By.className("log-item")).size());

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
    void adminToolsUsageSection(TestUtils testUtils)
    {
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
        assertEquals(2, Integer.parseInt(sizeModalView.getUserCount()));
        sizeModalView.clickCancelButton();
        assertFalse(sizeModalView.isDisplayed());

        // Test the spammed pages for all wikis modal.
        CommentsSpamModalView spamModalView = usageSectionView.getWikiSpamModal();
        assertTrue(spamModalView.isDisplayed());
        assertEquals("Pages with more than 2 comments", spamModalView.getTableTitle());
        WebElement spamRow = spamModalView.getTableRow();
        assertEquals("Home", spamRow.findElement(By.cssSelector("td:nth-child(1)")).getText());
        assertEquals("$services.localization.render(\"adminTools.extension.title\")",
            spamRow.findElement(By.cssSelector("td:nth-child(2)")).getText());
        assertEquals("3", spamRow.findElement(By.cssSelector("td:nth-child(3)")).getText());
        spamModalView.clickCancelButton();
        assertFalse(spamModalView.isDisplayed());

        // Test the recycle bins view for all wikis modal.
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
    void adminToolsHomePageFilesNotAdmin(TestUtils testUtils)
    {
        testUtils.login(USER_NAME, PASSWORD);

        WebElement filesSectionNonAdminView = AdminToolsHomePage.gotoPage().getNonAdminUserView();
        assertTrue(filesSectionNonAdminView.getText().contains("Access denied due to missing admin rights!"));
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

    private void excludeContent(TestUtils testUtils, List<String> lines)
    {
        testUtils.updateObject(ADMINTOOLS_CONFIGURATION_REFERENCE, ADMINTOOLS_CONFIGURATION_CLASSNAME, 0,
            "excludedLines", String.join(",", lines));
    }

    private void setSpamCount(TestUtils testUtils)
    {
        testUtils.updateObject(ADMINTOOLS_CONFIGURATION_REFERENCE, ADMINTOOLS_CONFIGURATION_CLASSNAME, 0, "spamSize",
            2);
    }

    private void addComments(TestUtils testUtils)
    {
        for (int i = 1; i < 4; i++) {
            Map<String, Object> parameters =
                Map.of("author", USER_NAME, "date", new Date(), "comment", String.format("test_%d", i));
            testUtils.addObject(ADMINTOOLS_WEBHOME_REFERENCE, "XWiki.XWikiComments", parameters);
        }
    }

    private void createAndDeletePage(TestUtils testUtils)
    {
        testUtils.createPage(ADMINTOOLS_DELETE_PAGE_REF, "testContent");
        testUtils.deletePage(ADMINTOOLS_DELETE_PAGE_REF);
    }

    private void createEmptyPage(TestUtils testUtils)
    {
        testUtils.createPage(EMPTY_PAGE_REF, "");
        testUtils.createPage(EMPTY_PAGE_WITH_COMM_REF, "");
        Map<String, Object> parameters = Map.of("author", USER_NAME, "date", new Date(), "comment", "test");
        testUtils.addObject(EMPTY_PAGE_WITH_COMM_REF, "XWiki.XWikiComments", parameters);
    }
}