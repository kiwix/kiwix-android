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

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.LocalActivity
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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.downloader.downloadManager.SIX
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.AllowPermission
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllHistory
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllNotes
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ExportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ImportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OnStorageItemClick
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OpenCredits
import org.kiwix.kiwixmobile.core.settings.viewmodel.SettingsViewModel
import org.kiwix.kiwixmobile.core.settings.viewmodel.ZOOM_OFFSET
import org.kiwix.kiwixmobile.core.settings.viewmodel.ZOOM_SCALE
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
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
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.dialog.DialogConfirmButton
import org.kiwix.kiwixmobile.core.utils.dialog.DialogTitle
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixBasicDialogFrame
import java.util.Locale
import kotlin.math.roundToInt

const val PREFERENCE_ITEM_TESTING_TAG = "preferenceItemTestingTag"
const val SWITCH_PREFERENCE_TESTING_TAG = "switchPreferenceTestingTag"
const val SEEKBAR_PREFERENCE_TESTING_TAG = "seekBarPreferenceTestingTag"
const val DIALOG_PREFERENCE_ITEM_TESTING_TAG = "dialogPreferenceItemTestingTag"
const val SETTINGS_LIST_TESTING_TAG = "settingsListTestingTag"

private const val DIALOG_LIST_MAX_HEIGHT_RATIO = 0.8f

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Composable
fun SettingsScreen(
  settingScreenState: SettingScreenState,
  settingsViewModel: SettingsViewModel,
  navigationIcon: @Composable() () -> Unit
) {
  KiwixTheme {
    Scaffold(
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
        item { DisplayCategory(settingsViewModel) }
        item { ExtrasCategory(settingsViewModel, settingScreenState) }
        storageCategory(settingScreenState, settingsViewModel)
        item { HistoryCategory(settingsViewModel) }
        item { NotesCategory(settingsViewModel) }
        item { BookmarksCategory(settingsViewModel) }
        item { PermissionCategory(settingsViewModel, settingScreenState) }
        if (settingScreenState.shouldShowLanguageCategory) {
          item { LanguageCategory(settingScreenState) }
        }
        informationCategory(settingsViewModel, settingScreenState)
      }
    }
  }
}

@Composable
private fun HistoryCategory(settingsViewModel: SettingsViewModel) {
  SettingsCategory(stringResource(R.string.history)) {
    PreferenceItem(
      stringResource(R.string.pref_clear_all_history_title),
      stringResource(R.string.pref_clear_all_history_summary)
    ) { settingsViewModel.sendAction(ClearAllHistory) }
  }
}

@Composable
private fun NotesCategory(settingsViewModel: SettingsViewModel) {
  SettingsCategory(stringResource(R.string.pref_notes)) {
    PreferenceItem(
      stringResource(R.string.pref_clear_all_notes_title),
      stringResource(R.string.pref_clear_all_notes_summary)
    ) { settingsViewModel.sendAction(ClearAllNotes) }
  }
}

@Composable
private fun BookmarksCategory(settingsViewModel: SettingsViewModel) {
  SettingsCategory(stringResource(R.string.bookmarks)) {
    PreferenceItem(
      stringResource(R.string.pref_import_bookmark_title),
      stringResource(R.string.pref_import_bookmark_summary)
    ) { settingsViewModel.sendAction(ImportBookmarks) }
    PreferenceItem(
      stringResource(R.string.pref_export_bookmark_title),
      stringResource(R.string.pref_export_bookmark_summary)
    ) { settingsViewModel.sendAction(ExportBookmarks) }
  }
}

@Composable
private fun PermissionCategory(
  settingsViewModel: SettingsViewModel,
  settingScreenState: SettingScreenState
) {
  if (settingScreenState.permissionItem.first) {
    SettingsCategory(stringResource(R.string.pref_permission)) {
      PreferenceItem(
        stringResource(R.string.pref_allow_to_read_or_write_zim_files_on_sd_card),
        settingScreenState.permissionItem.second
      ) { settingsViewModel.sendAction(AllowPermission) }
    }
  }
}

@Composable
private fun LanguageCategory(settingScreenState: SettingScreenState) {
  if (settingScreenState.shouldShowLanguageCategory) {
    val context = LocalActivity.current ?: return
    val languageCodes = remember {
      listOf(Locale.ROOT.language) + LanguageUtils(context).keys
    }

    val selectedCode = remember {
      if (languageCodes.contains(settingScreenState.sharedPreferenceUtil.prefLanguage)) {
        settingScreenState.sharedPreferenceUtil.prefLanguage
      } else {
        "en"
      }
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

        settingScreenState.sharedPreferenceUtil.putPrefLanguage(selectedLangCode)
        handleLocaleChange(context, selectedLangCode, settingScreenState.sharedPreferenceUtil)
        settingScreenState.onLanguageChanged()
      }
    }
  }
}

private fun LazyListScope.informationCategory(
  settingsViewModel: SettingsViewModel,
  settingScreenState: SettingScreenState
) {
  item {
    SettingsCategory(stringResource(R.string.pref_info_title)) {
      PreferenceItem(
        stringResource(R.string.pref_info_version),
        settingScreenState.versionInformation
      ) { }
      PreferenceItem(
        stringResource(R.string.pref_credits),
        stringResource(R.string.pref_credits_title),
      ) { settingsViewModel.sendAction(OpenCredits) }
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
  settingScreenState: SettingScreenState,
  settingsViewModel: SettingsViewModel
) {
  if (!settingScreenState.shouldShowStorageCategory) return
  item {
    SettingsCategory(stringResource(R.string.pref_storage)) {
      if (settingScreenState.isLoadingStorageDetails) {
        StorageLoadingPreference()
      } else {
        Column {
          settingScreenState.storageDeviceList.forEachIndexed { index, item ->
            StorageDeviceItem(
              index,
              item,
              true,
              { settingsViewModel.sendAction(OnStorageItemClick(it)) },
              settingScreenState.storageCalculator,
              settingScreenState.sharedPreferenceUtil
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ExtrasCategory(
  settingsViewModel: SettingsViewModel,
  settingScreenState: SettingScreenState
) {
  val newTabInBackground by settingsViewModel.newTabInBackground
  val externalLinkPopup by settingsViewModel.externalLinkPopup
  val wifiOnly by settingsViewModel.wifiOnly.collectAsState()
  SettingsCategory(stringResource(R.string.pref_extras)) {
    SwitchPreference(
      title = stringResource(R.string.pref_newtab_background_title),
      summary = stringResource(R.string.pref_newtab_background_summary),
      checked = newTabInBackground,
      onCheckedChange = { settingsViewModel.setNewTabInBackground(it) }
    )
    if (settingScreenState.shouldShowExternalLinkPreference) {
      SwitchPreference(
        title = stringResource(R.string.pref_external_link_popup_title),
        summary = stringResource(R.string.pref_external_link_popup_summary),
        checked = externalLinkPopup,
        onCheckedChange = { settingsViewModel.setExternalLinkPopup(it) }
      )
    }
    if (settingScreenState.shouldShowPrefWifiOnlyPreference) {
      SwitchPreference(
        title = stringResource(R.string.pref_wifi_only),
        summary = stringResource(R.string.pref_wifi_only),
        checked = wifiOnly,
        onCheckedChange = { settingsViewModel.setWifiOnly(it) }
      )
    }
  }
}

@Composable
private fun DisplayCategory(settingsViewModel: SettingsViewModel) {
  val themeLabel by settingsViewModel.themeLabel.collectAsState()
  val backToTopEnabled by settingsViewModel.backToTopEnabled
  val textZoom by settingsViewModel.textZoom.collectAsState()
  val textZoomPosition = (textZoom / ZOOM_SCALE) - ZOOM_OFFSET
  SettingsCategory(stringResource(R.string.pref_display_title)) {
    AppThemePreference(themeLabel = themeLabel, settingsViewModel = settingsViewModel)
    SwitchPreference(
      title = stringResource(R.string.pref_back_to_top),
      summary = stringResource(R.string.pref_back_to_top_summary),
      checked = backToTopEnabled,
      onCheckedChange = { settingsViewModel.setBackToTop(it) }
    )
    SeekBarPreference(
      title = stringResource(R.string.pref_text_zoom_title),
      summary = stringResource(R.string.percentage, textZoom),
      value = textZoomPosition,
      onValueChange = { settingsViewModel.setTextZoom(it) },
      valueRange = ZERO..SIX
    )
  }
}

@Composable
fun AppThemePreference(
  context: Context = LocalContext.current,
  themeLabel: String,
  settingsViewModel: SettingsViewModel,
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
      settingsViewModel.setAppTheme(selectedValue)
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        DialogConfirmButton(
          confirmButtonText = stringResource(R.string.cancel),
          dialogConfirmButtonClick = { showDialog = false },
          null
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
        testTag = PREFERENCE_ITEM_TESTING_TAG
        contentDescription = title
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
