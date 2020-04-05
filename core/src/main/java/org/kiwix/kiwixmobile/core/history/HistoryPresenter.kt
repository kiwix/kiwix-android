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
import io.reactivex.CompletableObserver
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.SingleObserver
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.base.BasePresenter
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.qualifiers.Computation
import org.kiwix.kiwixmobile.core.di.qualifiers.MainThread
import org.kiwix.kiwixmobile.core.history.HistoryContract.Presenter
import org.kiwix.kiwixmobile.core.history.HistoryContract.View
import org.kiwix.kiwixmobile.core.history.HistoryListItem.HistoryItem
import javax.inject.Inject

internal class HistoryPresenter @Inject constructor(
  private val dataSource: DataSource,
  @param:MainThread private val mainThread: Scheduler,
  @param:Computation private val computation: Scheduler
) : BasePresenter<View>(), Presenter {
  private var disposable: Disposable? = null

  override fun loadHistory(showHistoryCurrentBook: Boolean) {
    dataSource.getDateCategorizedHistory(showHistoryCurrentBook)
      .subscribe(object : SingleObserver<List<HistoryListItem>> {
        override fun onSubscribe(d: Disposable) {
          if (disposable != null && !disposable!!.isDisposed) {
            disposable!!.dispose()
          }
          disposable = d
          compositeDisposable.add(d)
        }

        override fun onSuccess(histories: List<HistoryListItem>) {
          view?.updateHistoryList(histories)
        }

        override fun onError(e: Throwable) {
          Log.e("HistoryPresenter", e.toString())
        }
      })
  }

  override fun filterHistory(
    historyList: List<HistoryListItem>,
    newText: String
  ) = Observable.just(historyList)
    .flatMapIterable { flatHistoryList: List<HistoryListItem> ->
      flatHistoryList.filter { item ->
        item is HistoryItem
          && item.historyTitle.toLowerCase()
          .contains(newText.toLowerCase())
      }
    }
    .toList()
    .subscribeOn(computation)
    .observeOn(mainThread)
    .subscribe(object : SingleObserver<List<HistoryListItem>> {
      override fun onSubscribe(d: Disposable) {
        compositeDisposable.add(d)
      }

      override fun onError(e: Throwable) {
        Log.e("HistoryPresenter", e.toString())
      }

      override fun onSuccess(historyList: List<HistoryListItem>) {
        view?.notifyHistoryListFiltered(historyList)
      }
    })

  override fun deleteHistory(deleteList: List<HistoryListItem>) =
    dataSource.deleteHistory(deleteList)
      .subscribe(object : CompletableObserver {
        override fun onSubscribe(d: Disposable) {}
        override fun onComplete() {}
        override fun onError(e: Throwable) {
          Log.e("HistoryPresenter", e.toString())
        }
      })
}
