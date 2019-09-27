/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.zim_manager.fileselect_view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.zim_list.file_management_no_files
import kotlinx.android.synthetic.main.zim_list.zim_swiperefresh
import kotlinx.android.synthetic.main.zim_list.zimfilelist
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.base.BaseFragment
import org.kiwix.kiwixmobile.di.components.ActivityComponent
import org.kiwix.kiwixmobile.extensions.toast
import org.kiwix.kiwixmobile.extensions.viewModel
import org.kiwix.kiwixmobile.utils.Constants.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.utils.LanguageUtils
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestOpen
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate.BookDelegate
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate.LanguageDelegate
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import javax.inject.Inject

class ZimFileSelectFragment : BaseFragment() {

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  private var actionMode: ActionMode? = null
  private val disposable = CompositeDisposable()

  private val zimManageViewModel by lazy {
    activity!!.viewModel<ZimManageViewModel>(viewModelFactory)
  }
  private val bookDelegate: BookDelegate by lazy {
    BookDelegate(sharedPreferenceUtil,
      { offerAction(RequestOpen(it)) },
      { offerAction(RequestMultiSelection(it)) },
      { offerAction(RequestSelect(it)) })
  }

  private val booksOnDiskAdapter: BooksOnDiskAdapter by lazy {
    BooksOnDiskAdapter(bookDelegate, LanguageDelegate)
  }

  override fun inject(activityComponent: ActivityComponent) {
    activityComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    LanguageUtils(activity!!).changeFont(activity!!.layoutInflater, sharedPreferenceUtil)
    return inflater.inflate(R.layout.zim_list, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    zim_swiperefresh.setOnRefreshListener(::requestFileSystemCheck)
    zimfilelist.run {
      adapter = booksOnDiskAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.fileSelectListStates.observe(this, Observer(::render))
    disposable.add(sideEffects())
    zimManageViewModel.deviceListIsRefreshing.observe(this, Observer {
      zim_swiperefresh.isRefreshing = it!!
    })
  }

  private fun sideEffects() = zimManageViewModel.sideEffects.subscribe(
    {
      val effectResult = it.invokeWith(activity!!)
      if (effectResult is ActionMode) {
        actionMode = effectResult
      }
    }, Throwable::printStackTrace
  )

  private fun render(state: FileSelectListState) {
    val items = state.bookOnDiskListItems
    bookDelegate.selectionMode = state.selectionMode
    booksOnDiskAdapter.items = items
    actionMode?.title = String.format("%d", state.selectedBooks.size)
    file_management_no_files.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
  }

  override fun onResume() {
    super.onResume()
    checkPermissions()
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable.clear()
  }

  private fun offerAction(
    action: FileSelectActions
  ) {
    zimManageViewModel.fileSelectActions.offer(action)
  }

  private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(
        activity!!,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      context.toast(R.string.request_storage)
      requestPermissions(
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        REQUEST_STORAGE_PERMISSION
      )
    } else {
      requestFileSystemCheck()
    }
  }

  private fun requestFileSystemCheck() {
    zimManageViewModel.requestFileSystemCheck.onNext(Unit)
  }
}
