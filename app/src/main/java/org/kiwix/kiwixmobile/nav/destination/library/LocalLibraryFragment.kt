/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.utils.FILE_SELECT_CODE
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.databinding.FragmentDestinationLibraryBinding
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import java.io.File
import javax.inject.Inject

private const val WAS_IN_ACTION_MODE = "WAS_IN_ACTION_MODE"

class LocalLibraryFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var dialogShower: DialogShower

  private var actionMode: ActionMode? = null
  private val disposable = CompositeDisposable()
  private var fragmentDestinationLibraryBinding: FragmentDestinationLibraryBinding? = null

  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val bookDelegate: BookOnDiskDelegate.BookDelegate by lazy {
    BookOnDiskDelegate.BookDelegate(
      sharedPreferenceUtil,
      { offerAction(RequestNavigateTo(it)) },
      { offerAction(RequestMultiSelection(it)) },
      { offerAction(RequestSelect(it)) }
    )
  }
  private val booksOnDiskAdapter: BooksOnDiskAdapter by lazy {
    BooksOnDiskAdapter(bookDelegate, BookOnDiskDelegate.LanguageDelegate)
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    checkStoragePermission()
    LanguageUtils(requireActivity())
      .changeFont(requireActivity(), sharedPreferenceUtil)
    fragmentDestinationLibraryBinding = FragmentDestinationLibraryBinding.inflate(
      inflater,
      container,
      false
    )
    val toolbar = fragmentDestinationLibraryBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
    val activity = activity as CoreMainActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setTitle(R.string.library)
    }
    if (toolbar != null) {
      activity.setupDrawerToggle(toolbar)
    }
    setHasOptionsMenu(true)

    return fragmentDestinationLibraryBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.setOnRefreshListener(
      ::requestFileSystemCheck
    )
    fragmentDestinationLibraryBinding?.zimfilelist?.run {
      adapter = booksOnDiskAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
    }
    zimManageViewModel.fileSelectListStates.observe(viewLifecycleOwner, Observer(::render))
      .also {
        coreMainActivity.navHostContainer
          .setBottomMarginToFragmentContainerView(0)

        val bottomNavigationView =
          requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)
        bottomNavigationView?.let {
          setBottomMarginToSwipeRefreshLayout(bottomNavigationView.measuredHeight)
        }
      }
    disposable.add(sideEffects())
    zimManageViewModel.deviceListIsRefreshing.observe(viewLifecycleOwner) {
      fragmentDestinationLibraryBinding?.zimSwiperefresh?.isRefreshing = it!!
    }
    if (savedInstanceState != null && savedInstanceState.getBoolean(WAS_IN_ACTION_MODE)) {
      zimManageViewModel.fileSelectActions.offer(FileSelectActions.RestartActionMode)
    }

    fragmentDestinationLibraryBinding?.goToDownloadsButtonNoFiles?.setOnClickListener {
      offerAction(FileSelectActions.UserClickedDownloadBooksButton)
    }
    hideFilePickerButton()
  }

  private fun setBottomMarginToSwipeRefreshLayout(marginBottom: Int) {
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.apply {
      val params = layoutParams as CoordinatorLayout.LayoutParams?
      params?.bottomMargin = marginBottom
      requestLayout()
    }
  }

  private fun hideFilePickerButton() {
    if (sharedPreferenceUtil.isPlayStoreBuild) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        fragmentDestinationLibraryBinding?.selectFile?.visibility = View.GONE
      }
    }

    fragmentDestinationLibraryBinding?.selectFile?.setOnClickListener {
      showFileChooser()
    }
  }

  private fun showFileChooser() {
    val intent = Intent().apply {
      action = Intent.ACTION_GET_CONTENT
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)
    }
    try {
      startActivityForResult(
        Intent.createChooser(intent, "Select a zim file"),
        FILE_SELECT_CODE
      )
    } catch (ex: ActivityNotFoundException) {
      activity.toast(resources.getString(R.string.no_app_found_to_open), Toast.LENGTH_SHORT)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      FILE_SELECT_CODE -> {
        data?.data?.let { uri ->
          getZimFileFromUri(uri)?.let(::navigateToReaderFragment)
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun getZimFileFromUri(
    uri: Uri
  ): File? {
    val filePath = FileUtils.getLocalFilePathByUri(
      requireActivity().applicationContext, uri
    )
    if (filePath == null || !File(filePath).exists()) {
      activity.toast(R.string.error_file_not_found)
      return null
    }
    val file = File(filePath)
    return if (!FileUtils.isValidZimFile(file.path)) {
      activity.toast(R.string.error_file_invalid)
      null
    } else {
      file
    }
  }

  private fun navigateToReaderFragment(file: File) {
    if (!file.canRead()) {
      activity.toast(R.string.unable_to_read_zim_file)
    } else {
      activity?.navigate(
        LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = file.toUri().toString() }
      )
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.menu_zim_manager, menu)
    val searchItem = menu.findItem(R.id.action_search)
    val languageItem = menu.findItem(R.id.select_language)
    languageItem.isVisible = false
    searchItem.isVisible = false
    super.onCreateOptionsMenu(menu, inflater)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.get_zim_nearby_device -> navigateToLocalFileTransferFragment()
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    actionMode = null
    fragmentDestinationLibraryBinding?.zimfilelist?.adapter = null
    fragmentDestinationLibraryBinding = null
    disposable.clear()
  }

  private fun sideEffects() = zimManageViewModel.sideEffects
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
      {
        val effectResult = it.invokeWith(requireActivity() as AppCompatActivity)
        if (effectResult is ActionMode) {
          actionMode = effectResult
        }
      }, Throwable::printStackTrace
    )

  private fun render(state: FileSelectListState) {
    val items: List<BooksOnDiskListItem> = state.bookOnDiskListItems
    bookDelegate.selectionMode = state.selectionMode
    booksOnDiskAdapter.items = items
    if (items.none(BooksOnDiskListItem::isSelected)) {
      actionMode?.finish()
      actionMode = null
    }
    actionMode?.title = String.format("%d", state.selectedBooks.size)
    fragmentDestinationLibraryBinding?.apply {
      if (items.isEmpty()) {
        fileManagementNoFiles.visibility = View.VISIBLE
        goToDownloadsButtonNoFiles.visibility = View.VISIBLE
      } else {
        fileManagementNoFiles.visibility = View.GONE
        goToDownloadsButtonNoFiles.visibility = View.GONE
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(WAS_IN_ACTION_MODE, actionMode != null)
  }

  @Suppress("NestedBlockDepth")
  private fun checkStoragePermission() {
    if (ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
      == PackageManager.PERMISSION_GRANTED
    ) {
      // Permission is granted
      if (sharedPreferenceUtil.isPlayStoreBuild) {
        requestFileSystemCheck()
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          if (Environment.isExternalStorageManager()) {
            // We already have permission!!
            requestFileSystemCheck()
          } else {
            if (sharedPreferenceUtil.manageExternalFilesPermissionDialog) {
              // We should only ask for first time, If the users wants to revoke settings
              // then they can directly toggle this feature from settings screen
              sharedPreferenceUtil.manageExternalFilesPermissionDialog = false
              // Show Dialog and  Go to settings to give permission
              dialogShower.show(
                KiwixDialog.ManageExternalFilesPermissionDialog,
                {
                  this.activity?.let(FragmentActivity::navigateToSettings)
                }
              )
            }
          }
        } else {
          requestFileSystemCheck()
        }
      }
    } else {
      // Permission is not granted
      // Request permission
      context.toast(R.string.request_storage)
      ActivityCompat.requestPermissions(
        requireActivity(),
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        REQUEST_STORAGE_PERMISSION
      )
    }
  }

  private fun showAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", requireActivity().packageName, null)
    startActivity(intent)
  }

  @Suppress("NestedBlockDepth")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_STORAGE_PERMISSION -> {
        // If request is cancelled, the result arrays are empty.
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          // Permission is granted
          if (sharedPreferenceUtil.isPlayStoreBuild) {
            requestFileSystemCheck()
          } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
              if (Environment.isExternalStorageManager()) {
                // We already have permission!!
                requestFileSystemCheck()
              } else {
                if (sharedPreferenceUtil.manageExternalFilesPermissionDialog) {
                  // We should only ask for first time, If the users wants to revoke settings
                  // then they can directly toggle this feature from settings screen
                  sharedPreferenceUtil.manageExternalFilesPermissionDialog = false
                  // Show Dialog and  Go to settings to give permission
                  dialogShower.show(
                    KiwixDialog.ManageExternalFilesPermissionDialog,
                    {
                      this.activity?.let(FragmentActivity::navigateToSettings)
                    }
                  )
                }
              }
            } else {
              requestFileSystemCheck()
            }
          }
        } else {
          // Permission is not granted
          // Show a message or exit the app
          requireActivity().finish()
          showAppSettings()
        }
        return
      }
    }
  }

  private fun requestFileSystemCheck() {
    zimManageViewModel.requestFileSystemCheck.onNext(Unit)
  }

  private fun offerAction(action: FileSelectActions) {
    zimManageViewModel.fileSelectActions.offer(action)
  }

  private fun navigateToLocalFileTransferFragment() {
    requireActivity().navigate(R.id.localFileTransferFragment)
  }
}
