package org.kiwix.kiwixmobile.core.history.viewmodel

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.DateItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.SearchResultGenerator
import org.kiwix.kiwixmobile.core.history.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.history.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler
import java.util.concurrent.TimeUnit.MILLISECONDS

@ExtendWith(InstantExecutorExtension::class)
internal class HistoryViewModelTest {
  private val historyDao: HistoryDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val searchResultGenerator: SearchResultGenerator = mockk()

  lateinit var viewModel: HistoryViewModel
  private val testScheduler = TestScheduler()

  init {
    setScheduler(testScheduler)
  }

  private val itemsFromDb: PublishProcessor<List<HistoryItem>> =
    PublishProcessor.create()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showHistoryCurrentBook } returns true
    every { historyDao.history() } returns itemsFromDb.distinctUntilChanged()
    viewModel = HistoryViewModel(historyDao, zimReaderContainer, sharedPreferenceUtil)
  }

  private fun resultsIn(st: State) {
    viewModel.state.test()
      .also { testScheduler.advanceTimeBy(100, MILLISECONDS) }
      .assertValue(st)
  }

  private fun emissionOf(
    searchTerm: String,
    databaseResults: List<HistoryItem>
  ) {
    itemsFromDb.offer(databaseResults)
    viewModel.actions.offer(Filter(searchTerm))
  }

  @Nested
  inner class StateTests {

    @Test
    fun `initial state is Initialising`() {
      viewModel.state.test().assertValue(NoResults(listOf()))
    }

    @Test
    fun `non empty search term with search results shows Results`() {
      val searchTerm = "searchTerm"
      val item = mockkedHistoryItem(historyTitle = searchTerm)
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = searchTerm,
        databaseResults = listOf(item)
      )
      resultsIn(Results((listOf(date, item))))
    }

    @Test
    fun `non empty search string with no search results is NoResults`() {
      emissionOf(
        searchTerm = "a",
        databaseResults = listOf(mockkedHistoryItem(""))
      )
      resultsIn(NoResults(emptyList()))
    }

    @Test
    fun `empty search string with database results shows Results`() {
      val item = mockkedHistoryItem("")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(date, item)))
    }

    @Test
    fun `empty search string with no database results is NoResults`() {
      emissionOf(
        searchTerm = "",
        databaseResults = emptyList()
      )
      resultsIn(NoResults(emptyList()))
    }

    @Test
    fun `duplicate search terms are ignored`() {
      val searchString1 = "a"
      val searchString2 = "b"
      val item = mockkedHistoryItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = searchString1,
        databaseResults = listOf(item)
      )
      viewModel.actions.offer(Filter(searchString2))
      resultsIn(Results(listOf(date, item)))
    }

    @Test
    fun `only latest search term is used`() {
      val item = mockkedHistoryItem("b")
      val date = DateItem(item.dateString)
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(date, item)))
    }

    @Test
    fun `order of date headers and items are correct`() {
      val item1 = mockkedHistoryItem("a", "1 Aug 2020")
      val date1 = DateItem(item1.dateString)
      val item2 = mockkedHistoryItem("b", "2 Jun 1990")
      val date2 = DateItem(item2.dateString)
      val item3 = mockkedHistoryItem("c", "1 Aug 2021")
      val date3 = DateItem(item3.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item2, item3, item1)
      )
      resultsIn(Results(listOf(date3, item3, date1, item1, date2, item2)))
    }

    @Test
    fun `date headers are merged if on same day`() {
      val item1 = mockkedHistoryItem("a", "1 Aug 2020")
      val date1 = DateItem(item1.dateString)
      val item2 = mockkedHistoryItem("b", "1 Aug 2020")
      val item3 = mockkedHistoryItem("c", "1 Aug 2019")
      val date3 = DateItem(item3.dateString)
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item2, item3, item1)
      )
      resultsIn(Results(listOf(date1, item2, item1, date3, item3)))
    }
  }

  // dateFormat = d MMM yyyy
  //             5 Jul 2020
  private fun mockkedHistoryItem(
    historyTitle: String = "historyTitle",
    dateString: String = "5 Jul 2020"
  ): HistoryItem {
    val item = mockk<HistoryItem>()
    every { item.historyTitle } returns historyTitle
    every { item.zimName } returns "zimName"
    every { item.zimId } returns "zimId"
    every { item.zimFilePath } returns "zimFilePath"
    every { item.historyUrl } returns "historyUrl"
    every { item.dateString } returns dateString
    every { item.id } returns 5
    every { item.isSelected } returns false
    return item
//    every { item.zim } returns "zimName"
  }
}
