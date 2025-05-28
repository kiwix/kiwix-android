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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment.Companion.ZOOM_OFFSET
import org.kiwix.kiwixmobile.core.settings.CorePrefsFragment.Companion.ZOOM_SCALE
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.TWO
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ComposableLambdaParameterNaming")
@Composable
fun SettingsScreen(
  settingsViewModel: SettingsViewModel,
  navigationIcon: @Composable() () -> Unit
) {
  KiwixTheme {
    Scaffold(
      topBar = {
        KiwixAppBar(
          titleId = R.string.menu_settings,
          navigationIcon = navigationIcon
        )
      }
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .padding(SIXTEEN_DP)
      ) {
        item {
          DisplayCategory(settingsViewModel)
        }
        item {
          ExtrasCategory(settingsViewModel)
        }
      }
    }
  }
}

@Composable
private fun ExtrasCategory(settingsViewModel: SettingsViewModel) {
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
    SwitchPreference(
      title = stringResource(R.string.pref_external_link_popup_title),
      summary = stringResource(R.string.pref_external_link_popup_summary),
      checked = externalLinkPopup,
      onCheckedChange = { settingsViewModel.setExternalLinkPopup(it) }
    )
    SwitchPreference(
      title = stringResource(R.string.pref_wifi_only),
      summary = stringResource(R.string.pref_wifi_only),
      checked = wifiOnly,
      onCheckedChange = { settingsViewModel.setWifiOnly(it) }
    )
  }
}

@Composable
private fun DisplayCategory(settingsViewModel: SettingsViewModel) {
  val darkModeLabel by settingsViewModel.darkModeLabel.collectAsState()
  val backToTopEnabled by settingsViewModel.backToTopEnabled
  val textZoom by settingsViewModel.textZoom.collectAsState()
  SettingsCategory(stringResource(R.string.pref_display_title)) {
    DarkModePreference(darkModeLabel = darkModeLabel, settingsViewModel = settingsViewModel)
    SwitchPreference(
      title = stringResource(R.string.pref_back_to_top),
      summary = stringResource(R.string.pref_back_to_top_summary),
      checked = backToTopEnabled,
      onCheckedChange = { settingsViewModel.setBackToTop(it) }
    )
    SeekBarPreference(
      title = stringResource(R.string.pref_text_zoom_title),
      summary = stringResource(R.string.percentage, textZoom),
      value = textZoom,
      onValueChange = {
        settingsViewModel.setTextZoom((it + ZOOM_OFFSET) * ZOOM_SCALE)
      },
      valueRange = 50..200
    )
  }
}

@Composable
fun DarkModePreference(
  context: Context = LocalContext.current,
  darkModeLabel: String,
  settingsViewModel: SettingsViewModel,
) {
  val entries = remember {
    context.resources.getStringArray(R.array.pref_dark_modes_entries).toList()
  }
  val values = remember {
    context.resources.getStringArray(R.array.pref_dark_modes_values).toList()
  }

  ListPreference(
    title = stringResource(id = R.string.pref_display_title),
    summary = stringResource(id = R.string.pref_dark_mode_summary),
    options = entries,
    selectedOption = darkModeLabel,
    onOptionSelected = { selectedEntry ->
      val selectedValue = entries.indexOf(selectedEntry).let { values[it] }
      settingsViewModel.setDarkMode(selectedValue)
    }
  )
}

@Composable
private fun SettingsCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = EIGHT_DP)
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.padding(vertical = SIXTEEN_DP))
    content()
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
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(text = summary, style = MaterialTheme.typography.bodySmall)
      }
      Switch(checked = checked, onCheckedChange = onCheckedChange)
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
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    Text(text = summary, style = MaterialTheme.typography.bodySmall)
    Slider(
      value = value.toFloat(),
      onValueChange = { onValueChange(it.toInt()) },
      valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
      steps = valueRange.count() - TWO
    )
  }
}

@Composable
fun ListPreference(
  title: String,
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
      .padding(SIXTEEN_DP)
  ) {
    PreferenceItem(title, summary) { showDialog = true }
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = {
        Text(text = title)
      },
      text = {
        Column {
          options.forEach { option ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .selectable(
                  selected = (option == selected),
                  onClick = {
                    selected = option
                    onOptionSelected(option)
                  }
                )
                .padding(vertical = EIGHT_DP)
            ) {
              RadioButton(
                selected = (option == selected),
                onClick = {
                  selected = option
                  onOptionSelected(option)
                }
              )
              Text(text = option, style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          showDialog = false
        }) {
          Text(stringResource(R.string.cancel))
        }
      }
    )
  }
}

@Composable
private fun PreferenceItem(title: String, summary: String, onClick: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = EIGHT_DP)
  ) {
    Text(text = title, style = MaterialTheme.typography.bodyLarge)
    Text(text = summary, style = MaterialTheme.typography.bodySmall)
  }
}
