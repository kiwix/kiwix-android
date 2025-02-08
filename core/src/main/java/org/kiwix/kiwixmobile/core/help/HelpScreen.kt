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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
  modifier: Modifier = Modifier,
  data: List<HelpScreenItemDataClass>,
  navController: NavController
) {
  val context = LocalContext.current

  val isDarkTheme = isSystemInDarkTheme()

  val backgroundColor =
    if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray900) else Color.White
  val dividerColor =
    if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray600) else colorResource(
      id = R.color.mine_shaft_gray350
    )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            modifier = modifier.padding(start = 16.dp),
            text = stringResource(id = R.string.menu_help),
            color = Color.White // Set title text color to white
          )
        },
        navigationIcon = {
          IconButton(onClick = navController::popBackStack) {
            Icon(
              imageVector = Icons.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White // Set navigation icon color to white
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Black // Set top app bar background color to black
        )
      )
    },
    containerColor = backgroundColor
  ) {

    Column(
      modifier = Modifier
        .padding(it)
    ) {
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
          text = stringResource(R.string.send_report),
          color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
          fontSize = 18.sp
        )
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        itemsIndexed(data, key = { _, item -> item.title }) { index, item ->
          HorizontalDivider(
            color = dividerColor
          )
          HelpScreenItem(data = item)
        }
        item {
          HorizontalDivider(
            color = dividerColor
          )
        }
      }
    }
  }
}
