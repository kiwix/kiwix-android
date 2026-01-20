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

package org.kiwix.kiwixmobile.core.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion.W400
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.navigateToSettings
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.AllowPermission
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllHistory
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllNotes
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ExportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ImportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OnStorageItemClick
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OpenCredits
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ShowSnackbar
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel.PermissionLaunchersForSettingScreen
import org.kiwix.kiwixmobile.core.settings.viewmodel.CoreSettingsViewModel.SettingsUiState
import org.kiwix.kiwixmobile.core.settings.viewmodel.ZOOM_OFFSET
import org.kiwix.kiwixmobile.core.settings.viewmodel.ZOOM_SCALE
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.components.StorageDeviceItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.CATEGORY_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_DEFAULT_PADDING_FOR_CONTENT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIX_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.STORAGE_LOADING_PROGRESS_BAR_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TWELVE_DP
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogConfirmButton
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogTitle
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixBasicDialogFrame
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import java.util.Locale
import kotlin.math.roundToInt

const val PREFERENCE_ITEM_TESTING_TAG = "preferenceItemTestingTag"
const val SWITCH_PREFERENCE_TESTING_TAG = "switchPreferenceTestingTag"
const val SEEKBAR_PREFERENCE_TESTING_TAG = "seekBarPreferenceTestingTag"
const val DIALOG_PREFERENCE_ITEM_TESTING_TAG = "dialogPreferenceItemTestingTag"
const val SETTINGS_LIST_TESTING_TAG = "settingsListTestingTag"

const val DIALOG_LIST_MAX_HEIGHT_RATIO = 0.8f

@Composable
fun SettingsScreenRoute(
  coreSettingsViewModel: CoreSettingsViewModel,
  navigateBack: () -> Unit
) {
  val activity = LocalActivity.current as CoreMainActivity
  // Setup viewModel data version name, observing the click events, etc.
  SetUpViewModelAndPermissionLauncher(coreSettingsViewModel, activity)
  // Attached DialogHost to screen to show our KiwixDialog.
  DialogHost(coreSettingsViewModel.alertDialogShower as AlertDialogShower)
  SettingsScreen(coreSettingsViewModel) { NavigationIcon(onClick = navigateBack) }
  // Change font according to app language.
  ChangeFontAccordingToLanguage(activity, coreSettingsViewModel.kiwixDataStore)
}

@Composable
private fun ChangeFontAccordingToLanguage(
  activity: CoreMainActivity,
  kiwixDataStore: KiwixDataStore
) {
  LaunchedEffect(Unit) {
    LanguageUtils(activity).changeFont(
      activity,
      kiwixDataStore
    )
  }
}

@Composable
private fun SetUpViewModelAndPermissionLauncher(
  coreSettingsViewModel: CoreSettingsViewModel,
  coreMainActivity: CoreMainActivity
) {
  val launchers = rememberPermissionLaunchers(
    viewModel = coreSettingsViewModel,
    activity = coreMainActivity
  )
  LaunchedEffect(Unit) {
    coreSettingsViewModel.initialize(activity = coreMainActivity)
    coreSettingsViewModel.actions
      .onEach { action ->
        handleSettingsAction(
          action = action,
          viewModel = coreSettingsViewModel,
          activity = coreMainActivity,
          launchers = launchers
        )
      }
      .launchIn(coreSettingsViewModel.viewModelScope)
  }
}

private suspend fun handleSettingsAction(
  action: Action,
  viewModel: CoreSettingsViewModel,
  activity: CoreMainActivity,
  launchers: PermissionLaunchersForSettingScreen
) {
  when (action) {
    AllowPermission -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.navigateToSettings()
      }
    }

    OpenCredits -> viewModel.openCredits()
    ClearAllHistory -> clearAllHistoryDialog(viewModel)
    ClearAllNotes -> showClearAllNotesDialog(viewModel)
    ExportBookmarks -> {
      if (viewModel.requestExternalStorageWritePermissionForExportBookmark()) {
        showExportBookmarkDialog(viewModel)
      }
    }

    ImportBookmarks ->
      showImportBookmarkDialog(viewModel, launchers.filePicker)

    is OnStorageItemClick ->
      viewModel.onStorageDeviceSelected(action.storageDevice, activity)

    is ShowSnackbar ->
      showSnackbar(action.message, action.lifecycleScope, viewModel)

    Action.RequestWriteStoragePermission ->
      launchers.writeStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    Action.NavigateToAppSettingsDialog ->
      showNavigateToAppSettingsDialog(viewModel)
  }
}

@Composable
private fun rememberPermissionLaunchers(
  viewModel: CoreSettingsViewModel,
  activity: CoreMainActivity
): PermissionLaunchersForSettingScreen {
  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      viewModel.onStoragePermissionResult(it, activity)
    }

  val filePickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK) {
        viewModel.onBookmarkFileSelected(it)
      }
    }

  return remember(permissionLauncher, filePickerLauncher) {
    PermissionLaunchersForSettingScreen(
      permissionLauncher,
      filePickerLauncher
    )
  }
}

private fun showSnackbar(
  message: String,
  lifeCycleScope: CoroutineScope,
  coreSettingsViewModel: CoreSettingsViewModel
) {
  coreSettingsViewModel.uiState.value.snackbarHostState.snack(
    message = message,
    lifecycleScope = lifeCycleScope
  )
}

private fun showNavigateToAppSettingsDialog(coreSettingsViewModel: CoreSettingsViewModel) {
  coreSettingsViewModel.alertDialogShower.show(
    KiwixDialog.ReadPermissionRequired,
    coreSettingsViewModel.context::navigateToAppSettings
  )
}

private fun clearAllHistoryDialog(coreSettingsViewModel: CoreSettingsViewModel) {
  coreSettingsViewModel.alertDialogShower.show(KiwixDialog.ClearAllHistory, {
    coreSettingsViewModel.clearHistory()
  })
}

private fun showClearAllNotesDialog(coreSettingsViewModel: CoreSettingsViewModel) {
  coreSettingsViewModel.alertDialogShower.show(
    KiwixDialog.ClearAllNotes,
    { coreSettingsViewModel.clearAllNotes() }
  )
}

private fun showImportBookmarkDialog(
  coreSettingsViewModel: CoreSettingsViewModel,
  fileSelectLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>
) {
  coreSettingsViewModel.alertDialogShower.show(
    KiwixDialog.ImportBookmarks,
    { coreSettingsViewModel.showFileChooser(fileSelectLauncher) }
  )
}

private fun showExportBookmarkDialog(coreSettingsViewModel: CoreSettingsViewModel) {
  coreSettingsViewModel.alertDialogShower.show(
    KiwixDialog.YesNoDialog.ExportBookmarks,
    { coreSettingsViewModel.exportBookmark() }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Composable
private fun SettingsScreen(
  coreSettingsViewModel: CoreSettingsViewModel,
  navigationIcon: @Composable() () -> Unit
) {
  val uiState = coreSettingsViewModel.uiState.collectAsStateWithLifecycle()
  KiwixTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = uiState.value.snackbarHostState) },
      topBar = {
        KiwixAppBar(
          title = stringResource(R.string.menu_settings),
          navigationIcon = navigationIcon
        )
      }
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .semantics {
            testTag = SETTINGS_LIST_TESTING_TAG
          }
      ) {
        item { DisplayCategory(coreSettingsViewModel) }
        item { ExtrasCategory(coreSettingsViewModel, uiState.value) }
        storageCategory(uiState.value, coreSettingsViewModel)
        item { HistoryCategory(coreSettingsViewModel) }
        item { NotesCategory(coreSettingsViewModel) }
        item { BookmarksCategory(coreSettingsViewModel) }
        item { PermissionCategory(coreSettingsViewModel, uiState.value) }
        if (uiState.value.shouldShowLanguageCategory) {
          item { LanguageCategory(uiState.value, coreSettingsViewModel) }
        }
        informationCategory(coreSettingsViewModel, uiState.value)
      }
    }
  }
}

@Composable
private fun HistoryCategory(coreSettingsViewModel: CoreSettingsViewModel) {
  SettingsCategory(stringResource(R.string.history)) {
    PreferenceItem(
      stringResource(R.string.pref_clear_all_history_title),
      stringResource(R.string.pref_clear_all_history_summary)
    ) { coreSettingsViewModel.sendAction(ClearAllHistory) }
  }
}

@Composable
private fun NotesCategory(coreSettingsViewModel: CoreSettingsViewModel) {
  SettingsCategory(stringResource(R.string.pref_notes)) {
    PreferenceItem(
      stringResource(R.string.pref_clear_all_notes_title),
      stringResource(R.string.pref_clear_all_notes_summary)
    ) { coreSettingsViewModel.sendAction(ClearAllNotes) }
  }
}

@Composable
private fun BookmarksCategory(coreSettingsViewModel: CoreSettingsViewModel) {
  SettingsCategory(stringResource(R.string.bookmarks)) {
    PreferenceItem(
      stringResource(R.string.pref_import_bookmark_title),
      stringResource(R.string.pref_import_bookmark_summary)
    ) { coreSettingsViewModel.sendAction(ImportBookmarks) }
    PreferenceItem(
      stringResource(R.string.pref_export_bookmark_title),
      stringResource(R.string.pref_export_bookmark_summary)
    ) { coreSettingsViewModel.sendAction(ExportBookmarks) }
  }
}

@Composable
private fun PermissionCategory(
  coreSettingsViewModel: CoreSettingsViewModel,
  settingsUiState: SettingsUiState,
) {
  if (settingsUiState.permissionItem.first) {
    SettingsCategory(stringResource(R.string.pref_permission)) {
      PreferenceItem(
        stringResource(R.string.pref_allow_to_read_or_write_zim_files_on_sd_card),
        settingsUiState.permissionItem.second
      ) { coreSettingsViewModel.sendAction(AllowPermission) }
    }
  }
}

@Composable
private fun LanguageCategory(
  settingsUiState: SettingsUiState,
  coreSettingsViewModel: CoreSettingsViewModel
) {
  if (settingsUiState.shouldShowLanguageCategory) {
    val context = LocalActivity.current ?: return
    val languageCodes = remember {
      listOf(Locale.ROOT.language) + LanguageUtils(context).keys
    }

    val prefLanguage by coreSettingsViewModel.kiwixDataStore.prefLanguage
      .collectAsState(initial = "en")
    val selectedCode =
      when {
        !AppCompatDelegate.getApplicationLocales().isEmpty ->
          // Fetches the current Application Locale from the list
          AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"

        languageCodes.contains(prefLanguage) -> prefLanguage
        else -> "en"
      }

    val languageDisplayNames = languageCodes.mapIndexed { index, code ->
      if (index == 0) {
        stringResource(R.string.device_default)
      } else {
        val locale = code.convertToLocal()
        "${locale.displayLanguage} (${locale.getDisplayLanguage(locale)})"
      }
    }

    val selectedIndex = languageCodes.indexOf(selectedCode)
    SettingsCategory(stringResource(R.string.pref_language_title)) {
      ListPreference(
        titleId = R.string.pref_language_title,
        summary = languageDisplayNames.getOrNull(selectedIndex) ?: selectedCode,
        options = languageDisplayNames,
        selectedOption = languageDisplayNames[selectedIndex]
      ) { selectedDisplay ->
        val index = languageDisplayNames.indexOf(selectedDisplay)
        val selectedLangCode = languageCodes.getOrNull(index) ?: return@ListPreference
        coreSettingsViewModel.updateAppLanguage(selectedLangCode)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selectedLangCode))
      }
    }
  }
}

private fun LazyListScope.informationCategory(
  coreSettingsViewModel: CoreSettingsViewModel,
  settingsUiState: SettingsUiState,
) {
  item {
    SettingsCategory(stringResource(R.string.pref_info_title)) {
      PreferenceItem(
        stringResource(R.string.pref_info_version),
        settingsUiState.versionInformation
      ) { }
      PreferenceItem(
        stringResource(R.string.pref_credits),
        stringResource(R.string.pref_credits_title),
      ) { coreSettingsViewModel.sendAction(OpenCredits) }
    }
  }
}

@Composable
private fun StorageLoadingPreference() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      ContentLoadingProgressBar(modifier = Modifier.size(STORAGE_LOADING_PROGRESS_BAR_SIZE))
      Spacer(modifier = Modifier.height(EIGHT_DP))
      PreferenceSummaryText(stringResource(R.string.fetching_storage_info))
    }
  }
}

private fun LazyListScope.storageCategory(
  settingsUiState: SettingsUiState,
  coreSettingsViewModel: CoreSettingsViewModel
) {
  if (!settingsUiState.shouldShowStorageCategory) return
  item {
    SettingsCategory(stringResource(R.string.pref_storage)) {
      if (settingsUiState.isLoadingStorageDetails) {
        StorageLoadingPreference()
      } else {
        Column {
          settingsUiState.storageDeviceList.forEachIndexed { index, item ->
            StorageDeviceItem(
              index,
              item,
              true,
              { coreSettingsViewModel.sendAction(OnStorageItemClick(it)) },
              coreSettingsViewModel.storageCalculator,
              coreSettingsViewModel.kiwixDataStore
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ExtrasCategory(
  coreSettingsViewModel: CoreSettingsViewModel,
  settingsUiState: SettingsUiState
) {
  val newTabInBackground by coreSettingsViewModel.newTabInBackground.collectAsStateWithLifecycle()
  val externalLinkPopup by coreSettingsViewModel.externalLinkPopup.collectAsStateWithLifecycle()
  val wifiOnly by coreSettingsViewModel.wifiOnly.collectAsStateWithLifecycle()
  SettingsCategory(stringResource(R.string.pref_extras)) {
    SwitchPreference(
      title = stringResource(R.string.pref_newtab_background_title),
      summary = stringResource(R.string.pref_newtab_background_summary),
      checked = newTabInBackground,
      onCheckedChange = { coreSettingsViewModel.setNewTabInBackground(it) }
    )
    if (settingsUiState.shouldShowExternalLinkPreference) {
      SwitchPreference(
        title = stringResource(R.string.pref_external_link_popup_title),
        summary = stringResource(R.string.pref_external_link_popup_summary),
        checked = externalLinkPopup,
        onCheckedChange = { coreSettingsViewModel.setExternalLinkPopup(it) }
      )
    }
    if (settingsUiState.shouldShowPrefWifiOnlyPreference) {
      SwitchPreference(
        title = stringResource(R.string.pref_wifi_only),
        summary = stringResource(R.string.pref_wifi_only),
        checked = wifiOnly,
        onCheckedChange = { coreSettingsViewModel.setWifiOnly(it) }
      )
    }
  }
}

@Composable
private fun DisplayCategory(coreSettingsViewModel: CoreSettingsViewModel) {
  val themeLabel by coreSettingsViewModel.themeLabel.collectAsStateWithLifecycle()
  val backToTopEnabled by coreSettingsViewModel.backToTopEnabled.collectAsStateWithLifecycle()
  val textZoom by coreSettingsViewModel.textZoom.collectAsStateWithLifecycle()
  val textZoomPosition = (textZoom / ZOOM_SCALE) - ZOOM_OFFSET
  SettingsCategory(stringResource(R.string.pref_display_title)) {
    AppThemePreference(themeLabel = themeLabel, coreSettingsViewModel = coreSettingsViewModel)
    SwitchPreference(
      title = stringResource(R.string.pref_back_to_top),
      summary = stringResource(R.string.pref_back_to_top_summary),
      checked = backToTopEnabled,
      onCheckedChange = { coreSettingsViewModel.setBackToTop(it) }
    )
    SeekBarPreference(
      title = stringResource(R.string.pref_text_zoom_title),
      summary = stringResource(R.string.percentage, textZoom),
      value = textZoomPosition,
      onValueChange = { coreSettingsViewModel.setTextZoom(it) },
      valueRange = ZERO..SIX
    )
  }
}

@Composable
fun AppThemePreference(
  context: Context = LocalContext.current,
  themeLabel: String,
  coreSettingsViewModel: CoreSettingsViewModel,
) {
  val entries = remember {
    context.resources.getStringArray(R.array.pref_themes_entries).toList()
  }
  val values = remember {
    context.resources.getStringArray(R.array.pref_themes_values).toList()
  }

  ListPreference(
    titleId = R.string.pref_theme,
    summary = stringResource(id = R.string.pref_theme_summary),
    options = entries,
    selectedOption = themeLabel,
    onOptionSelected = { selectedEntry ->
      val selectedValue = entries.indexOf(selectedEntry).let { values[it] }
      coreSettingsViewModel.setAppTheme(selectedValue)
    }
  )
}

@Composable
private fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = SIXTEEN_DP)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = TWELVE_DP)
    ) {
      PreferenceCategoryText(title)
      Spacer(modifier = Modifier.padding(vertical = FIVE_DP))
      content()
    }
    HorizontalDivider(
      thickness = ONE_DP,
      modifier = Modifier.padding(vertical = FIVE_DP)
    )
  }
}

@Composable
private fun SwitchPreference(
  title: String,
  summary: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = TWELVE_DP)
      .clickable(onClick = { onCheckedChange.invoke(!checked) })
      .semantics {
        testTag = SWITCH_PREFERENCE_TESTING_TAG
        contentDescription = title
      }
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(end = EIGHT_DP)
      ) {
        PreferenceTitleText(title)
        PreferenceSummaryText(summary)
      }
      Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.semantics { contentDescription = title }
      )
    }
  }
}

@Composable
private fun SeekBarPreference(
  title: String,
  summary: String,
  value: Int,
  onValueChange: (Int) -> Unit,
  valueRange: IntRange
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = TWELVE_DP)
      .semantics {
        testTag = SEEKBAR_PREFERENCE_TESTING_TAG
        contentDescription = title
        hideFromAccessibility()
      }
  ) {
    PreferenceTitleText(title)
    PreferenceSummaryText(summary)
    Slider(
      value = value.toFloat(),
      onValueChange = { newValue ->
        val rounded = newValue.roundToInt().coerceIn(valueRange)
        onValueChange(rounded)
      },
      valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
      steps = valueRange.last - valueRange.first - 1
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPreference(
  titleId: Int,
  summary: String,
  options: List<String>,
  selectedOption: String,
  onOptionSelected: (String) -> Unit
) {
  var showDialog by remember { mutableStateOf(false) }
  var selected by remember { mutableStateOf(selectedOption) }

  LaunchedEffect(selectedOption) {
    selected = selectedOption
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showDialog = true }
  ) {
    PreferenceItem(stringResource(titleId), summary) { showDialog = true }
  }

  if (showDialog) {
    KiwixBasicDialogFrame(
      onDismissRequest = { showDialog = false }
    ) {
      Column(Modifier.padding(horizontal = DIALOG_DEFAULT_PADDING_FOR_CONTENT)) {
        DialogTitle(titleId)
        BoxWithConstraints {
          val listMaxHeight = this.maxHeight * DIALOG_LIST_MAX_HEIGHT_RATIO
          ListOptions(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = listMaxHeight)
              .verticalScroll(rememberScrollState()),
            options = options,
            selected = selected,
            onOptionSelected = {
              selected = it
              onOptionSelected(it)
              showDialog = false
            }
          )
        }
        Spacer(modifier = Modifier.height(DIALOG_DEFAULT_PADDING_FOR_CONTENT))
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        DialogConfirmButton(
          confirmButtonText = stringResource(R.string.cancel),
          dialogConfirmButtonClick = { showDialog = false },
          alertDialogShower = null
        )
      }
    }
  }
}

@Composable
private fun ListOptions(
  modifier: Modifier,
  options: List<String>,
  selected: String,
  onOptionSelected: (String) -> Unit
) {
  Column(modifier = modifier) {
    options.forEach { option ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .selectable(
            selected = option == selected,
            onClick = { onOptionSelected(option) }
          )
          .padding(vertical = EIGHT_DP)
          .semantics {
            testTag = DIALOG_PREFERENCE_ITEM_TESTING_TAG
            contentDescription = option
          }
      ) {
        RadioButton(
          selected = option == selected,
          onClick = { onOptionSelected(option) },
          modifier = Modifier.semantics { contentDescription = "$option${option.hashCode()}" }
        )
        Text(text = option, style = MaterialTheme.typography.bodyLarge)
      }
    }
  }
}

@Composable
private fun PreferenceItem(title: String, summary: String, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = TWELVE_DP)
      .semantics {
        testTag = PREFERENCE_ITEM_TESTING_TAG + title
        hideFromAccessibility()
      }
  ) {
    PreferenceTitleText(title)
    PreferenceSummaryText(summary)
  }
}

@Composable
private fun PreferenceTitleText(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.bodyLarge
  )
  Spacer(Modifier.padding(top = SIX_DP))
}

@Composable
private fun PreferenceSummaryText(summary: String) {
  Text(
    text = summary,
    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = W400),
    color = MaterialTheme.colorScheme.onSecondary
  )
}

@Composable
private fun PreferenceCategoryText(categoryTitle: String) {
  Text(
    text = categoryTitle,
    fontSize = CATEGORY_TITLE_TEXT_SIZE,
    fontWeight = FontWeight.W600,
    color = MaterialTheme.colorScheme.primary
  )
}
