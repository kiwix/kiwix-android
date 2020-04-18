package org.kiwix.kiwixmobile.core.history.viewmodel.effects

import android.annotation.TargetApi
import android.content.Intent
import android.os.Build.VERSION_CODES
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.history.viewmodel.Action
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.history.viewmodel.Action.ReceivedPromptForSpeechInput
import org.kiwix.kiwixmobile.core.utils.EXTRA_IS_WIDGET_VOICE
import org.kiwix.kiwixmobile.core.utils.EXTRA_SEARCH
import org.kiwix.kiwixmobile.core.utils.TAG_FROM_TAB_SWITCHER

data class SearchIntentProcessing(
  private val intent: Intent?,
  private val actions: PublishProcessor<Action>
) : SideEffect<Unit> {
  @TargetApi(VERSION_CODES.M)
  override fun invokeWith(activity: AppCompatActivity) {
    intent?.let {
      if (it.hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
        actions.offer(Filter(it.getStringExtra(Intent.EXTRA_PROCESS_TEXT)))
      }
      if (intent.hasExtra(EXTRA_SEARCH)) {
        actions.offer(Filter(intent.getStringExtra(EXTRA_SEARCH)))
      }
      if (intent.getBooleanExtra(EXTRA_IS_WIDGET_VOICE, false)) {
        actions.offer(ReceivedPromptForSpeechInput)
      }
    }
  }
}
