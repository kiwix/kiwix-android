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
import dagger.hilt.android.qualifiers.ApplicationContext
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
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.ShowTTSLanguageDownloadDialog
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

class ReadAloudManager @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val zimReaderContainer: ZimReaderContainer,
  private val kiwixDataStore: KiwixDataStore
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
    data object ShowTTSLanguageDownloadDialog : TtsState
  }

  private var ttsStateCallback: ((TtsState) -> Unit)? = null
  var tts: KiwixTextToSpeech? = null

  // This is for if the read aloud is currently reading the selected text inside webView.
  private var isReadSelection = false
  var currentTtsIndex: Int = 0
    private set

  fun setTtsStateCallback(callback: (TtsState) -> Unit) {
    ttsStateCallback = callback
  }

  private fun requireTtsStateCallback() = requireNotNull(ttsStateCallback) {
    "TtsState callback is not set. Set ReadAloudManager.setTtsStateCallback before using it"
  }

  private fun requireTts() = requireNotNull(tts) {
    "KiwixTextToSpeech is not initialized. Call ReadAloudManager.setUpTTS before using it"
  }

  private fun dispatchState(state: TtsState) {
    requireTtsStateCallback().invoke(state)
  }

  fun isTtsInitialed() = requireTts().isInitialized

  private val initListener = object : OnInitSucceedListener {
    override fun onInitSucceed() {
      dispatchState(if (isReadSelection) StartReadSelection else StartReadAloud)
    }
  }

  private val speakingListener = object : OnSpeakingListener {
    override fun onSpeakingStarted() {
      dispatchState(SpeakingStarted)
      setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
    }

    override fun onSpeakingEnded() {
      dispatchState(SpeakingEnded)
      setActionAndStartTTSService(ACTION_STOP_TTS)
    }
  }

  private val audioFocusChangedListener = OnAudioFocusChangeListener { focusChange: Int ->
    val tts = tts ?: return@OnAudioFocusChangeListener
    Log.d(TAG_KIWIX, "Focus change: $focusChange")
    tts.currentTTSTask?.let {
      tts.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
      return@OnAudioFocusChangeListener
    }
    when (focusChange) {
      AudioManager.AUDIOFOCUS_LOSS -> {
        if (tts.currentTTSTask?.paused == false) tts.pauseOrResume()
        dispatchState(AudioFocusLoss)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
      }

      AudioManager.AUDIOFOCUS_GAIN -> {
        dispatchState(AudioFocusGain)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
      }
    }
  }

  fun setUpTTS() {
    tts =
      KiwixTextToSpeech(
        context,
        initListener,
        speakingListener,
        audioFocusChangedListener,
        zimReaderContainer,
        kiwixDataStore
      )
  }

  fun initializeTTS(isReadSelection: Boolean) {
    this.isReadSelection = isReadSelection
    requireTts().initializeTTS()
  }

  fun initWebView(kiwixWebView: KiwixWebView) {
    tts?.initWebView(kiwixWebView)
  }

  fun stopReadAloudSafely() {
    runCatching {
      ttsStateCallback = null
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
    )
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
    requireTts().readAloud(kiwixWebView) {
      dispatchState(ShowTTSLanguageDownloadDialog)
    }
  }

  fun pauseTts() {
    val tts = requireTts()
    val task = tts.currentTTSTask
    if (task == null) {
      tts.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
      return
    }
    val wasPaused = task.paused
    tts.pauseOrResume()
    dispatchState(if (wasPaused) TtsResumed else TtsPaused)
    setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, !wasPaused)
  }

  fun seekTo(positionMs: Long) {
    tts?.seekTo(positionMs)
  }

  fun rewind10s() {
    tts?.rewind10s()
  }

  fun forward10s() {
    tts?.forward10s()
  }

  fun getAvailableVoices(): List<android.speech.tts.Voice> =
    tts?.getAvailableVoices().orEmpty()

  fun setVoiceByName(voiceName: String) {
    tts?.setVoiceByName(voiceName)
  }

  val currentPositionMs: Long
    get() = tts?.currentPositionMs ?: 0L

  val currentVoiceName: String?
    get() = tts?.currentVoiceName

  val totalDurationMs: Long
    get() = tts?.totalDurationMs ?: 0L

  fun stopReadAloud() {
    val tts = requireTts()
    tts.currentTTSTask?.let {
      tts.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
    }
  }
}
