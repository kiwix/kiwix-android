/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResultOnCurrent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.zimManager.MAX_PROGRESS
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.libkiwix.Book
import java.io.File
import java.util.Locale
import javax.inject.Inject

private const val WAS_IN_ACTION_MODE = "WAS_IN_ACTION_MODE"
const val LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG = "localFileTransferMenuButtonTestingTag"
private const val SHOW_SCAN_DIALOG_DELAY = 2000L

@Suppress("LargeClass")
class LocalLibraryFragment : BaseFragment(), SelectedZimFileCallback {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Inject lateinit var kiwixDataStore: KiwixDataStore

  @Inject lateinit var dialogShower: DialogShower

  @Inject lateinit var mainRepositoryActions: MainRepositoryActions

  @Inject lateinit var zimReaderFactory: ZimFileReader.Factory

  @Inject lateinit var storageCalculator: StorageCalculator

  @JvmField
  @Inject
  var processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone? = null

  @JvmField
  @Inject
  var processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore? = null

  private var actionMode: ActionMode? = null
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  private var permissionDeniedLayoutShowing = false
  private val selectedZimFileUriList: MutableList<Uri> = mutableListOf()

  /**
   * Manages the scanning of storage on firs app launch
   * and the necessary permission are not granted.
   */
  private var shouldScanFileSystem = false
  private val libraryScreenState = mutableStateOf(
    LocalLibraryScreenState(
      fileSelectListState = FileSelectListState(emptyList()),
      snackBarHostState = SnackbarHostState(),
      swipeRefreshItem = Pair(false, true),
      scanningProgressItem = Pair(false, ZERO),
      noFilesViewItem = Triple("", "", false),
      actionMenuItems = listOf()
    )
  )

  private val zimManageViewModel by lazy {
    requireActivity().viewModel<ZimManageViewModel>(viewModelFactory)
  }

  private val validateZimViewModel by lazy {
    requireActivity().viewModel<ValidateZimViewModel>(viewModelFactory)
  }

  private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
      val isGranted = permissionResult.values.all { it }
      val isPermanentlyDenied = readStorageHasBeenPermanentlyDenied(isGranted)
      permissionDeniedLayoutShowing = isPermanentlyDenied
      if (permissionDeniedLayoutShowing) {
        shouldScanFileSystem = false
        updateLibraryScreenState(
          noFilesViewItem = Triple(
            requireActivity().resources.getString(string.grant_read_storage_permission),
            requireActivity().resources.getString(string.go_to_settings_label),
            true
          )
        )
      } else if (shouldScanFileSystem) {
        runCatching { lifecycleScope.launch { scanFileSystem() } }
      }
    }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    safelyChangeFont()
    return ComposeView(requireContext()).apply {
      setContent {
        val lazyListState = rememberLazyListState()
        LaunchedEffect(Unit) {
          updateLibraryScreenState(actionMenuItems = actionMenuItems())
        }
        LocalLibraryScreen(
          listState = lazyListState,
          state = libraryScreenState.value,
          fabButtonClick = { filePickerButtonClick() },
          onClick = { onBookItemClick(it) },
          onLongClick = { onBookItemLongClick(it) },
          onMultiSelect = { offerAction(RequestSelect(it)) },
          onRefresh = { onSwipeRefresh() },
          onDownloadButtonClick = { downloadBookButtonClick() },
          bottomAppBarScrollBehaviour = (requireActivity() as CoreMainActivity).bottomAppBarScrollBehaviour,
          navHostController = (requireActivity() as CoreMainActivity).navController,
          onUserBackPressed = { onUserBackPressed() }
        ) {
          NavigationIcon(
            iconItem = IconItem.Vector(Icons.Filled.Menu),
            contentDescription = string.open_drawer,
            onClick = { navigationIconClick() }
          )
        }
        DialogHost(dialogShower as AlertDialogShower)
        DisposableEffect(Unit) {
          onDispose {
            // Dispose UI resources when this Compose view is removed. Compose disposes
            // its content before Fragment.onDestroyView(), so callback and listener cleanup
            // should happen here.
            destroyViews()
          }
        }
      }
    }
  }

  private fun safelyChangeFont() {
    runCatching {
      lifecycleScope.launch {
        LanguageUtils(requireActivity())
          .changeFont(requireActivity(), kiwixDataStore)
      }
    }
  }

  private fun onUserBackPressed(): FragmentActivityExtensions.Super {
    val coreMainActivity = activity as? CoreMainActivity
    if (coreMainActivity?.navigationDrawerIsOpen() == true) {
      coreMainActivity.closeNavigationDrawer()
      return FragmentActivityExtensions.Super.ShouldNotCall
    }
    return FragmentActivityExtensions.Super.ShouldCall
  }

  private fun navigationIconClick() {
    val activity = activity as CoreMainActivity
    if (activity.navigationDrawerIsOpen()) {
      activity.closeNavigationDrawer()
    } else {
      activity.openNavigationDrawer()
    }
  }

  private fun actionMenuItems() = listOf(
    ActionMenuItem(
      IconItem.Drawable(R.drawable.ic_baseline_mobile_screen_share_24px),
      string.get_content_from_nearby_device,
      { navigateToLocalFileTransferFragment() },
      isEnabled = true,
      testingTag = LOCAL_FILE_TRANSFER_MENU_BUTTON_TESTING_TAG
    )
  )

  private fun onBookItemClick(bookOnDisk: BookOnDisk) {
    if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
      showManageExternalStoragePermissionDialog()
    } else {
      offerAction(RequestNavigateTo(bookOnDisk))
    }
  }

  private fun onBookItemLongClick(bookOnDisk: BookOnDisk) {
    if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
      showManageExternalStoragePermissionDialog()
    } else {
      offerAction(RequestMultiSelection(bookOnDisk))
    }
  }

  private fun updateLibraryScreenState(
    fileSelectListState: FileSelectListState? = null,
    snackBarHostState: SnackbarHostState? = null,
    swipeRefreshItem: Pair<Boolean, Boolean>? = null,
    scanningProgressItem: Pair<Boolean, Int>? = null,
    noFilesViewItem: Triple<String, String, Boolean>? = null,
    actionMenuItems: List<ActionMenuItem>? = null
  ) {
    libraryScreenState.value = libraryScreenState.value.copy(
      fileSelectListState = fileSelectListState ?: libraryScreenState.value.fileSelectListState,
      snackBarHostState = snackBarHostState ?: libraryScreenState.value.snackBarHostState,
      swipeRefreshItem = swipeRefreshItem ?: libraryScreenState.value.swipeRefreshItem,
      scanningProgressItem = scanningProgressItem
        ?: libraryScreenState.value.scanningProgressItem,
      noFilesViewItem = noFilesViewItem ?: libraryScreenState.value.noFilesViewItem,
      actionMenuItems = actionMenuItems ?: libraryScreenState.value.actionMenuItems
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    processSelectedZimFilesForStandalone?.setSelectedZimFileCallback(this)
    processSelectedZimFilesForPlayStore?.init(
      lifecycleScope = lifecycleScope,
      alertDialogShower = dialogShower as AlertDialogShower,
      snackBarHostState = libraryScreenState.value.snackBarHostState,
      fragmentManager = parentFragmentManager,
      selectedZimFileCallback = this@LocalLibraryFragment
    )
    zimManageViewModel.setAlertDialogShower(dialogShower as AlertDialogShower)
    zimManageViewModel.setValidateZimViewModel(validateZimViewModel)
    zimManageViewModel.fileSelectListStates.observe(viewLifecycleOwner, Observer(::render))
    coroutineJobs.apply {
      add(sideEffects())
      add(fileSelectActions())
    }
    zimManageViewModel.deviceListScanningProgress.observe(viewLifecycleOwner) {
      updateLibraryScreenState(
        // hide this progress bar when scanning is complete.
        scanningProgressItem = Pair(it != MAX_PROGRESS, it),
        // enable if the previous scanning is completes.
        swipeRefreshItem = Pair(false, it == MAX_PROGRESS)
      )
    }
    if (savedInstanceState != null && savedInstanceState.getBoolean(WAS_IN_ACTION_MODE)) {
      offerAction(FileSelectActions.RestartActionMode)
    }
    showCopyMoveDialogForOpenedZimFileFromStorage()
  }

  private fun downloadBookButtonClick() {
    if (permissionDeniedLayoutShowing) {
      permissionDeniedLayoutShowing = false
      requireActivity().navigateToAppSettings()
    } else {
      offerAction(FileSelectActions.UserClickedDownloadBooksButton)
    }
  }

  private fun showCopyMoveDialogForOpenedZimFileFromStorage() {
    val zimFileUri = arguments?.getString(ZIM_FILE_URI_KEY).orEmpty()
    if (zimFileUri.isNotEmpty()) {
      selectedZimFileUriList.clear()
      selectedZimFileUriList.add(zimFileUri.toUri())
      if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
        showManageExternalStoragePermissionDialog()
      } else if (requestExternalStorageWritePermission()) {
        handleSelectedFileUri(listOf(zimFileUri.toUri()))
      }
    }
    requireArguments().clear()
  }

  private fun onSwipeRefresh() {
    if (permissionDeniedLayoutShowing) {
      // When permission denied layout is showing hide the "Swipe refresh".
      updateLibraryScreenState(swipeRefreshItem = false to true)
    } else {
      if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
        showManageExternalStoragePermissionDialog()
        // Set loading to false since the dialog is currently being displayed.
        // If the user clicks on "No" in the permission dialog,
        // the loading icon remains visible infinitely.
        updateLibraryScreenState(swipeRefreshItem = false to true)
      } else {
        // hide the swipe refreshing because now we are showing the ContentLoadingProgressBar
        // to show the progress of how many files are scanned.
        // disable the swipe refresh layout until the ongoing scanning will not complete
        // to avoid multiple scanning.
        updateLibraryScreenState(
          swipeRefreshItem = false to false,
          // Show the progress Bar.
          scanningProgressItem = true to ZERO
        )
        requestFileSystemCheck()
      }
    }
  }

  private fun showManageExternalStoragePermissionDialog() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!isAdded) return
      dialogShower.show(
        KiwixDialog.ManageExternalFilesPermissionDialog,
        {
          this.activity?.let(FragmentActivity::navigateToSettings)
        }
      )
    }
  }

  /**
   * Handles the file picker button click.
   *
   * It asks for all storage permissions before opening the file picker so that we do not need to
   * ask for permission later.
   */
  private fun filePickerButtonClick() {
    if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
      showManageExternalStoragePermissionDialog()
    } else if (requestExternalStorageWritePermission()) {
      showFileChooser()
    }
  }

  private fun showFileChooser() {
    val intent = Intent().apply {
      action = Intent.ACTION_OPEN_DOCUMENT
      type = "application/*"
      addCategory(Intent.CATEGORY_OPENABLE)
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      if (sharedPreferenceUtil.prefIsTest) {
        putExtra(
          "android.provider.extra.INITIAL_URI",
          "content://com.android.externalstorage.documents/document/primary:Download".toUri()
        )
      }
    }
    try {
      fileSelectLauncher.launch(Intent.createChooser(intent, "Select a zim file"))
    } catch (_: ActivityNotFoundException) {
      activity.toast(resources.getString(R.string.no_app_found_to_open), Toast.LENGTH_SHORT)
    }
  }

  private val fileSelectLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode != RESULT_OK) return@registerForActivityResult
      val uriList = extractUrisFromIntent(result.data)
      if (uriList.isNotEmpty()) {
        handleSelectedFileUri(uriList)
      }
    }

  /**
   * Extract the uris from result intent and return the uri list.
   */
  private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
    val urisList = arrayListOf<Uri>()
    when {
      intent?.clipData != null -> {
        // Handle multiple files.
        val count: Int = intent.clipData?.itemCount ?: ZERO
        for (i in ZERO..count - ONE) {
          intent.clipData?.getItemAt(i)?.uri?.let {
            takePersistableUriPermission(it)
            urisList.add(it)
          }
        }
      }

      intent?.data != null -> {
        // Handle single file.
        intent.data?.let {
          takePersistableUriPermission(it)
          urisList.add(it)
        }
      }
    }
    return urisList
  }

  private fun takePersistableUriPermission(uri: Uri) {
    runCatching {
      activity?.applicationContext?.contentResolver?.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )
    }.onFailure {
      Log.e(TAG_KIWIX, "Could not take persistable permission for uri = $uri")
    }
  }

  fun handleSelectedFileUri(uris: List<Uri>) {
    lifecycleScope.launch {
      selectedZimFileUriList.clear()
      selectedZimFileUriList.addAll(uris)
      when {
        // Process the ZIM file for standalone app.
        processSelectedZimFilesForStandalone?.canHandleUris() == true ->
          processSelectedZimFilesForStandalone?.processSelectedFiles(uris)

        // Process the ZIM file for PlayStore app.
        processSelectedZimFilesForPlayStore?.canHandleUris() == true ->
          processSelectedZimFilesForPlayStore?.processSelectedFiles(uris)
      }
    }
  }

  override fun showFileCopyMoveErrorDialog(
    errorMessage: String,
    callBack: suspend () -> Unit
  ) {
    dialogShower.show(
      KiwixDialog.FileCopyMoveError(errorMessage),
      {
        lifecycleScope.launch {
          callBack.invoke()
        }
      }
    )
  }

  override fun navigateToReaderFragment(file: File) {
    if (!file.canRead()) {
      activity.toast(string.unable_to_read_zim_file)
    } else {
      // Save the ZIM file to the libkiwix to display it on the local library screen.
      // This is particularly useful when storage is slow or contains a large number of files.
      // In such cases, scanning may take some time to show all the files on the
      // local library screen. Since our application is already aware of this opened ZIM file,
      // we can directly add it to the libkiwix.
      // See https://github.com/kiwix/kiwix-android/issues/3650
      addBookToLibkiwixBookOnDisk(file)
      val navOptions = NavOptions.Builder()
        .setPopUpTo(KiwixDestination.Reader.route, false)
        .build()
      activity?.apply {
        navigate(KiwixDestination.Reader.route, navOptions)
        setNavigationResultOnCurrent(file.toUri().toString(), ZIM_FILE_URI_KEY)
      }
    }
  }

  @Suppress("InjectDispatcher")
  override fun addBookToLibkiwixBookOnDisk(file: File) {
    CoroutineScope(Dispatchers.IO).launch {
      zimReaderFactory.create(ZimReaderSource(file), false)
        ?.let { zimFileReader ->
          val book = Book().apply { update(zimFileReader.jniKiwixReader) }
          mainRepositoryActions.saveBook(book)
          zimFileReader.dispose()
        }
    }
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      when {
        shouldShowFileSystemDialog() -> {
          Handler(Looper.getMainLooper()).postDelayed({
            showFileSystemScanDialog()
          }, SHOW_SCAN_DIALOG_DELAY)
        }

        shouldScanFileSystem -> {
          // When user goes to settings for granting the `MANAGE_EXTERNAL_STORAGE` permission, and
          // came back to the application then initiate the scanning of file system.
          scanFileSystem()
        }

        !sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
          !sharedPreferenceUtil.prefIsTest && !permissionDeniedLayoutShowing -> {
          checkPermissions()
        }

        else -> {
          updateLibraryScreenState(
            noFilesViewItem = Triple(
              requireActivity().resources.getString(string.no_files_here),
              requireActivity().resources.getString(string.download_books),
              false
            )
          )
        }
      }
    }
  }

  // Shows the FileSystemScan dialog.
  private fun showFileSystemScanDialog() {
    // Do not execute the code if fragment is not visible.
    // We are showing this dialog 2 seconds later. In the meantime; user can
    // navigate to other screens.
    if (!isAdded) return
    dialogShower.show(
      KiwixDialog.YesNoDialog.FileSystemScan,
      {
        lifecycleScope.launch {
          // Sets true so that it can not show again.
          kiwixDataStore.setIsScanFileSystemDialogShown(true)
          shouldScanFileSystem = true
          scanFileSystem()
        }
      },
      {
        lifecycleScope.launch {
          // User clicks on the "No" button so not show again.
          kiwixDataStore.setIsScanFileSystemDialogShown(true)
        }
      }
    )
  }

  /**
   * Scan the file system for ZIM files.
   * Checks:
   * 1. If our app has the storage permission. If not, it asks for the permission(if not running in test).
   * 2. Checks if app has the full scan permission. If not, then it asks for the permission.
   * 3. Then finally it scans the storage for ZIM files.
   */
  private suspend fun scanFileSystem() {
    if (!isAdded) return
    when {
      !hasReadExternalStoragePermission() && !kiwixDataStore.isScanFileSystemTest.first() ->
        askForReaderExternalStoragePermission(true)

      !requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil) ->
        showManageExternalStoragePermissionDialog()

      else -> {
        shouldScanFileSystem = false
        requestFileSystemCheck()
      }
    }
  }

  /**
   * Determines whether the file system scan dialog should be shown.
   * Conditions:
   *  1. The scan dialog has not already been shown.
   *  2. This is not the Play Store build (only the non-PS version, e.g. standalone app).
   *  3. If there are no ZIM files showing on the library screen.
   */
  private suspend fun shouldShowFileSystemDialog(): Boolean =
    !kiwixDataStore.isScanFileSystemDialogShown.first() &&
      !BuildConfig.IS_PLAYSTORE &&
      libraryScreenState.value.fileSelectListState.bookOnDiskListItems.isEmpty()

  override fun onDestroyView() {
    super.onDestroyView()
    destroyViews()
  }

  private fun destroyViews() {
    actionMode = null
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    storagePermissionLauncher?.unregister()
    storagePermissionLauncher = null
    processSelectedZimFilesForPlayStore?.dispose()
    processSelectedZimFilesForPlayStore = null
    readStoragePermissionLauncher?.unregister()
    readStoragePermissionLauncher = null
    selectedZimFileUriList.clear()
  }

  private fun sideEffects() =
    lifecycleScope.launch {
      zimManageViewModel.sideEffects
        .collect {
          val effectResult = it.invokeWith(requireActivity() as AppCompatActivity)
          if (effectResult is ActionMode) {
            actionMode = effectResult
            libraryScreenState
              .value
              .fileSelectListState
              .selectedBooks
              .size.let(::setActionModeTitle)
          }
        }
    }

  private fun fileSelectActions() =
    lifecycleScope.launch {
      zimManageViewModel.fileSelectActions
        .filter { it === RequestDeleteMultiSelection }
        .collect {
          animateBottomViewToOrigin()
        }
    }

  private fun animateBottomViewToOrigin() {
    // getBottomNavigationView().animate()
    //   .translationY(0F)
    //   .setDuration(MATERIAL_BOTTOM_VIEW_ENTER_ANIMATION_DURATION)
    //   .start()
  }

  private fun render(state: FileSelectListState) {
    // Force recomposition by first setting an empty list before assigning the updated list.
    // This is necessary because modifying an object's property doesn't trigger recomposition,
    // as Compose still considers the list unchanged.
    updateLibraryScreenState(fileSelectListState = FileSelectListState(emptyList()))
    // Update the real state for UI.
    updateLibraryScreenState(
      fileSelectListState = state,
      noFilesViewItem = Triple(
        requireActivity().resources.getString(string.no_files_here),
        requireActivity().resources.getString(string.download_books),
        // If here are no items available then show the "No files here" text, and "Download books"
        // button so that user can go to "Online library" screen by clicking this button.
        state.bookOnDiskListItems.isEmpty()
      )
    )
    if (state.bookOnDiskListItems.none(BooksOnDiskListItem::isSelected)) {
      actionMode?.finish()
      actionMode = null
    }
    setActionModeTitle(state.selectedBooks.size)
  }

  private fun setActionModeTitle(selectedBookCount: Int) {
    actionMode?.title = String.format(Locale.getDefault(), "%d", selectedBookCount)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(WAS_IN_ACTION_MODE, actionMode != null)
  }

  private fun hasReadExternalStoragePermission(): Boolean =
    ContextCompat.checkSelfPermission(
      requireActivity(),
      Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

  private fun checkPermissions() {
    if (!isAdded) return
    if (!hasReadExternalStoragePermission()) {
      askForReaderExternalStoragePermission()
    } else {
      checkManageExternalStoragePermission()
    }
  }

  private fun askForReaderExternalStoragePermission(shouldScanIfHasPermission: Boolean = false) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      context.toast(string.request_storage)
      storagePermissionLauncher?.launch(
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      )
    } else {
      // Pass the parameter it comes from scan dialog.
      // So ask for the permission or scan if granted.
      // This will help us in scanning on already installed apps.
      checkManageExternalStoragePermission(shouldScanIfHasPermission)
    }
  }

  private fun checkManageExternalStoragePermission(shouldScanIfHasPermission: Boolean = false) {
    if (!sharedPreferenceUtil.isPlayStoreBuild && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        // We do not have the permission!!
        if (sharedPreferenceUtil.manageExternalFilesPermissionDialog || shouldScanIfHasPermission) {
          // We should only ask for first time, If the users wants to revoke settings
          // then they can directly toggle this feature from settings screen
          sharedPreferenceUtil.manageExternalFilesPermissionDialog = false
          // Show Dialog and  Go to settings to give permission
          showManageExternalStoragePermissionDialog()
        }
      } else if (shouldScanIfHasPermission) {
        shouldScanFileSystem = false
        requestFileSystemCheck()
      }
    }
  }

  private fun requestFileSystemCheck(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    CoroutineScope(dispatcher).launch {
      zimManageViewModel.requestFileSystemCheck.emit(Unit)
    }
  }

  private fun offerAction(action: FileSelectActions) {
    lifecycleScope.launch {
      zimManageViewModel.fileSelectActions.emit(action)
    }
  }

  private fun navigateToLocalFileTransferFragment() {
    requireActivity().navigate(KiwixDestination.LocalFileTransfer.route)
  }

  private fun shouldShowRationalePermission() =
    ActivityCompat.shouldShowRequestPermissionRationale(
      requireActivity(),
      Manifest.permission.READ_EXTERNAL_STORAGE
    )

  private fun readStorageHasBeenPermanentlyDenied(isPermissionGranted: Boolean) =
    !isPermissionGranted &&
      !shouldShowRationalePermission()

  private var readStoragePermissionLauncher: ActivityResultLauncher<Array<String>>? =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
      val isGranted =
        permissionResult.entries.all(
          Map.Entry<String, @JvmSuppressWildcards Boolean>::value
        )
      if (isGranted) {
        // handle the selected ZIM files.
        handleSelectedFileUri(selectedZimFileUriList)
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          requireActivity().toast(string.request_storage, Toast.LENGTH_LONG)
        } else {
          dialogShower.show(
            KiwixDialog.ReadPermissionRequired,
            requireActivity()::navigateToAppSettings
          )
        }
      }
    }

  @Suppress("NestedBlockDepth")
  private fun requestExternalStorageWritePermission(): Boolean {
    var isPermissionGranted = false
    if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
      ) {
        isPermissionGranted = true
      } else {
        readStoragePermissionLauncher?.launch(
          arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
          )
        )
      }
    } else {
      isPermissionGranted = true
    }
    return isPermissionGranted
  }
}
