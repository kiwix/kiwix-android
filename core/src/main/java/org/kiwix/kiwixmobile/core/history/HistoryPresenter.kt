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
package org.kiwix.kiwixmobile.core.history

import android.util.Log
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.qualifiers.Computation
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.core.history.HistoryContract.Presenter
import org.kiwix.kiwixmobile.core.history.HistoryContract.View
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import java.util.Locale
import javax.inject.Inject

internal class HistoryPresenter @Inject constructor(
  private val dataSource: DataSource,
  @param:MainThread private val mainThread: Scheduler,
  @param:Computation private val computation: Scheduler
) : BasePresenter<View>(), Presenter {

  private var disposable: Disposable? = null

  override fun loadHistory(showHistoryCurrentBook: Boolean) {
    val d = dataSource.getDateCategorizedHistory(showHistoryCurrentBook).subscribe(
        { histories: List<HistoryListItem> -> view?.updateHistoryList(histories) },
        { e: Throwable -> Log.e("HistoryPresenter", "Failed to load history.", e) }
    )
    disposable?.takeIf { !it.isDisposed }?.dispose()
    disposable = d
    compositeDisposable.add(d)
  }

  override fun filterHistory(historyList: List<HistoryListItem>, newText: String) {
    compositeDisposable.add(Observable.just(historyList)
      .flatMapIterable { flatHistoryList: List<HistoryListItem> ->
        flatHistoryList.filter { item ->
          item is HistoryItem &&
            item.historyTitle.toLowerCase(Locale.getDefault())
              .contains(newText.toLowerCase(Locale.getDefault()))
        }
      }
      .toList()
      .subscribeOn(computation)
      .observeOn(mainThread)
      .subscribe(
        { hList: List<HistoryListItem> -> view?.notifyHistoryListFiltered(hList) },
        { e: Throwable -> Log.e("HistoryPresenter", "Failed to filter history", e) }
      ))
  }

  override fun deleteHistory(deleteList: List<HistoryListItem>) {
    dataSource.deleteHistory(deleteList).subscribe({}, { e: Throwable ->
        Log.e("HistoryPresenter", "Failed to delete history", e)
      })
  }
}
