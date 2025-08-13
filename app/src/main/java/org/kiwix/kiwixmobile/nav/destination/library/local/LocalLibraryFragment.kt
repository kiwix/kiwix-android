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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import eu.mhutti1.utils.storage.Bytes
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isManageExternalStoragePermissionGranted
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.isSplittedZimFile
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
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

@Suppress("LargeClass")
class LocalLibraryFragment : BaseFragment(), CopyMoveFileHandler.FileCopyMoveCallback {
  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Inject lateinit var dialogShower: DialogShower

  @Inject lateinit var mainRepositoryActions: MainRepositoryActions

  @Inject lateinit var zimReaderFactory: ZimFileReader.Factory

  @JvmField
  @Inject
  var copyMoveFileHandler: CopyMoveFileHandler? = null

  private var actionMode: ActionMode? = null
  private val coroutineJobs: MutableList<Job> = mutableListOf()
  private var permissionDeniedLayoutShowing = false
  private var zimFileUri: Uri? = null
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

  private var storagePermissionLauncher: ActivityResultLauncher<Array<String>>? =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
      val isGranted = permissionResult.values.all { it }
      val isPermanentlyDenied = readStorageHasBeenPermanentlyDenied(isGranted)
      permissionDeniedLayoutShowing = isPermanentlyDenied
      if (permissionDeniedLayoutShowing) {
        updateLibraryScreenState(
          noFilesViewItem = Triple(
            requireActivity().resources.getString(string.grant_read_storage_permission),
            requireActivity().resources.getString(string.go_to_settings_label),
            true
          )
        )
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
    LanguageUtils(requireActivity())
      .changeFont(requireActivity(), sharedPreferenceUtil)
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
    copyMoveFileHandler?.apply {
      setFileCopyMoveCallback(this@LocalLibraryFragment)
      setLifeCycleScope(lifecycleScope)
      setAlertDialogShower(dialogShower as AlertDialogShower)
    }
    zimManageViewModel.setAlertDialogShower(dialogShower as AlertDialogShower)
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
      handleSelectedFileUri(zimFileUri.toUri())
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
      dialogShower.show(
        KiwixDialog.ManageExternalFilesPermissionDialog,
        {
          this.activity?.let(FragmentActivity::navigateToSettings)
        }
      )
    }
  }

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
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)
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
      if (result.resultCode == RESULT_OK) {
        result.data?.data?.let {
          requireActivity().applicationContext.contentResolver.takePersistableUriPermission(
            it,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          )
          handleSelectedFileUri(it)
        }
      }
    }

  fun handleSelectedFileUri(uri: Uri) {
    lifecycleScope.launch {
      if (sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()) {
        val documentFile =
          when (uri.scheme) {
            "file" -> DocumentFile.fromFile(File("$uri"))
            else -> {
              DocumentFile.fromSingleUri(requireActivity(), uri)
            }
          }
        // If the file is not valid, it shows an error message and stops further processing.
        // If the file name is not found, then let them to copy the file
        // and we will handle this later.
        val fileName = documentFile?.name
        if (fileName != null && !isValidZimFile(fileName)) {
          activity.toast(getString(string.error_file_invalid, "$uri"))
          return@launch
        }
        copyMoveFileHandler?.showMoveFileToPublicDirectoryDialog(
          uri,
          documentFile,
          // pass if fileName is null then we will validate it after copying/moving
          fileName == null,
          parentFragmentManager
        )
      } else {
        zimFileUri = uri
        if (!requireActivity().isManageExternalStoragePermissionGranted(sharedPreferenceUtil)) {
          showManageExternalStoragePermissionDialog()
        } else if (requestExternalStorageWritePermission()) {
          getZimFileFromUri(uri)?.let(::navigateToReaderFragment)
        }
      }
    }
  }

  private fun isValidZimFile(fileName: String): Boolean =
    FileUtils.isValidZimFile(fileName) || isSplittedZimFile(fileName)

  private suspend fun getZimFileFromUri(
    uri: Uri
  ): File? {
    val filePath =
      FileUtils.getLocalFilePathByUri(
        requireActivity().applicationContext,
        uri
      )
    if (filePath == null || !File(filePath).isFileExist()) {
      Log.e(
        TAG_KIWIX,
        "The Selected ZIM file not found in the storage. File Uri = $uri\n" +
          "Retrieved Path = $filePath"
      )
      activity.toast(getString(string.error_file_not_found, "$uri"))
      return null
    }
    val file = File(filePath)
    return if (!FileUtils.isValidZimFile(file.path)) {
      Log.e(TAG_KIWIX, "Selected ZIM file is not a valid ZIM file. File path = ${file.path}")
      activity.toast(getString(string.error_file_invalid, file.path))
      null
    } else {
      file
    }
  }

  @Suppress("InjectDispatcher")
  private fun navigateToReaderFragment(file: File) {
    if (!file.canRead()) {
      activity.toast(string.unable_to_read_zim_file)
    } else {
      // Save the ZIM file to the database to display it on the local library screen.
      // This is particularly useful when storage is slow or contains a large number of files.
      // In such cases, scanning may take some time to show all the files on the
      // local library screen. Since our application is already aware of this opened ZIM file,
      // we can directly add it to the database.
      // See https://github.com/kiwix/kiwix-android/issues/3650
      CoroutineScope(Dispatchers.IO).launch {
        zimReaderFactory.create(ZimReaderSource(file))
          ?.let { zimFileReader ->
            val book = Book().apply { update(zimFileReader.jniKiwixReader) }
            mainRepositoryActions.saveBook(book)
            zimFileReader.dispose()
          }
      }
      val navOptions = NavOptions.Builder()
        .setPopUpTo(KiwixDestination.Reader.route, false)
        .build()
      activity?.navigate(
        KiwixDestination.Reader.createRoute(zimFileUri = file.toUri().toString()),
        navOptions
      )
    }
  }

  override fun onResume() {
    super.onResume()
    if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
      !sharedPreferenceUtil.prefIsTest && !permissionDeniedLayoutShowing
    ) {
      checkPermissions()
    } else if (!permissionDeniedLayoutShowing) {
      updateLibraryScreenState(
        noFilesViewItem = Triple(
          requireActivity().resources.getString(string.no_files_here),
          requireActivity().resources.getString(string.download_books),
          false
        )
      )
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    actionMode = null
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    storagePermissionLauncher?.unregister()
    storagePermissionLauncher = null
    copyMoveFileHandler?.dispose()
    copyMoveFileHandler = null
    readStoragePermissionLauncher?.unregister()
    readStoragePermissionLauncher = null
    zimFileUri = null
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

  private fun checkPermissions() {
    if (ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        context.toast(string.request_storage)
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
    if (!sharedPreferenceUtil.isPlayStoreBuild && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      if (!Environment.isExternalStorageManager()) {
        // We do not have the permission!!
        if (sharedPreferenceUtil.manageExternalFilesPermissionDialog) {
          // We should only ask for first time, If the users wants to revoke settings
          // then they can directly toggle this feature from settings screen
          sharedPreferenceUtil.manageExternalFilesPermissionDialog = false
          // Show Dialog and  Go to settings to give permission
          showManageExternalStoragePermissionDialog()
        }
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

  override fun onFileCopied(file: File) {
    validateAndOpenZimInReader(file)
  }

  override fun onFileMoved(file: File) {
    validateAndOpenZimInReader(file)
  }

  override fun onError(errorMessage: String) {
    activity.toast(errorMessage)
  }

  override fun filesystemDoesNotSupportedCopyMoveFilesOver4GB() {
    showStorageSelectionSnackBar(getString(R.string.file_system_does_not_support_4gb))
  }

  override fun insufficientSpaceInStorage(availableSpace: Long) {
    val message =
      """
      ${getString(string.move_no_space)}
      ${getString(string.space_available)} ${Bytes(availableSpace).humanReadable}
      """.trimIndent()

    showStorageSelectionSnackBar(message)
  }

  private fun showStorageSelectionSnackBar(message: String) {
    libraryScreenState.value.snackBarHostState.snack(
      message = message,
      actionLabel = getString(string.change_storage),
      lifecycleScope = lifecycleScope,
      actionClick = {
        lifecycleScope.launch {
          showStorageSelectDialog((requireActivity() as KiwixMainActivity).getStorageDeviceList())
        }
      }
    )
  }

  private fun showStorageSelectDialog(storageDeviceList: List<StorageDevice>) =
    StorageSelectDialog()
      .apply {
        onSelectAction = ::storeDeviceInPreferences
        setStorageDeviceList(storageDeviceList)
        setShouldShowCheckboxSelected(true)
      }
      .show(parentFragmentManager, getString(string.pref_storage))

  private fun storeDeviceInPreferences(
    storageDevice: StorageDevice
  ) {
    lifecycleScope.launch {
      sharedPreferenceUtil.putPrefStorage(
        sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name)
      )
      sharedPreferenceUtil.putStoragePosition(
        if (storageDevice.isInternal) {
          INTERNAL_SELECT_POSITION
        } else {
          EXTERNAL_SELECT_POSITION
        }
      )
      // after selecting the storage try to copy/move the zim file.
      copyMoveFileHandler?.copyMoveZIMFileInSelectedStorage(storageDevice)
    }
  }

  private fun validateAndOpenZimInReader(file: File) {
    if (isSplittedZimFile(file.path)) {
      showWarningDialogForSplittedZimFile()
    } else {
      navigateToReaderFragment(file = file)
    }
  }

  private fun showWarningDialogForSplittedZimFile() {
    dialogShower.show(KiwixDialog.ShowWarningAboutSplittedZimFile)
  }

  private var readStoragePermissionLauncher: ActivityResultLauncher<Array<String>>? =
    registerForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionResult ->
      val isGranted =
        permissionResult.entries.all(
          Map.Entry<String, @JvmSuppressWildcards Boolean>::value
        )
      if (isGranted) {
        zimFileUri?.let {
          // open the selected ZIM file in reader.
          lifecycleScope.launch {
            getZimFileFromUri(it)?.let(::navigateToReaderFragment)
          }
        }
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
