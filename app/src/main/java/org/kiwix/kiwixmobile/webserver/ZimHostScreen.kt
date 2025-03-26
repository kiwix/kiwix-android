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

import android.content.Context
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.util.LinkifyCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MATERIAL_MINIMUM_HEIGHT_AND_WIDTH
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MAXIMUM_HEIGHT_OF_QR_CODE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_BOOKS_LIST
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.MINIMUM_HEIGHT_OF_QR_CODE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.ui.BookItem
import org.kiwix.kiwixmobile.ui.ZimFilesLanguageHeader

const val START_SERVER_BUTTON_TESTING_TAG = "startServerButtonTestingTag"
const val QR_IMAGE_TESTING_TAG = "qrImageTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongParameterList")
@Composable
fun ZimHostScreen(
  serverIpText: String,
  shareIconItem: Pair<Boolean, () -> Unit>,
  qrImageItem: Pair<Boolean, IconItem>,
  booksList: List<BooksOnDiskListItem>,
  startServerButtonItem: Triple<String, Color, () -> Unit>,
  selectionMode: SelectionMode,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
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
          ServerIpText(serverIpText, Modifier.weight(1f), LocalContext.current)
          ShareIcon(shareIconItem)
        }
        Box(modifier = Modifier.weight(1f)) {
          BookItemList(
            booksList,
            selectionMode,
            qrImageItem,
            onClick,
            onLongClick,
            onMultiSelect
          )
        }
        KiwixButton(
          startServerButtonItem.first,
          startServerButtonItem.third,
          modifier = Modifier
            .fillMaxWidth()
            .padding(FOUR_DP)
            .testTag(START_SERVER_BUTTON_TESTING_TAG),
          buttonBackgroundColor = startServerButtonItem.second
        )
      }
    }
  }
}

@Suppress("MagicNumber")
@Composable
private fun ServerIpText(
  serverIpText: String,
  modifier: Modifier,
  context: Context
) {
  val serverIpTextView = remember { TextView(context) }
  AndroidView(factory = { serverIpTextView }, modifier = modifier) { textView ->
    textView.apply {
      text = serverIpText
      textSize = 14F
      minHeight = context.resources.getDimensionPixelSize(R.dimen.material_minimum_height_and_width)
      gravity = Gravity.CENTER or Gravity.START
      LinkifyCompat.addLinks(this, Linkify.WEB_URLS)
      movementMethod = LinkMovementMethod.getInstance()
    }
  }
}

@Composable
private fun ShareIcon(shareIconItem: Pair<Boolean, () -> Unit>) {
  if (shareIconItem.first) {
    Image(
      painter = painterResource(id = R.drawable.ic_share_35dp),
      contentDescription = stringResource(id = R.string.share_host_address),
      modifier = Modifier
        .clickable { shareIconItem.second.invoke() }
        .padding(FOUR_DP)
        .heightIn(min = MATERIAL_MINIMUM_HEIGHT_AND_WIDTH)
        .widthIn(min = MATERIAL_MINIMUM_HEIGHT_AND_WIDTH),
      contentScale = ContentScale.Inside
    )
  }
}

@Composable
private fun QRImage(qrImageItem: Pair<Boolean, IconItem>) {
  if (qrImageItem.first) {
    Image(
      painter = qrImageItem.second.toPainter(),
      contentDescription = stringResource(id = R.string.qr_code),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = MINIMUM_HEIGHT_OF_QR_CODE, max = MAXIMUM_HEIGHT_OF_QR_CODE)
        .padding(horizontal = SIXTEEN_DP)
        .testTag(QR_IMAGE_TESTING_TAG),
      contentScale = ContentScale.Fit
    )
  }
}

@Composable
private fun BookItemList(
  booksList: List<BooksOnDiskListItem>,
  selectionMode: SelectionMode,
  qrImageItem: Pair<Boolean, IconItem>,
  onClick: ((BookOnDisk) -> Unit)?,
  onLongClick: ((BookOnDisk) -> Unit)?,
  onMultiSelect: ((BookOnDisk) -> Unit)?
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .heightIn(min = MINIMUM_HEIGHT_OF_BOOKS_LIST),
    content = {
      // Adding QR image here because of when we scrolls then QR image will go up.
      item {
        QRImage(qrImageItem)
      }
      itemsIndexed(booksList) { index, bookItem ->
        when (bookItem) {
          is BooksOnDiskListItem.LanguageItem -> {
            ZimFilesLanguageHeader(bookItem)
          }

          is BookOnDisk -> {
            BookItem(
              index = index,
              bookOnDisk = bookItem,
              selectionMode = selectionMode,
              onClick = onClick,
              onLongClick = onLongClick,
              onMultiSelect = onMultiSelect
            )
          }
        }
      }
    }
  )
}
