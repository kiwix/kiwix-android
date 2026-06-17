/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library

import android.app.Application
import android.os.Build
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.ui.components.CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.nav.destination.library.online.CATEGORY_ITEM_RADIO_BUTTON_TESTING_TAG
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineCategoryDialogScreen
import org.kiwix.kiwixmobile.nav.destination.library.online.toSentenceCaseCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class OnlineCategoryDialogScreenTest {
  @Rule
  @JvmField
  val composeRule = createComposeRule()

  private lateinit var viewModel: CategoryViewModel

  private val stateFlow = MutableStateFlow<State>(State.Loading)
  private val effectsFlow = MutableSharedFlow<SideEffect<*>>()

  @Before
  fun setup() {
    viewModel = mockk(relaxed = true)

    every { viewModel.state } returns stateFlow
    every { viewModel.effects } returns effectsFlow
  }

  private fun setDialogScreen() {
    composeRule.setContent {
      OnlineCategoryDialogScreen(
        categoryViewModel = viewModel,
        navigationIcon = {}
      )
    }
  }

  private fun getContent(categoryList: List<Category>) =
    State.Content(items = categoryList)

  @Test
  fun showsErrorMessage_whenStateIsError() {
    stateFlow.value = State.Error("Network error")
    setDialogScreen()
    composeRule
      .onNodeWithText("Network error")
      .assertIsDisplayed()
  }

  @Test
  fun showsCategoryHeaders_whenStateIsContent() {
    stateFlow.value = getContent(
      listOf(
        Category(
          id = 1,
          active = true,
          category = "science"
        ),
        Category(
          id = 2,
          active = false,
          category = "history"
        )
      )
    )

    val context = ApplicationProvider.getApplicationContext<Application>()

    setDialogScreen()

    composeRule
      .onNodeWithText(context.getString(R.string.your_selected_category))
      .assertIsDisplayed()

    composeRule
      .onNodeWithText(context.getString(R.string.other_categories))
      .assertIsDisplayed()
  }

  @Test
  fun showsCategories_whenStateIsContent() {
    stateFlow.value = getContent(
      listOf(
        Category(
          id = 1,
          active = true,
          category = "science"
        ),
        Category(
          id = 2,
          active = false,
          category = "history"
        )
      )
    )
    setDialogScreen()

    composeRule
      .onNodeWithText("Science")
      .assertIsDisplayed()

    composeRule
      .onNodeWithText("History")
      .assertIsDisplayed()
  }

  @Test
  fun showsLoadingIndicator_whenStateIsLoading() {
    stateFlow.value = State.Loading

    setDialogScreen()

    composeRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun showsLoadingIndicator_whenStateIsSaving() {
    stateFlow.value = State.Saving(
      getContent(
        listOf(
          Category(
            id = 1,
            active = true,
            category = "science"
          )
        )
      )
    )

    setDialogScreen()

    composeRule
      .onNodeWithTag(CONTENT_LOADING_PROGRESS_BAR_TESTING_TAG)
      .assertExists()
  }

  @Test
  fun selectsRadioButton_whenCategoryIsActive() {
    stateFlow.value = getContent(
      listOf(Category(id = 1, active = true, category = "science"))
    )

    setDialogScreen()

    composeRule
      .onNodeWithTag(
        "${CATEGORY_ITEM_RADIO_BUTTON_TESTING_TAG}science",
        useUnmergedTree = true
      ).assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Selected,
          true
        )
      )
  }

  @Test
  fun deselectsRadioButton_whenCategoryIsInactive() {
    stateFlow.value = getContent(
      listOf(
        Category(
          id = 1,
          active = false,
          category = "science"
        )
      )
    )

    setDialogScreen()

    composeRule
      .onNodeWithTag(
        "${CATEGORY_ITEM_RADIO_BUTTON_TESTING_TAG}science",
        useUnmergedTree = true
      )
      .assert(
        SemanticsMatcher.expectValue(
          SemanticsProperties.Selected,
          false
        )
      )
  }

  @Test
  fun emitsSelectAction_whenCategoryIsClicked() {
    val category = Category(
      id = 1,
      active = false,
      category = "science"
    )

    stateFlow.value = getContent(listOf(category))
    setDialogScreen()

    composeRule
      .onNodeWithText("Science")
      .performClick()

    verify {
      viewModel.actions.tryEmit(
        CategoryViewModel.Action.Select(CategoryListItem.CategoryItem(category))
      )
    }
  }

  @Test
  fun `toSentenceCaseCategory converts underscore text`() {
    assertThat("computer_science".toSentenceCaseCategory()).isEqualTo("Computer science")
  }

  @Test
  fun `toSentenceCaseCategory converts single word`() {
    assertThat("history".toSentenceCaseCategory()).isEqualTo("History")
  }

  @Test
  fun `toSentenceCaseCategory handles empty string`() {
    assertThat("".toSentenceCaseCategory()).isEqualTo("")
  }
}
