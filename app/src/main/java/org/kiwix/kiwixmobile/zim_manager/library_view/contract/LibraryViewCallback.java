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
package org.kiwix.kiwixmobile.zim_manager.library_view.contract;

import org.kiwix.kiwixmobile.base.contract.ViewCallback;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import java.util.LinkedList;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public interface LibraryViewCallback extends ViewCallback {

  void showBooks(LinkedList<Book> books);

  void displayNoNetworkConnection();

  void displayNoItemsFound();

  void displayNoItemsAvailable();

  void displayScanningContent();

  void stopScanningContent();

  void downloadFile(LibraryNetworkEntity.Book book);
}
