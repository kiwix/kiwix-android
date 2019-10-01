/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.webserver;

import android.util.Log;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.base.BasePresenter;
import org.kiwix.kiwixmobile.core.data.DataSource;
import org.kiwix.kiwixmobile.core.di.ActivityScope;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

@ActivityScope
class ZimHostPresenter extends BasePresenter<ZimHostContract.View>
  implements ZimHostContract.Presenter {

  private static final String TAG = "ZimHostPresenter";
  private final DataSource dataSource;

  @Inject ZimHostPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void loadBooks() {
    dataSource.getLanguageCategorizedBooks()
      .subscribe(new SingleObserver<List<BooksOnDiskListItem>>() {
        @Override
        public void onSubscribe(Disposable d) {
          compositeDisposable.add(d);
        }

        @Override
        public void onSuccess(List<BooksOnDiskListItem> books) {
          view.addBooks(books);
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to load books", e);
        }
      });
  }
}
