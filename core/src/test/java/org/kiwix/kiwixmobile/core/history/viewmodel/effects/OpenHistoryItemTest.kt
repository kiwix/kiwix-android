package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.history.viewmodel.createSimpleHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL

internal class OpenHistoryItemTest {
  @Test
  fun `invokeWith returns an Ok Result with historyUrl`() {
    val item = createSimpleHistoryItem()
    val zimReaderContainer: ZimReaderContainer = mockk()
    every {
      zimReaderContainer.zimCanonicalPath
    } returns "zimFilePath"
    val activity: AppCompatActivity = mockk()
    mockkConstructor(Intent::class)
    val intent: Intent = mockk()
    every {
      anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, item.historyUrl)
    } returns intent
    OpenHistoryItem(item, zimReaderContainer).invokeWith(activity)
    verify {
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
    }
  }

  @Test
  fun `invokeWith returns an Ok Result with historyUrl and zimFilePath`() {
    val item = createSimpleHistoryItem()
    val zimReaderContainer: ZimReaderContainer = mockk()
    every {
      zimReaderContainer.zimCanonicalPath
    } returns "notZimFilePath"
    val activity: AppCompatActivity = mockk()
    mockkConstructor(Intent::class)
    val intent: Intent = mockk()
    every {
      anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, item.historyUrl)
    } returns intent
    every {
      intent.putExtra(EXTRA_CHOSE_X_FILE, item.zimFilePath)
    } returns intent
    OpenHistoryItem(item, zimReaderContainer).invokeWith(activity)
    verify {
      intent.putExtra(EXTRA_CHOSE_X_FILE, item.zimFilePath)
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
    }
  }
}
