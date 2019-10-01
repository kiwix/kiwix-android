/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.core.main;

import android.content.Intent;
import android.view.View;

public interface WebViewCallback {
  void webViewUrlLoading();

  void webViewUrlFinishedLoading();

  void webViewFailedLoading(String failingUrl);

  void showHomePage();

  void openExternalUrl(Intent intent);

  void manageZimFiles(int tab);

  void webViewProgressChanged(int progress);

  void webViewTitleUpdated(String title);

  void webViewPageChanged(int page, int maxPages);

  void webViewLongClick(String url);

  void setHomePage(View view);
}
