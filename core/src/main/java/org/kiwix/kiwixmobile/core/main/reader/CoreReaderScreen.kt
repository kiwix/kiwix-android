package org.kiwix.kiwixmobile.core.main.reader

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.safelyConsumeObservable
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.note.AddNoteDialogComposable
import org.kiwix.kiwixmobile.core.main.note.AddNoteDialogConfig
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.ClearNavigationHistory
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction.NavigationHistoryItemClick
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderEffect
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryDialog
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CoreReaderScreen(
  viewModel: CoreReaderViewModel,
  addNoteViewModel: AddNoteViewModel,
  activity: CoreMainActivity,
  alertDialogShower: AlertDialogShower,
  navHostController: NavHostController
) {
  val uiState by viewModel.uiState.collectAsState()
  val lifeCycleScope = rememberCoroutineScope()
  val snackBarHostState = remember { SnackbarHostState() }
  val readPermissionState =
    rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE) { isGranted ->
      viewModel.onReadStoragePermissionResult(isGranted)
    }
  val notificationPermission = if (viewModel.isAndroid13OrAbove) {
    rememberPermissionState(POST_NOTIFICATIONS) {
      viewModel.onNotificationPermissionResult(it, activity)
    }
  } else {
    null
  }
  val backStackEntry = navHostController.currentBackStackEntry
  LaunchedEffect(backStackEntry) {
    backStackEntry
      ?.savedStateHandle
      ?.getStateFlow<SearchItemToOpen?>(
        TAG_FILE_SEARCHED,
        null
      )
      ?.collect { item ->
        item ?: return@collect
        viewModel.pendingSearchItemManager.store(item)
        viewModel.emitEffect(ReaderEffect.ConsumeSavedStateHandle(listOf(TAG_FILE_SEARCHED)))
      }
  }
  CollectEffect(
    viewModel,
    addNoteViewModel,
    activity,
    alertDialogShower,
    snackBarHostState,
    lifeCycleScope,
    navHostController,
    readPermissionState,
    notificationPermission
  )
  ReaderScreen(
    state = uiState,
    snackBarHost = snackBarHostState,
    actionMenuItems = viewModel.readerMenuState?.menuItems.orEmpty(),
    onReaderAction = viewModel::onAction,
    onUserBackPressed = { viewModel.onUserBackPressed(activity) },
    navHostController = navHostController,
    mainActivityBottomAppBarScrollBehaviour = activity.bottomAppBarScrollBehaviour,
    navigationIcon = { NavigationItem(viewModel, activity) }
  )
}

@Composable
private fun NavigationItem(viewModel: CoreReaderViewModel, activity: CoreMainActivity) {
  NavigationIcon(
    iconItem = viewModel.navigationIcon(),
    contentDescription = viewModel.navigationIconContentDescription(),
    onClick = { viewModel.navigationIconClick(activity.navigationDrawerIsOpen()) },
    iconTint = viewModel.navigationIconTint()
  )
}

@OptIn(ExperimentalPermissionsApi::class)
@Suppress("LongParameterList", "CyclomaticComplexMethod")
@Composable
private fun CollectEffect(
  viewModel: CoreReaderViewModel,
  addNoteViewModel: AddNoteViewModel,
  activity: CoreMainActivity,
  alertDialogShower: AlertDialogShower,
  snackBarHostState: SnackbarHostState,
  lifeCycleScope: CoroutineScope,
  navHostController: NavHostController,
  readPermissionState: PermissionState,
  notificationPermission: PermissionState?,
) {
  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        is ReaderEffect.ShowSnackbar -> handleSnackBar(snackBarHostState, effect, lifeCycleScope)
        ReaderEffect.ClearActivityIntentAction -> clearActivityIntent(activity)
        ReaderEffect.CloseActivitySideBar -> openCloseActivitySidebar(activity, false)
        ReaderEffect.OpenActivitySideBar -> openCloseActivitySidebar(activity, true)
        is ReaderEffect.ConsumeSavedStateHandle -> consumeSavedStateHandle(effect, activity)
        ReaderEffect.DisableLeftSideBar -> enableDisableActivitySideBar(activity, false)
        ReaderEffect.EnableLeftSideBar -> enableDisableActivitySideBar(activity, true)
        ReaderEffect.HideActivityBottomAppBar -> showHideActivityBottomAppBar(activity, false)
        ReaderEffect.ShowActivityBottomAppBar -> showHideActivityBottomAppBar(activity, true)
        is ReaderEffect.NavigateTo -> handleNavigateTo(navHostController, effect)
        ReaderEffect.RequestNotificationPermission -> notificationPermission?.launchPermissionRequest()
        ReaderEffect.RequestReadStoragePermission -> readPermissionState.launchPermissionRequest()
        is ReaderEffect.SharePdfFile -> sharePdfFile(activity, effect.pdfFile)
        is ReaderEffect.ShowKiwixDialog -> showKiwixDialog(effect, alertDialogShower)
        is ReaderEffect.ShowToast -> showToast(activity, effect)
        is ReaderEffect.ShowNavigationHistoryDialog ->
          showNavigationHistoryDialog(effect, alertDialogShower, viewModel)

        is ReaderEffect.ShowAddNoteDialog ->
          showAddNoteDialog(alertDialogShower, addNoteViewModel, effect.kiwixWebView)
      }
    }
  }
}

private fun showKiwixDialog(
  effect: ReaderEffect.ShowKiwixDialog,
  alertDialogShower: AlertDialogShower
) {
  alertDialogShower.show(effect.kiwixDialog, effect.onClick)
}

private fun showAddNoteDialog(
  alertDialogShower: AlertDialogShower,
  addNoteViewModel: AddNoteViewModel,
  kiwixWebView: KiwixWebView?
) {
  val config = AddNoteDialogConfig(
    articleTitle = kiwixWebView?.title,
    currentWebViewUrl = kiwixWebView?.url,
    currentWebViewTitle = kiwixWebView?.title
  )

  alertDialogShower.show(
    KiwixDialog.AddNoteDialogDialog(
      ZERO.dp,
      {
        AddNoteDialogComposable(
          addNoteViewModel = addNoteViewModel,
          config = config,
          onDismiss = { alertDialogShower.dismiss() }
        )
      }
    )
  )
}

private fun sharePdfFile(context: Context, pdfFile: File) {
  try {
    val uri = FileProvider.getUriForFile(
      context,
      context.packageName + ".fileprovider",
      pdfFile
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
      putExtra(Intent.EXTRA_STREAM, uri)
      flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
  } catch (e: IOException) {
    e.printStackTrace()
    context.toast(string.unable_to_share_article, Toast.LENGTH_SHORT)
  }
}

private fun showNavigationHistoryDialog(
  effect: ReaderEffect.ShowNavigationHistoryDialog,
  alertDialogShower: AlertDialogShower,
  viewModel: CoreReaderViewModel
) {
  alertDialogShower.show(
    KiwixDialog.NavigationHistoryDialog(
      ZERO.dp,
      {
        NavigationHistoryDialog(
          titleId = if (effect.result.isForwardHistory) {
            string.forward_history
          } else {
            string.backward_history
          },
          effect.result.list.toMutableList(),
          { viewModel.onAction(NavigationHistoryItemClick(it)) },
          onClearNavigationHistoryClick = { viewModel.onAction(ClearNavigationHistory) },
          onDialogDismissRequest = { alertDialogShower.dismiss() },
        )
      }
    )
  )
}

private fun showToast(activity: CoreMainActivity, effect: ReaderEffect.ShowToast) {
  activity.toast(effect.message)
}

private fun handleNavigateTo(navController: NavHostController, effect: ReaderEffect.NavigateTo) {
  navController.navigate(effect.route, effect.navOptions)
}

private fun showHideActivityBottomAppBar(activity: CoreMainActivity, show: Boolean) {
  if (show) {
    activity.showBottomAppBar()
  } else {
    activity.hideBottomAppBar()
  }
}

private fun enableDisableActivitySideBar(activity: CoreMainActivity, enable: Boolean) {
  if (enable) {
    activity.enableLeftDrawer()
  } else {
    activity.disableLeftDrawer()
  }
}

private fun consumeSavedStateHandle(
  effect: ReaderEffect.ConsumeSavedStateHandle,
  activity: CoreMainActivity
) {
  effect.keys.forEach { key ->
    activity.safelyConsumeObservable(key)
  }
}

private fun openCloseActivitySidebar(activity: CoreMainActivity, open: Boolean) {
  if (open) {
    activity.openNavigationDrawer()
  } else {
    activity.closeNavigationDrawer()
  }
}

private fun clearActivityIntent(activity: CoreMainActivity) {
  activity.intent.action = null
}

private fun handleSnackBar(
  snackBarHostState: SnackbarHostState,
  snackBarData: ReaderEffect.ShowSnackbar,
  lifecycleScope: CoroutineScope
) {
  snackBarHostState.snack(
    snackBarData.message,
    actionLabel = snackBarData.actionLabel,
    actionClick = snackBarData.actionClick,
    snackbarDuration = snackBarData.snackbarDuration,
    lifecycleScope = lifecycleScope,
    snackBarResult = snackBarData.snackBarResult
  )
}
