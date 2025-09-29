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

package org.kiwix.kiwixmobile.nav.destination.library.online

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.extensions.CollectSideEffectWithActivity
import org.kiwix.kiwixmobile.core.ui.components.KiwixAppBar
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens
import org.kiwix.kiwixmobile.language.LoadingScreen
import org.kiwix.kiwixmobile.language.ShowErrorMessage
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Select
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.CategoryItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem.HeaderItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Error
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Saving

const val CATEGORY_ITEM_RADIO_BUTTON_TESTING_TAG = "categoryItemRadioButtonTestingTag"

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("ComposableLambdaParameterNaming")
@Composable
fun OnlineCategoryDialogScreen(
  categoryViewModel: CategoryViewModel,
  navigationIcon: @Composable () -> Unit
) {
  val state by categoryViewModel.state.collectAsState(Loading)
  val context = LocalContext.current
  categoryViewModel.effects.CollectSideEffectWithActivity { effect, activity ->
    effect.invokeWith(activity)
  }
  KiwixDialogTheme {
    Scaffold(
      topBar = {
        KiwixAppBar(
          title = stringResource(R.string.select_category),
          navigationIcon = navigationIcon
        )
      }
    ) { paddingValues ->
      Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        when (state) {
          Loading, Saving -> {
            LoadingScreen()
          }

          is Content -> {
            CategoryList(
              state = state,
              context = context,
              selectCategoryItem = { categoryItem ->
                categoryViewModel.actions.tryEmit(Select(categoryItem))
              }
            )
          }

          is Error -> ShowErrorMessage((state as Error).errorMessage)
        }
      }
    }
  }
}

@Composable
private fun CategoryList(
  state: State,
  context: Context,
  selectCategoryItem: (CategoryListItem.CategoryItem) -> Unit
) {
  val viewItem = (state as Content).viewItems

  LazyColumn {
    items(
      items = viewItem,
      key = { item ->
        when (item) {
          is HeaderItem -> "header_${item.id}"
          is CategoryItem -> "language_${item.category.id}"
        }
      }
    ) { item ->
      when (item) {
        is HeaderItem -> CategoryHeaderText(
          item = item,
          modifier = Modifier.animateItem()
        )

        is CategoryItem -> CategoryItemRow(
          context = context,
          modifier = Modifier
            .animateItem()
            .fillMaxWidth()
            .height(ComposeDimens.SIXTY_FOUR_DP)
            .semantics {
              contentDescription =
                context.getString(R.string.select_category_content_description)
            }
            .clickable {
              selectCategoryItem(item)
            },
          item = item,
          onCheckedChange = { selectCategoryItem(it) }
        )
      }
    }
  }
}

@Composable
fun CategoryHeaderText(modifier: Modifier, item: HeaderItem) {
  Text(
    text = when (item.id) {
      HeaderItem.SELECTED -> stringResource(R.string.your_selected_category)

      HeaderItem.OTHER -> stringResource(R.string.other_categories)
      else -> ""
    },
    modifier = modifier
      .padding(horizontal = ComposeDimens.SIXTEEN_DP, vertical = ComposeDimens.EIGHT_DP),
    fontSize = ComposeDimens.FOURTEEN_SP,
    style = MaterialTheme.typography.headlineMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
  )
}

@Composable
fun CategoryItemRow(
  context: Context,
  modifier: Modifier,
  item: CategoryItem,
  onCheckedChange: (CategoryItem) -> Unit
) {
  val category = item.category
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      modifier = Modifier
        .padding(ComposeDimens.SIXTEEN_DP)
        .semantics {
          testTag = "$CATEGORY_ITEM_RADIO_BUTTON_TESTING_TAG${category.category}"
        },
      selected = category.active,
      onClick = {
        onCheckedChange(item)
      }
    )
    Text(
      text = category.category.ifEmpty { context.getString(R.string.all_categories) },
      style = MaterialTheme.typography.bodyLarge
    )
  }
}
