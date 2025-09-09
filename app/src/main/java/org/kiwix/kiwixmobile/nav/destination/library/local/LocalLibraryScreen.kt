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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.main.reader.CONTENT_LOADING_PROGRESSBAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.reader.OnBackPressed
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.KiwixSnackbarHost
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.ui.components.SwipeRefreshLayout
import org.kiwix.kiwixmobile.core.ui.theme.Black
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.ui.BookItem
import org.kiwix.kiwixmobile.ui.ZimFilesLanguageHeader
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState

const val NO_FILE_TEXT_TESTING_TAG = "noFileTextTestingTag"
const val DOWNLOAD_BUTTON_TESTING_TAG = "downloadButtonTestingTag"
const val BOOK_LIST_TESTING_TAG = "bookListTestingTag"
const val SELECT_FILE_BUTTON_TESTING_TAG = "selectFileButtonTestingTag"
const val SHOW_SWIPE_DOWN_TO_SCAN_FILE_SYSTEM_TEXT_TESTING_TAG =
  "showSwipeDownToScanFileSystemTextTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming", "LongParameterList")
@Composable
fun LocalLibraryScreen(
  state: LocalLibraryScreenState,
  listState: LazyListState,
  onRefresh: () -> Unit,
  onDownloadButtonClick: () -> Unit,
  fabButtonClick: () -> Unit,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
  bottomAppBarScrollBehaviour: BottomAppBarScrollBehavior?,
  onUserBackPressed: () -> FragmentActivityExtensions.Super,
  navHostController: NavHostController,
  navigationIcon: @Composable () -> Unit
) {
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
  KiwixTheme {
    Scaffold(
      snackbarHost = { KiwixSnackbarHost(snackbarHostState = state.snackBarHostState) },
      topBar = {
        KiwixAppBar(
          title = stringResource(R.string.library),
          navigationIcon = navigationIcon,
          actionMenuItems = state.actionMenuItems,
          topAppBarScrollBehavior = scrollBehavior
        )
      },
      bottomBar = { ShowSwipeDownToScanFileSystemText() },
      floatingActionButton = { SelectFileButton(fabButtonClick) },
      modifier = Modifier
        .systemBarsPadding()
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .let { baseModifier ->
          bottomAppBarScrollBehaviour?.let {
            baseModifier.nestedScroll(it.nestedScrollConnection)
          } ?: baseModifier
        }
    ) { contentPadding ->
      SwipeRefreshLayout(
        isRefreshing = state.swipeRefreshItem.first,
        isEnabled = state.swipeRefreshItem.second,
        onRefresh = onRefresh,
        modifier = Modifier
          .fillMaxSize()
          .padding(contentPadding)
      ) {
        OnBackPressed(onUserBackPressed, navHostController)
        if (state.scanningProgressItem.first) {
          ContentLoadingProgressBar(
            modifier = Modifier.testTag(CONTENT_LOADING_PROGRESSBAR_TESTING_TAG),
            progressBarStyle = ProgressBarStyle.HORIZONTAL,
            progress = state.scanningProgressItem.second
          )
        }
        if (state.noFilesViewItem.third || state.fileSelectListState.bookOnDiskListItems.isEmpty()) {
          NoFilesView(state.noFilesViewItem, onDownloadButtonClick)
        } else {
          BookItemList(
            state.fileSelectListState,
            onClick,
            onLongClick,
            onMultiSelect,
            listState
          )
        }
      }
    }
  }
}

@Composable
private fun ShowSwipeDownToScanFileSystemText() {
  Surface(
    color = MaterialTheme.colorScheme.onPrimary,
    tonalElevation = ONE_DP
  ) {
    Text(
      text = stringResource(string.swipe_down_to_scan_storage),
      modifier = Modifier
        .fillMaxWidth()
        .padding(EIGHT_DP)
        .semantics { testTag = SHOW_SWIPE_DOWN_TO_SCAN_FILE_SYSTEM_TEXT_TESTING_TAG },
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    )
  }
}

@Composable
private fun BookItemList(
  state: FileSelectListState,
  onClick: ((BookOnDisk) -> Unit)? = null,
  onLongClick: ((BookOnDisk) -> Unit)? = null,
  onMultiSelect: ((BookOnDisk) -> Unit)? = null,
  lazyListState: LazyListState,
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag(BOOK_LIST_TESTING_TAG),
    state = lazyListState
  ) {
    itemsIndexed(state.bookOnDiskListItems) { index, bookItem ->
      when (bookItem) {
        is BooksOnDiskListItem.LanguageItem -> {
          ZimFilesLanguageHeader(bookItem)
        }

        is BookOnDisk -> {
          BookItem(
            index = index,
            bookOnDisk = bookItem,
            selectionMode = state.selectionMode,
            onClick = onClick,
            onLongClick = onLongClick,
            onMultiSelect = onMultiSelect
          )
        }
      }
    }
  }
}

@Composable
private fun SelectFileButton(fabButtonClick: () -> Unit) {
  FloatingActionButton(
    onClick = fabButtonClick,
    modifier = Modifier
      .testTag(SELECT_FILE_BUTTON_TESTING_TAG),
    containerColor = Black,
    shape = MaterialTheme.shapes.extraLarge
  ) {
    Icon(
      painter = painterResource(id = R.drawable.ic_add_blue_24dp),
      contentDescription = stringResource(id = string.select_zim_file),
      tint = White
    )
  }
}

@Composable
fun NoFilesView(
  noFilesViewItem: Triple<String, String, Boolean>,
  onDownloadButtonClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = FOUR_DP)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      modifier = Modifier.testTag(NO_FILE_TEXT_TESTING_TAG),
      text = noFilesViewItem.first,
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(EIGHT_DP))
    KiwixButton(
      buttonText = noFilesViewItem.second,
      clickListener = onDownloadButtonClick,
      modifier = Modifier.testTag(DOWNLOAD_BUTTON_TESTING_TAG)
    )
  }
}
