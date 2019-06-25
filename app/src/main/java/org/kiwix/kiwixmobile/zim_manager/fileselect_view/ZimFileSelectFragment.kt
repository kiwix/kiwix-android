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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.zim_list.file_management_no_files
import kotlinx.android.synthetic.main.zim_list.zim_swiperefresh
import kotlinx.android.synthetic.main.zim_list.zimfilelist
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.base.BaseFragment
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.di.components.ActivityComponent
import org.kiwix.kiwixmobile.extensions.toast
import org.kiwix.kiwixmobile.utils.BookUtils
import org.kiwix.kiwixmobile.utils.Constants.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.utils.DialogShower
import org.kiwix.kiwixmobile.utils.KiwixDialog.DeleteZim
import org.kiwix.kiwixmobile.utils.LanguageUtils
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.utils.files.FileUtils
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate.BookDelegate
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate.LanguageDelegate
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class ZimFileSelectFragment : BaseFragment() {

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var bookDao: NewBookDao
  @Inject lateinit var dialogShower: DialogShower
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var bookUtils: BookUtils

  private val zimManageViewModel: ZimManageViewModel by lazy {
    ViewModelProviders.of(activity!!, viewModelFactory)
        .get(ZimManageViewModel::class.java)
  }

  private val booksOnDiskAdapter: BooksOnDiskAdapter by lazy {
    BooksOnDiskAdapter(
        BookDelegate(sharedPreferenceUtil, this::openOnClick, this::deleteOnLongClick),
        LanguageDelegate
    )
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
    zim_swiperefresh.setOnRefreshListener(this::requestFileSystemCheck)
    zimfilelist.run {
      adapter = booksOnDiskAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.bookItems.observe(this, Observer {
      booksOnDiskAdapter.itemList = it!!
      checkEmpty(it)
    })
    zimManageViewModel.deviceListIsRefreshing.observe(this, Observer {
      zim_swiperefresh.isRefreshing = it!!
    })
  }

  override fun onResume() {
    super.onResume()
    checkPermissions()
  }

  private fun checkEmpty(books: List<Any>) {
    file_management_no_files.visibility =
      if (books.isEmpty()) View.VISIBLE
      else View.GONE
  }

  private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT > 18
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

  private fun openOnClick(it: BookOnDisk) {
    val file = it.file
    ZimContentProvider.canIterate = false
    if (!file.canRead()) {
      context.toast(string.error_filenotfound)
    } else {
      (activity as ZimManageActivity).finishResult(file.path)
    }
  }

  private fun deleteOnLongClick(it: BookOnDisk) {
    dialogShower.show(DeleteZim, {
      if (deleteSpecificZimFile(it)) {
        context.toast(string.delete_specific_zim_toast)
      } else {
        context.toast(string.delete_zim_failed)
      }
    })
  }

  private fun deleteSpecificZimFile(book: BookOnDisk): Boolean {
    val file = book.file
    FileUtils.deleteZimFile(file.path)
    if (file.exists()) {
      return false
    }
    bookDao.delete(book.databaseId!!)
    return true
  }
}
