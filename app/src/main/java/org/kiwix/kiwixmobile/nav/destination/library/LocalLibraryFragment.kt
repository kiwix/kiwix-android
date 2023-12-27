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
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
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
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.coreMainActivity
import org.kiwix.kiwixmobile.core.extensions.setBottomMarginToFragmentContainerView
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KIWIX_APK_WEBSITE_URL
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener.Companion.SCROLL_DOWN
import org.kiwix.kiwixmobile.core.utils.SimpleRecyclerViewScrollListener.Companion.SCROLL_UP
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.databinding.FragmentDestinationLibraryBinding
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import javax.inject.Inject

private const val WAS_IN_ACTION_MODE = "WAS_IN_ACTION_MODE"
private const val MATERIAL_BOTTOM_VIEW_ENTER_ANIMATION_DURATION = 225L

class LocalLibraryFragment : BaseFragment() {

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var dialogShower: DialogShower

  private var actionMode: ActionMode? = null
  private val disposable = CompositeDisposable()
  private var fragmentDestinationLibraryBinding: FragmentDestinationLibraryBinding? = null
  private var permissionDeniedLayoutShowing = false

  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
      val isGranted =
        permissionResult.entries.all(
          Map.Entry<String, @kotlin.jvm.JvmSuppressWildcards Boolean>::value
        )
      if (readStorageHasBeenPermanentlyDenied(isGranted)) {
        fragmentDestinationLibraryBinding?.apply {
          permissionDeniedLayoutShowing = true
          fileManagementNoFiles.visibility = VISIBLE
          goToDownloadsButtonNoFiles.visibility = VISIBLE
          fileManagementNoFiles.text =
            requireActivity().resources.getString(R.string.grant_read_storage_permission)
          goToDownloadsButtonNoFiles.text =
            requireActivity().resources.getString(R.string.go_to_settings_label)
          zimfilelist.visibility = GONE
        }
      } else if (isGranted) {
        permissionDeniedLayoutShowing = false
      }
    }

  private val bookDelegate: BookOnDiskDelegate.BookDelegate by lazy {
    BookOnDiskDelegate.BookDelegate(
      sharedPreferenceUtil,
      {
        if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
          showManageExternalStoragePermissionDialog()
        } else {
          offerAction(RequestNavigateTo(it))
        }
      },
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
    setupMenu()

    return fragmentDestinationLibraryBinding?.root
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.menu_zim_manager, menu)
          val searchItem = menu.findItem(R.id.action_search)
          val languageItem = menu.findItem(R.id.select_language)
          languageItem.isVisible = false
          searchItem.isVisible = false
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          when (menuItem.itemId) {
            R.id.get_zim_nearby_device -> {
              navigateToLocalFileTransferFragment()
              return true
            }
          }
          return false
        }
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setUpSwipeRefreshLayout()
    fragmentDestinationLibraryBinding?.zimfilelist?.run {
      adapter = booksOnDiskAdapter
      layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
      setHasFixedSize(true)
      visibility = GONE
    }
    zimManageViewModel.fileSelectListStates.observe(viewLifecycleOwner, Observer(::render))
      .also {
        coreMainActivity.navHostContainer
          .setBottomMarginToFragmentContainerView(0)

        getBottomNavigationView()?.let {
          setBottomMarginToSwipeRefreshLayout(it.measuredHeight)
        }
      }
    disposable.add(sideEffects())
    disposable.add(fileSelectActions())
    zimManageViewModel.deviceListIsRefreshing.observe(viewLifecycleOwner) {
      fragmentDestinationLibraryBinding?.zimSwiperefresh?.isRefreshing = it!!
    }
    if (savedInstanceState != null && savedInstanceState.getBoolean(WAS_IN_ACTION_MODE)) {
      zimManageViewModel.fileSelectActions.offer(FileSelectActions.RestartActionMode)
    }

    fragmentDestinationLibraryBinding?.goToDownloadsButtonNoFiles?.setOnClickListener {
      if (permissionDeniedLayoutShowing) {
        permissionDeniedLayoutShowing = false
        requireActivity().navigateToAppSettings()
      } else {
        offerAction(FileSelectActions.UserClickedDownloadBooksButton)
      }
    }
    setUpFilePickerButton()

    fragmentDestinationLibraryBinding?.zimfilelist?.addOnScrollListener(
      SimpleRecyclerViewScrollListener { _, newState ->
        when (newState) {
          SCROLL_DOWN -> {
            setBottomMarginToSwipeRefreshLayout(0)
          }
          SCROLL_UP -> {
            getBottomNavigationView()?.let {
              setBottomMarginToSwipeRefreshLayout(it.measuredHeight)
            }
          }
        }
      }
    )
  }

  private fun setUpSwipeRefreshLayout() {
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.setOnRefreshListener {
      if (permissionDeniedLayoutShowing) {
        fragmentDestinationLibraryBinding?.zimSwiperefresh?.isRefreshing = false
      } else {
        if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
          showManageExternalStoragePermissionDialog()
          // Set loading to false since the dialog is currently being displayed.
          // If the user clicks on "No" in the permission dialog,
          // the loading icon remains visible infinitely.
          fragmentDestinationLibraryBinding?.zimSwiperefresh?.isRefreshing = false
        } else {
          requestFileSystemCheck()
        }
      }
    }
  }

  private fun showManageExternalStoragePermissionDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialogShower.show(
        KiwixDialog.ManageExternalFilesPermissionDialog,
        {
          this.activity?.let(FragmentActivity::navigateToSettings)
        }
      )
    }
  }

  private fun getBottomNavigationView() =
    requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_view)

  private fun setBottomMarginToSwipeRefreshLayout(marginBottom: Int) {
    fragmentDestinationLibraryBinding?.zimSwiperefresh?.apply {
      val params = layoutParams as CoordinatorLayout.LayoutParams?
      params?.bottomMargin = marginBottom
      requestLayout()
    }
  }

  private fun setUpFilePickerButton() {
    fragmentDestinationLibraryBinding?.selectFile?.setOnClickListener {
      showFileChooser()
    }
  }

  private fun showFileChooser() {
    val intent = Intent().apply {
      action = Intent.ACTION_OPEN_DOCUMENT
      type = "application/octet-stream"
      addCategory(Intent.CATEGORY_OPENABLE)
    }
    try {
      fileSelectLauncher.launch(Intent.createChooser(intent, "Select a zim file"))
    } catch (ex: ActivityNotFoundException) {
      activity.toast(resources.getString(R.string.no_app_found_to_open), Toast.LENGTH_SHORT)
    }
  }

  private val fileSelectLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == RESULT_OK) {
        result.data?.data?.let {
          // Taking `takePersistableUriPermission` for uris that user try to open via file picker.
          // Since we need access of this uri to open the same file again,
          // if user tries to open notes, history etc.
          requireActivity().contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
          )
          navigateToReaderFragment(it)
        }
      }
    }

  private fun navigateToReaderFragment(uri: Uri) {
    activity?.navigate(
      LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
        .apply { zimFileUri = "$uri" }
    )
  }

  override fun onResume() {
    super.onResume()
    if (sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
      sharedPreferenceUtil.playStoreRestrictionPermissionDialog
    ) {
      showPlayStoreRestrictionInformationToUser()
    } else if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
      !sharedPreferenceUtil.prefIsTest && !permissionDeniedLayoutShowing
    ) {
      checkPermissions()
    } else if (!permissionDeniedLayoutShowing) {
      fragmentDestinationLibraryBinding?.zimfilelist?.visibility = VISIBLE
    }
  }

  private fun showPlayStoreRestrictionInformationToUser() {
    // We should only ask for first time
    sharedPreferenceUtil.playStoreRestrictionPermissionDialog = false
    // Show Dialog to the user to inform about the play store restriction
    dialogShower.show(
      KiwixDialog.PlayStoreRestrictionPopup,
      {},
      ::openKiwixWebsiteForDownloadingApk
    )
  }

  private fun openKiwixWebsiteForDownloadingApk() {
    requireActivity().startActivity(KIWIX_APK_WEBSITE_URL.toUri().browserIntent())
  }

  override fun onDestroyView() {
    super.onDestroyView()
    actionMode = null
    fragmentDestinationLibraryBinding?.zimfilelist?.adapter = null
    fragmentDestinationLibraryBinding = null
    disposable.clear()
    storagePermissionLauncher?.unregister()
    storagePermissionLauncher = null
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

  private fun fileSelectActions() = zimManageViewModel.fileSelectActions
    .observeOn(AndroidSchedulers.mainThread())
    .filter { it === RequestDeleteMultiSelection }
    .subscribe(
      {
        animateBottomViewToOrigin()
      },
      Throwable::printStackTrace
    )

  private fun animateBottomViewToOrigin() {
    getBottomNavigationView().animate()
      .translationY(0F)
      .setDuration(MATERIAL_BOTTOM_VIEW_ENTER_ANIMATION_DURATION)
      .start()
  }

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
        fileManagementNoFiles.text = requireActivity().resources.getString(R.string.no_files_here)
        goToDownloadsButtonNoFiles.text =
          requireActivity().resources.getString(R.string.download_books)

        fileManagementNoFiles.visibility = View.VISIBLE
        goToDownloadsButtonNoFiles.visibility = View.VISIBLE
        zimfilelist.visibility = View.GONE
      } else {
        fileManagementNoFiles.visibility = View.GONE
        goToDownloadsButtonNoFiles.visibility = View.GONE
        zimfilelist.visibility = View.VISIBLE
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(WAS_IN_ACTION_MODE, actionMode != null)
  }

  private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        context.toast(R.string.request_storage)
        storagePermissionLauncher?.launch(
          arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
          )
        )
      } else {
        checkManageExternalStoragePermission()
      }
    } else {
      checkManageExternalStoragePermission()
    }
  }

  private fun checkManageExternalStoragePermission() {
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
            showManageExternalStoragePermissionDialog()
          }
        }
      } else {
        requestFileSystemCheck()
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

  private fun shouldShowRationalePermission() =
    ActivityCompat.shouldShowRequestPermissionRationale(
      requireActivity(),
      Manifest.permission.READ_EXTERNAL_STORAGE
    )

  private fun readStorageHasBeenPermanentlyDenied(isPermissionGranted: Boolean) =
    !isPermissionGranted &&
      !shouldShowRationalePermission()
}
