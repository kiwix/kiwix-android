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

package org.kiwix.kiwixmobile.webserver

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MATERIAL_MINIMUM_HEIGHT_AND_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MAXIMUM_HEIGHT_OF_QR_CODE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_BOOKS_LIST
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_QR_CODE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem

@Suppress("ComposableLambdaParameterNaming")
@Composable
fun ZimHostScreen(
  serverIpText: String,
  shareIconItem: Pair<MutableState<Boolean>, () -> Unit>,
  qrImageItem: Pair<MutableState<Boolean>, IconItem>,
  booksList: List<BooksOnDiskListItem>,
  startServerButtonItem: Pair<String, Color>,
  navigationIcon: @Composable () -> Unit
) {
  KiwixTheme {
    Scaffold(topBar = {
      KiwixAppBar(R.string.menu_wifi_hotspot, navigationIcon)
    }) { contentPadding ->
      Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = SIXTEEN_DP),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ServerIpText(serverIpText, Modifier.weight(1f))
          ShareIcon(shareIconItem)
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
          QRImage(qrImageItem)
          BookItemList(booksList)
        }
        KiwixButton(
          startServerButtonItem.first,
          {},
          modifier = Modifier.fillMaxWidth(),
          buttonBackgroundColor = startServerButtonItem.second
        )
      }
    }
  }
}

@Composable
fun ServerIpText(
  serverIpText: String,
  modifier: Modifier
) {
  Text(
    text = serverIpText,
    modifier = modifier.minimumInteractiveComponentSize(),
    textAlign = TextAlign.Start,
  )
}

@Composable
fun ShareIcon(shareIconItem: Pair<MutableState<Boolean>, () -> Unit>) {
  if (shareIconItem.first.value) {
    Image(
      painter = painterResource(id = R.drawable.ic_share_35dp),
      contentDescription = stringResource(id = R.string.share_host_address),
      modifier = Modifier
        .clickable { shareIconItem.second.invoke() }
        .padding(4.dp)
        .heightIn(min = MATERIAL_MINIMUM_HEIGHT_AND_WIDTH)
        .widthIn(min = MATERIAL_MINIMUM_HEIGHT_AND_WIDTH),
      contentScale = ContentScale.Inside
    )
  }
}

@Composable
fun QRImage(qrImageItem: Pair<MutableState<Boolean>, IconItem>) {
  Image(
    painter = qrImageItem.second.toPainter(),
    contentDescription = stringResource(id = R.string.qr_code),
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = MINIMUM_HEIGHT_OF_QR_CODE, max = MAXIMUM_HEIGHT_OF_QR_CODE)
      .padding(horizontal = SIXTEEN_DP),
    contentScale = ContentScale.Fit
  )
}

@Suppress("UnusedParameter")
@Composable
fun BookItemList(booksList: List<BooksOnDiskListItem>) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .heightIn(min = MINIMUM_HEIGHT_OF_BOOKS_LIST),
    content = {
    }
  )
}

// @Preview
// @Preview(name = "DarkMode", uiMode = Configuration.UI_MODE_NIGHT_YES)
// @Composable
// fun PreviewScreen() {
//   val shareIconVisibility = remember { mutableStateOf(true) }
//   ZimHostScreen(
//     stringResource(R.string.server_textview_default_message),
//     shareIconVisibility to {},
//     shareIconVisibility to IconItem.Drawable(org.kiwix.kiwixmobile.R.drawable.launch_screen),
//     listOf(),
//     stringResource(R.string.start_server_label) to StartServerGreen,
//     {
//       NavigationIcon(onClick = {})
//     }
//   )
// }
