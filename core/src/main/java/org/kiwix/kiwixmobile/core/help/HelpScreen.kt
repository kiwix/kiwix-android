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

package org.kiwix.kiwixmobile.core.help

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.error.DiagnosticReportActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start

/*
Added the Values Here because of Linter configured according to XML Project
and the Composable names are in Camel case for the linter rules needed to be changed to Capital
*/

@Composable
fun HelpScreen(
  modifier: Modifier = Modifier,
  data: List<HelpScreenItemDataClass>,
  navController: NavController
) {
  val context = LocalContext.current
  val isDarkTheme = isSystemInDarkTheme()
  val backgroundColor = getBackgroundColor(isDarkTheme)
  val dividerColor = getDividerColor(isDarkTheme)

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = { HelpScreenTopBar(modifier, navController) },
    containerColor = backgroundColor
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .padding(paddingValues)
    ) {
      DiagnosticDataRow(context, isDarkTheme)
      HelpItemList(data, dividerColor)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpScreenTopBar(modifier: Modifier, navController: NavController) {
  TopAppBar(
    title = {
      Text(
        modifier = modifier.padding(start = 16.dp),
        text = stringResource(id = R.string.menu_help),
        color = Color.White
      )
    },
    navigationIcon = {
      IconButton(onClick = navController::popBackStack) {
        Icon(
          imageVector = Icons.Filled.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Black
    )
  )
}

@Composable
private fun DiagnosticDataRow(context: Context, isDarkTheme: Boolean) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        (context as? Activity)?.start<DiagnosticReportActivity>()
      },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Start
  ) {
    Image(
      painter = painterResource(R.drawable.ic_feedback_orange_24dp),
      contentDescription = "Feedback",
      modifier = Modifier
        .padding(16.dp)
    )
    Text(
      text = "Send Diagnostic Data",
      color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
      fontSize = 18.sp
    )
  }
}

@Composable
private fun HelpItemList(data: List<HelpScreenItemDataClass>, dividerColor: Color) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
  ) {
    itemsIndexed(data, key = { _, item -> item.title }) { _, item ->
      HorizontalDivider(
        color = dividerColor
      )
      HelpItem(data = item)
    }
    item {
      HorizontalDivider(
        color = dividerColor
      )
    }
  }
}

@Composable
private fun getBackgroundColor(isDarkTheme: Boolean): Color =
  if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray900) else Color.White

@Composable
private fun getDividerColor(isDarkTheme: Boolean): Color {
  return if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray600) else colorResource(
    id = R.color.mine_shaft_gray350
  )
}
