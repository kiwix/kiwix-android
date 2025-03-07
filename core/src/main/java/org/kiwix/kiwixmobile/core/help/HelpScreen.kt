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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

import androidx.navigation.NavController
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.error.DiagnosticReportActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start

val SendDiagnosticReportFontSize = 18.sp

@Composable
fun HelpScreen(
  modifier: Modifier = Modifier,
  data: List<HelpScreenItemDataClass>,
  navController: NavController
) {
  val isDarkTheme = isSystemInDarkTheme()
  val backgroundColor =
    if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray900) else Color.White
  val dividerColor =
    if (isDarkTheme) colorResource(id = R.color.mine_shaft_gray600)
    else colorResource(id = R.color.mine_shaft_gray350)

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HelpTopAppBar(navController)
      }
    },
    containerColor = backgroundColor
  ) { innerPadding ->
    HelpContent(data, dividerColor, innerPadding)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpTopAppBar(navController: NavController) {
  // Retrieve the actionBarSize from the current theme
  val context = LocalContext.current
  val actionBarHeight = with(LocalDensity.current) {
    // Obtain the height defined in the theme (usually 56dp on phones)
    val styledAttributes =
      context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    styledAttributes.getDimension(0, 0f).toDp().also { styledAttributes.recycle() }
  }

  TopAppBar(
    modifier = Modifier.height(actionBarHeight), // set the height here
    title = {
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          modifier = Modifier.padding(
            start = dimensionResource(R.dimen.activity_horizontal_margin)
          ),
          text = stringResource(id = R.string.menu_help),
          color = Color.White,
          fontWeight = FontWeight.SemiBold
        )
      }
    },
    navigationIcon = {
      Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = navController::popBackStack) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back_Navigation",
            tint = Color.White
          )
        }
      }

    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Black
    )
  )
}

@Composable
fun HelpContent(
  data: List<HelpScreenItemDataClass>,
  dividerColor: Color,
  innerPadding: androidx.compose.foundation.layout.PaddingValues
) {
  Column(
    modifier = Modifier
      .padding(innerPadding)
  ) {
    SendReportRow()
    HelpItemList(data, dividerColor)
  }
}

@Composable
fun SendReportRow() {
  val context = LocalContext.current
  val isDarkTheme = isSystemInDarkTheme()

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
      contentDescription = stringResource(R.string.send_report),
      modifier = Modifier
        .padding(dimensionResource(R.dimen.activity_horizontal_margin))
    )

    Text(
      text = stringResource(R.string.send_report),
      color = if (isDarkTheme) Color.LightGray else Color.DarkGray,
      fontSize = SendDiagnosticReportFontSize
    )
  }
}

@Composable
fun HelpItemList(data: List<HelpScreenItemDataClass>, dividerColor: Color) {
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
  ) {
    itemsIndexed(data, key = { _, item -> item.title }) { _, item ->
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
