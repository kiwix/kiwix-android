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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnInitSucceedListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnSpeakingListener
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.AudioFocusGain
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.AudioFocusLoss
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.SpeakingEnded
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.SpeakingStarted
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.StartReadAloud
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.StartReadSelection
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.TtsPaused
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.TtsResumed
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.Log
import javax.inject.Inject

class ReadAloudManager @Inject constructor(
  private val context: Context,
  private val zimReaderContainer: ZimReaderContainer
) {
  sealed interface TtsState {
    data object StartReadSelection : TtsState
    data object StartReadAloud : TtsState
    data object SpeakingStarted : TtsState
    data object SpeakingEnded : TtsState
    data object AudioFocusLoss : TtsState
    data object AudioFocusGain : TtsState
    data object TtsPaused : TtsState
    data object TtsResumed : TtsState
  }

  private var ttsStateCallback: ((TtsState) -> Unit)? = null
  var tts: KiwixTextToSpeech? = null
  private var isReadAloudServiceRunning = false

  // This is for if the read aloud is currently reading the selected text inside webView.
  private var isReadSelection = false
  var currentTtsIndex: Int = 0
    private set

  fun setTtsStateCallback(callback: (TtsState) -> Unit) {
    ttsStateCallback = callback
  }

  private fun requireTtsStateCallback() = requireNotNull(ttsStateCallback) {
    "TtsState callback is set. Set ReadAloudManager.setTtsStateCallback before using it"
  }

  private fun requireTts() = requireNotNull(tts) {
    "KiwixTextToSpeech is not initialized. Call ReadAloudManager.setUpTTS before using it"
  }

  fun isTtsInitialed() = requireTts().isInitialized

  fun setUpTTS() {
    tts =
      KiwixTextToSpeech(
        context,
        object : OnInitSucceedListener {
          override fun onInitSucceed() {
            if (isReadSelection) {
              requireTtsStateCallback().invoke(StartReadSelection)
            } else {
              requireTtsStateCallback().invoke(StartReadAloud)
            }
          }
        },
        object : OnSpeakingListener {
          override fun onSpeakingStarted() {
            requireTtsStateCallback().invoke(SpeakingStarted)
            setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
          }

          override fun onSpeakingEnded() {
            requireTtsStateCallback().invoke(SpeakingEnded)
            setActionAndStartTTSService(ACTION_STOP_TTS)
          }
        },
        OnAudioFocusChangeListener label@{ focusChange: Int ->
          if (tts != null) {
            Log.d(TAG_KIWIX, "Focus change: $focusChange")
            tts?.currentTTSTask?.let {
              tts?.stop()
              setActionAndStartTTSService(ACTION_STOP_TTS)
              return@label
            }
            when (focusChange) {
              AudioManager.AUDIOFOCUS_LOSS -> {
                if (tts?.currentTTSTask?.paused == false) tts?.pauseOrResume()
                requireTtsStateCallback().invoke(AudioFocusLoss)
                setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
              }

              AudioManager.AUDIOFOCUS_GAIN -> {
                requireTtsStateCallback().invoke(AudioFocusGain)
                setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
              }
            }
          }
        },
        zimReaderContainer
      )
  }

  fun initializeTTS(isReadSelection: Boolean) {
    this.isReadSelection = isReadSelection
    requireTts().initializeTTS()
  }

  fun initWebView(kiwixWebView: KiwixWebView) {
    requireTts().initWebView(kiwixWebView)
  }

  fun stopReadAloudSafely() {
    runCatching {
      ttsStateCallback = null
      isReadAloudServiceRunning = false
      tts?.apply {
        setActionAndStartTTSService(ACTION_STOP_TTS)
        shutdown()
        tts = null
      }
    }.onFailure {
      Log.e(
        TAG_KIWIX,
        "Could not stop read aloud service. Original exception = $it"
      )
    }
  }

  private fun setActionAndStartTTSService(action: String, isPauseTTS: Boolean = false) {
    context.startService(
      createReadAloudIntent(action, isPauseTTS)
    ).also {
      isReadAloudServiceRunning = action == ACTION_PAUSE_OR_RESUME_TTS
    }
  }

  private fun createReadAloudIntent(action: String, isPauseTTS: Boolean): Intent =
    Intent(context, ReadAloudService::class.java).apply {
      setAction(action)
      putExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        isPauseTTS
      )
    }

  fun readSelection(kiwixWebView: KiwixWebView) {
    requireTts().readSelection(kiwixWebView)
  }

  fun startReadAloud(kiwixWebView: KiwixWebView, index: Int) {
    currentTtsIndex = index
    requireTts().readAloud(kiwixWebView)
  }

  fun pauseTts() {
    if (tts?.currentTTSTask == null) {
      tts?.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
      return
    }
    tts?.currentTTSTask?.let {
      if (it.paused) {
        tts?.pauseOrResume()
        requireTtsStateCallback().invoke(TtsResumed)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
      } else {
        tts?.pauseOrResume()
        requireTtsStateCallback().invoke(TtsPaused)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
      }
    }
  }

  fun stopReadAloud() {
    requireTts().currentTTSTask?.let {
      requireTts().stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
    }
  }
}
