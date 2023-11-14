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
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import com.xwiki.admintools.test.po.AdminToolsHomePage;
import com.xwiki.admintools.test.po.AdminToolsViewPage;
import com.xwiki.admintools.test.po.DownloadArchiveModalView;
import com.xwiki.admintools.test.po.LastNLinesModalView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class AdminToolsIT
{
    private static final List<String> supportedServers = List.of("TOMCAT");

    private static final String USER_NAME = "JonSnow";

    private static final DocumentReference ADMINTOOLS_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("AdminTools", "Code"), "Configuration");

    private static final String ADMINTOOLS_CONFIGURATION_CLASSNAME = "AdminTools.Code.ConfigurationClass";

    private static final String BACKEND_SECTION_VIEW_LAST_LOGS_MODAL_ID = "#configurationViewLastNLinesModal";

    private static final String DOWNLOAD_FILES_MODAL_ID = "#downloadFilesModal";

    private static final String PASSWORD = "pass";

    private static boolean isSupportedServer = true;

    private final List<String> supportedDatabases =
        List.of("mysql", "hsql", "hsqldb", "mariadb", "postgresql", "oracle");

    private final List<String> excludedLines =
        List.of("environment.permanentDirectory", "rendering.linkLabelFormat", "core.defaultDocumentSyntax", "MISC");

    @BeforeAll
    static void setUp(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.createUser(USER_NAME, PASSWORD, setup.getURLToNonExistentPage(), "first_name", "Jon", "last_name",
            "Snow");
        String serverType = testConfiguration.getServletEngine().name();
        if (!supportedServers.contains(serverType)) {
            isSupportedServer = false;
            AdminToolsHomePage page = AdminToolsHomePage.gotoPage();
            setup.gotoPage(page.getPageURL());
            AdminToolsViewPage webHomePage = new AdminToolsViewPage();
            assertEquals(0, webHomePage.getDashboardElements().size());
            List<WebElement> warningElements = webHomePage.getWarningElements();
            assertEquals(2, warningElements.size());
        }

        // By default the minimal distribution used for the tests doesn't have any rights setup. Let's create an Admin
        // user part of the Admin Group and make sure that this Admin Group has admin rights in the wiki. We could also
        // have given that Admin user the admin right directly but the solution we chose is closer to the XS
        // distribution.
        setup.loginAsSuperAdmin();
        setup.setGlobalRights("XWiki.XWikiAdminGroup", "", "admin", true);
        setup.createAdminUser();
        setup.loginAsAdmin();
    }

    @BeforeEach
    void goToPage(TestUtils testUtils)
    {
        AdminToolsHomePage page = AdminToolsHomePage.gotoPage();
        testUtils.gotoPage(page.getPageURL());
        page.waitUntilPageIsReady();
    }

    @Test
    void adminToolsHomePageBackend(TestConfiguration testConfiguration)
    {
        if (!isSupportedServer) {
            return;
        }
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        assertEquals(2, webHomePage.getDashboardElements().size());
        String backendText = webHomePage.getBackendText();
        assertTrue(supportedServers.stream().anyMatch(s -> backendText.toLowerCase().contains(s.toLowerCase())));

        String configurationDatabase = testConfiguration.getDatabase().name().toLowerCase();
        List<WebElement> warningMessages = webHomePage.getBackendErrorMessages();
        if (supportedDatabases.stream().anyMatch(configurationDatabase::contains)) {
            assertEquals(0, warningMessages.size());
        } else {
            assertEquals(1, warningMessages.size());
        }
        assertTrue(supportedDatabases.stream().anyMatch(d -> backendText.toLowerCase().contains(d)));
        assertFalse(backendText.toLowerCase().contains("null"));
    }

    @Test
    void adminToolViewLastLogLinesModal(TestUtils testUtils)
    {
        if (!isSupportedServer) {
            return;
        }
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        WebElement viewLastLogsHyperlink = webHomePage.getBackendLogsHyperlink();
        viewLastLogsHyperlink.click();
        LastNLinesModalView lastLinesModalView = new LastNLinesModalView();
        lastLinesModalView.waitUntilPageIsReady();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        lastLinesModalView.clickButton(BACKEND_SECTION_VIEW_LAST_LOGS_MODAL_ID);
        switchToNewTab(testUtils, mainWindowHandle);
        testUtils.getDriver().switchTo().window(mainWindowHandle);
        WebElement cancelButton = lastLinesModalView.getCancelButton(BACKEND_SECTION_VIEW_LAST_LOGS_MODAL_ID);
        cancelButton.click();
        lastLinesModalView = new LastNLinesModalView();
        assertTrue(lastLinesModalView.getModalText().isEmpty());
    }

    @Test
    void adminToolsHomePageFiles(TestUtils testUtils)
    {
        if (!isSupportedServer) {
            return;
        }
        excludeContent(testUtils, excludedLines);
        AdminToolsHomePage page = AdminToolsHomePage.gotoPage();
        testUtils.gotoPage(page.getPageURL());
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        WebElement propertiesHyperlink = webHomePage.getPropertiesHyperlink();
        WebElement configurationHyperlink = webHomePage.getConfigurationHyperlink();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();
        propertiesHyperlink.click();
        checkXWikiFileOpen(testUtils, mainWindowHandle);
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
        configurationHyperlink.click();
        checkXWikiFileOpen(testUtils, mainWindowHandle);
    }

    @Test
    void adminToolDownloadArchiveModal()
    {
        if (!isSupportedServer) {
            return;
        }
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        webHomePage.waitUntilPageIsReady();
        WebElement archiveDownloadHyperlink = webHomePage.getFilesArchiveHyperlink();
        archiveDownloadHyperlink.click();
        DownloadArchiveModalView archiveModalView = new DownloadArchiveModalView();
        WebElement configCheck = archiveModalView.getXWikiConfigCheck();
        WebElement propertiesCheck = archiveModalView.getXWikiPropertiesCheck();
        WebElement providerCheck = archiveModalView.getProviderCheck();
        WebElement logsCheck = archiveModalView.getLogsCheck();
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

        WebElement downloadButton = archiveModalView.getViewButton(DOWNLOAD_FILES_MODAL_ID);
        WebElement cancelButton = archiveModalView.getCancelButton(DOWNLOAD_FILES_MODAL_ID);

        downloadButton.click();
        archiveModalView = new DownloadArchiveModalView();
        assertTrue(archiveModalView.getModalText().isEmpty());

        archiveDownloadHyperlink.click();
        assertFalse(archiveModalView.getModalText().isEmpty());
        cancelButton.click();
        archiveModalView = new DownloadArchiveModalView();
        assertTrue(archiveModalView.getModalText().isEmpty());
    }

    @Test
    void adminToolsHomePageFilesNotAdmin(TestUtils testUtils)
    {
        if (!isSupportedServer) {
            return;
        }
        testUtils.login(USER_NAME, PASSWORD);
        AdminToolsViewPage webHomePage = new AdminToolsViewPage();
        WebElement propertiesHyperlink = webHomePage.getPropertiesHyperlink();
        WebElement configurationHyperlink = webHomePage.getConfigurationHyperlink();
        String mainWindowHandle = testUtils.getDriver().getWindowHandle();

        // Click the anchor, switch tabs to the new page and check the content. After this, close the new tab and
        // change focus to the main page.
        propertiesHyperlink.click();
        checkTabOpenForNonAdmin(testUtils, mainWindowHandle);

        configurationHyperlink.click();
        checkTabOpenForNonAdmin(testUtils, mainWindowHandle);

        WebElement archiveDownloadHyperlink = webHomePage.getFilesArchiveHyperlink();
        archiveDownloadHyperlink.click();
        DownloadArchiveModalView archiveModalView = new DownloadArchiveModalView();
        WebElement downloadButton = archiveModalView.getViewButton(DOWNLOAD_FILES_MODAL_ID);
        downloadButton.click();
        WebElement tabContent = testUtils.getDriver().findElement(By.tagName("body"));
        assertTrue(tabContent.getText().contains("Unauthorized"));
    }

    /**
     * Switch to new tab as Selenium does not change focus when a tab is opened.
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

    private void checkTabOpenForNonAdmin(TestUtils testUtils, String mainWindowHandle)
    {
        switchToNewTab(testUtils, mainWindowHandle);
        WebElement tabContent = testUtils.getDriver().findElement(By.tagName("body"));
        assertTrue(tabContent.getText().contains("Unauthorized"));
        testUtils.getDriver().close();
        testUtils.getDriver().switchTo().window(mainWindowHandle);
    }

    private void checkXWikiFileOpen(TestUtils testUtils, String mainWindowHandle)
    {
        switchToNewTab(testUtils, mainWindowHandle);
        String fileContent = testUtils.getDriver().findElement(By.tagName("pre")).getText();
        System.out.println(fileContent);
        for (String excludedLine : excludedLines) {
            assertFalse(fileContent.contains(excludedLine));
        }
    }

    private void excludeContent(TestUtils testUtils, List<String> lines)
    {
        testUtils.updateObject(ADMINTOOLS_CONFIGURATION_REFERENCE, ADMINTOOLS_CONFIGURATION_CLASSNAME, 0,
            "excludedLines", String.join(",", lines));
    }
}