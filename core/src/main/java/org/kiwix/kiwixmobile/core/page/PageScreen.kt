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

package org.kiwix.kiwixmobile.core.page

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.Flow
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.bottomShadow
import org.kiwix.kiwixmobile.core.extensions.hideKeyboardOnLazyColumnScroll
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel

import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixSearchView
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOURTEEN_SP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.KIWIX_TOOLBAR_SHADOW_ELEVATION
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_SWITCH_LEFT_RIGHT_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.PAGE_SWITCH_ROW_BOTTOM_MARGIN
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.SIXTEEN_DP
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException

const val SWITCH_TEXT_TESTING_TAG = "switchTextTestingTag"
const val NO_ITEMS_TEXT_TESTING_TAG = "noItemsTextTestingTag"
const val PAGE_LIST_TEST_TAG = "pageListTestingTag"
const val SEARCH_ICON_TESTING_TAG = "searchIconTestingTag"
const val DELETE_MENU_ICON_TESTING_TAG = "deleteMenuIconTestingTag"

@Suppress("LongMethod", "LongParameterList")
@Composable
fun <T : Page, S : PageState<T>> PageScreenRoute(
  switchString: String,
  screenTitle: String,
  searchQueryHint: String,
  deleteIconTitle: Int,
  noItemsString: String,
  switchIsCheckedFlow: Flow<Boolean>,
  alertDialogShower: AlertDialogShower,
  navigateBack: () -> Unit,
  viewModel: PageViewModel<T, S>
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val activity = LocalActivity.current as CoreMainActivity

  var isSearchActive by rememberSaveable { mutableStateOf(false) }
  var searchText by rememberSaveable { mutableStateOf("") }
  val isInSelectionMode = state.isInSelectionState
  val selectedCount = state.pageItems.count { it.isSelected }

  LaunchedEffect(Unit) {
    viewModel.setAlertDialogShower(alertDialogShower)
    viewModel.setLifeCycleScope(activity.lifecycleScope)
  }

  viewModel.effects.CollectSideEffectWithActivity { effect, coreActivity ->
    @Suppress("UNCHECKED_CAST")
    (effect as SideEffect<CoreMainActivity>).invokeWith(coreActivity)
  }

  PageScreen(
    state = state,
    searchText = searchText,
    screenTitle = screenTitle,
    switchString = switchString,
    noItemsString = noItemsString,
    searchQueryHint = searchQueryHint,
    isSearchBarActive = isSearchActive,
    isInSelectionMode = isInSelectionMode,
    selectedCount = selectedCount,
    switchIsCheckedFlow = switchIsCheckedFlow,
    navigationIcon = {
      NavigationIcon(
        onClick = {
          if (isSearchActive) {
            isSearchActive = false
            searchText = ""
            viewModel.actions.tryEmit(Action.Filter(""))
          } else if (isInSelectionMode) {
            viewModel.actions.tryEmit(Action.ExitActionModeMenu)
          } else {
            navigateBack()
          }
        }
      )
    },
    actionMenuItems = actionMenuList(
      deleteIconTitle = deleteIconTitle,
      isSearchActive = isSearchActive,
      isInSelectionMode = isInSelectionMode,
      onSearchClick = {
        isSearchActive = true
      },
      onDeleteClick = {
        viewModel.actions.tryEmit(Action.UserClickedDeleteButton)
      },
      onSelectionDeleteClick = {
        viewModel.actions.tryEmit(Action.UserClickedDeleteSelectedPages)
      }
    ),
    onClearSearch = {
      searchText = ""
      viewModel.actions.tryEmit(Action.Filter(""))
    },
    onSearchTextChange = { newText ->
      searchText = newText
      viewModel.actions.tryEmit(Action.Filter(newText.trim()))
    },
    onSwitchCheckedChange = { isChecked ->
      viewModel.actions.tryEmit(Action.UserClickedShowAllToggle(isChecked))
    },
    onItemLongClick = { page ->
      viewModel.actions.tryEmit(Action.OnItemLongClick(page))
    },
    onItemClick = { page ->
      viewModel.actions.tryEmit(Action.OnItemClick(page))
    },
  )
}

@Suppress("ComposableLambdaParameterNaming", "LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Page, S : PageState<T>> PageScreen(
  state: S,
  searchText: String,
  switchString: String,
  screenTitle: String,
  searchQueryHint: String,
  noItemsString: String,
  isSearchBarActive: Boolean,
  isInSelectionMode: Boolean,
  selectedCount: Int,
  switchIsCheckedFlow: Flow<Boolean>,
  onItemClick: (Page) -> Unit,
  onItemLongClick: (Page) -> Unit,
  onSearchTextChange: (String) -> Unit,
  onSwitchCheckedChange: (Boolean) -> Unit,
  onClearSearch: () -> Unit,
  actionMenuItems: List<ActionMenuItem>,
  navigationIcon: @Composable () -> Unit
) {
  KiwixTheme {
    Scaffold(
      topBar = {
        Column {
          KiwixAppBar(
            title = if (isInSelectionMode) {
              stringResource(R.string.selected_items, selectedCount)
            } else {
              screenTitle
            },
            navigationIcon = navigationIcon,
            actionMenuItems = actionMenuItems,
            searchBar = searchBarIfActive(
              isSearchBarActive = isSearchBarActive,
              isInSelectionMode = isInSelectionMode,
              searchQueryHint = searchQueryHint,
              searchText = searchText,
              onSearchTextChange = onSearchTextChange,
              onClearSearch = onClearSearch
            )
          )
          if (!isInSelectionMode) {
            PageSwitchRow(
              switchString = switchString,
              switchIsEnabled = !state.isInSelectionState,
              switchIsCheckedFlow = switchIsCheckedFlow,
              onSwitchCheckedChange = onSwitchCheckedChange
            )
          }
        }
      }
    ) { padding ->
      val items = state.pageItems
      Box(
        modifier = Modifier
          .padding(
            top = padding.calculateTopPadding(),
            start = padding.calculateStartPadding(LocalLayoutDirection.current),
            end = padding.calculateEndPadding(LocalLayoutDirection.current)
          )
          .fillMaxSize()
      ) {
        if (items.isEmpty()) {
          Text(
            text = noItemsString,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
              .align(Alignment.Center)
              .semantics { testTag = NO_ITEMS_TEXT_TESTING_TAG }
          )
        } else {
          PageList(
            visiblePageItems = state.visiblePageItems,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick
          )
        }
      }
    }
  }
}

@Composable
private fun PageList(
  visiblePageItems: List<PageRelated>,
  onItemClick: (Page) -> Unit,
  onItemLongClick: (Page) -> Unit,
) {
  val listState = rememberLazyListState()
  LazyColumn(
    state = listState,
    modifier = Modifier
      .semantics { testTag = PAGE_LIST_TEST_TAG }
      // hides keyboard when scrolled
      .hideKeyboardOnLazyColumnScroll(listState)
  ) {
    itemsIndexed(visiblePageItems) { index, item ->
      when (item) {
        is Page -> PageListItem(
          index = index,
          page = item,
          onItemClick = onItemClick,
          onItemLongClick = onItemLongClick
        )

        is DateItem -> DateItemText(item)
      }
    }
  }
}

@Composable
private fun PageSwitchRow(
  switchString: String,
  switchIsEnabled: Boolean,
  switchIsCheckedFlow: Flow<Boolean>,
  onSwitchCheckedChange: (Boolean) -> Unit,
) {
  val context = LocalActivity.current as CoreMainActivity
  // hide switches for custom apps, see more info here https://github.com/kiwix/kiwix-android/issues/3523
  if (!context.isCustomApp()) {
    val isChecked by switchIsCheckedFlow.collectAsState(true)
    Surface(modifier = Modifier.bottomShadow(KIWIX_TOOLBAR_SHADOW_ELEVATION)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.onPrimary)
          .padding(bottom = PAGE_SWITCH_ROW_BOTTOM_MARGIN),
        horizontalArrangement = Arrangement.Absolute.Right,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = switchString,
          color = MaterialTheme.colorScheme.onBackground,
          style = TextStyle(fontSize = FOURTEEN_SP),
          modifier = Modifier.testTag(SWITCH_TEXT_TESTING_TAG)
        )
        Switch(
          checked = isChecked,
          onCheckedChange = onSwitchCheckedChange,
          enabled = switchIsEnabled,
          modifier = Modifier
            .padding(horizontal = PAGE_SWITCH_LEFT_RIGHT_MARGIN),
          colors = SwitchDefaults.colors(
            uncheckedTrackColor = White
          )
        )
      }
    }
  }
}

@Composable
fun DateItemText(dateItem: DateItem) {
  Text(
    text = getFormattedDateLabel(dateItem.dateString),
    style = MaterialTheme.typography.bodySmall,
    modifier = Modifier.padding(SIXTEEN_DP)
  )
}

@Composable
private fun getFormattedDateLabel(dateString: String): String {
  val today = LocalDate.now()
  val yesterday = today.minusDays(1)

  val parsedDate = parseDateSafely(dateString)
  return when (parsedDate) {
    today -> stringResource(R.string.time_today)
    yesterday -> stringResource(R.string.time_yesterday)
    else -> dateString
  }
}

private fun parseDateSafely(dateString: String): LocalDate? {
  return try {
    LocalDate.parse(dateString, DateTimeFormatter.ofPattern("d MMM yyyy"))
  } catch (_: DateTimeParseException) {
    null
  }
}

/**
 * Builds the list of action menu items for the app bar.
 *
 * @param isSearchActive Whether the search mode is currently active.
 * @param onSearchClick Callback to invoke when the search icon is clicked.
 * @param onDeleteClick Callback to invoke when the delete icon is clicked.
 * @return A list of [ActionMenuItem]s to be displayed in the app bar.
 *
 * - Shows the search icon only when search is not active.
 * - Always includes the delete icon, with a content description for accessibility (#3825).
 */
private fun actionMenuList(
  isSearchActive: Boolean,
  isInSelectionMode: Boolean,
  deleteIconTitle: Int,
  onSearchClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onSelectionDeleteClick: () -> Unit,
): List<ActionMenuItem> {
  if (isInSelectionMode) {
    return listOf(
      ActionMenuItem(
        icon = IconItem.Vector(Icons.Default.Delete),
        contentDescription = R.string.delete,
        onClick = onSelectionDeleteClick,
        testingTag = DELETE_MENU_ICON_TESTING_TAG
      )
    )
  }
  return listOfNotNull(
    when {
      !isSearchActive -> ActionMenuItem(
        icon = IconItem.Drawable(R.drawable.action_search),
        contentDescription = R.string.search_label,
        onClick = onSearchClick,
        testingTag = SEARCH_ICON_TESTING_TAG
      )

      else -> null
    },
    ActionMenuItem(
      icon = IconItem.Vector(Icons.Default.Delete),
      contentDescription = deleteIconTitle,
      onClick = onDeleteClick,
      testingTag = DELETE_MENU_ICON_TESTING_TAG
    )
  )
}

/**
 * Returns the search bar composable if the search is active and not in selection mode,
 * otherwise returns null.
 */
@Composable
private fun searchBarIfActive(
  isSearchBarActive: Boolean,
  isInSelectionMode: Boolean,
  searchQueryHint: String,
  searchText: String,
  onSearchTextChange: (String) -> Unit,
  onClearSearch: () -> Unit,
): (@Composable () -> Unit)? {
  return if (isSearchBarActive && !isInSelectionMode) {
    {
      KiwixSearchView(
        placeholder = searchQueryHint,
        value = searchText,
        searchViewTextFiledTestTag = "",
        onValueChange = onSearchTextChange,
        onClearClick = onClearSearch
      )
    }
  } else {
    null
  }
}
