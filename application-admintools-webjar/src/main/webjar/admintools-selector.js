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

require.config({
  paths: {
    'xwiki-suggestPages': "$xwiki.getSkinFile('uicomponents/suggest/suggestPages.js', true)?" +
    "v=$escapetool.url($xwiki.version)",
    'xwiki-suggestWikiUsers': "$xwiki.getSkinFile('XWikiProCommons.Pickers.UserWikiPicker', true)?" +
    "v=$escapetool.url($xwiki.version)"
  }
});

// As there is no platform implementation to allow the user to dynamically select the page from where the users and
// pages are shown, a custom implementation was made to dynamically update the displayed picker options in correlation
// to the selected wiki parameter. This can be removed after the implementation of:
// XWIKI-22850: Add an option in a macro parameter to select the page/space for attachment suggestions
// XWIKI-23006: Add a data-search-scope for wiki selection for user/groups suggestion widget
define(['jquery', 'xwiki-meta', 'xwiki-suggestPages', 'xwiki-suggestWikiUsers'], function ($, xm) {
  const sharedPickers = {
    removeSelectize: function (element) {
      let selectize = element.siblings('.selectize-control');
      if (selectize.length) {
        selectize.remove();
        element.removeAttr('class tabindex style').val('');
        const clone = element.clone().appendTo(element.parent());
        element.remove();
        element = clone;
      }
      return element;
    },

    initializePages: function (selectElement, pageInputName, scopePrefix = 'wiki:') {
      let scope = scopePrefix;
      if (selectElement.val() != null) {
        scope += selectElement.val();
      }
      const pageElement = $(`input[name="${pageInputName}"]`);
      if (pageElement.length) {
        const updatedPageElement = this.removeSelectize(pageElement);
        updatedPageElement.suggestPages({
          maxItems: 1,
          searchScope: scope,
        });
      }
    },
    initializeUsers: function (selectElement, userInputName, targetUserScope = 'LOCAL') {
      var userElement = $(`input[name="${userInputName}"]`);
      if (userElement.length) {
        userElement = this.removeSelectize(userElement);
        userElement.suggestWikiUsers({
          maxItems: 1,
          wikiId: selectElement.val(),
          userScope: targetUserScope
        });
      }
    }
  };

  return sharedPickers;
});