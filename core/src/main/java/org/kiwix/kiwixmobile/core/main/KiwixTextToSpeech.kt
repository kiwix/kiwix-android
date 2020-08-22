/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.main

import android.content.Context
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.iSO3ToLocale
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import java.util.HashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Constructor.
 *
 * @param context the context to create TextToSpeech with
 * @param onInitSucceedListener listener that receives event when initialization of TTS is done
 * (and does not receive if it failed)
 * @param onSpeakingListener listener that receives an event when speaking just started or
 */
class KiwixTextToSpeech internal constructor(
  val context: Context,
  onInitSucceedListener: OnInitSucceedListener,
  val onSpeakingListener: OnSpeakingListener,
  private val onAudioFocusChangeListener: OnAudioFocusChangeListener,
  private val zimReaderContainer: ZimReaderContainer
) {
  private val focusLock: Any = Any()
  private val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  @JvmField var currentTTSTask: TTSTask? = null
  private var tts: TextToSpeech? = null

  /**
   * Returns whether the TTS is initialized.
   *
   * @return `true` if TTS is initialized; `false` otherwise
   */
  private var isInitialized = false

  init {
    Log.d(TAG_KIWIX, "Initializing TextToSpeech")
    initTTS(onInitSucceedListener)
  }

  private fun initTTS(onInitSucceedListener: OnInitSucceedListener) {
    tts = TextToSpeech(instance, OnInitListener { status: Int ->
      if (status == TextToSpeech.SUCCESS) {
        Log.d(TAG_KIWIX, "TextToSpeech was initialized successfully.")
        this.isInitialized = true
        onInitSucceedListener.onInitSucceed()
      } else {
        Log.e(TAG_KIWIX, "Initialization of TextToSpeech Failed!")
        context.toast(R.string.texttospeech_initialization_failed, Toast.LENGTH_SHORT)
      }
    })
  }

  /**
   * Reads the currently selected text in the WebView.
   */
  fun readSelection(webView: WebView) {
    webView.loadUrl("javascript:tts.speakAloud(window.getSelection().toString());", null)
  }

  /**
   * Starts speaking the WebView content aloud (or stops it if TTS is speaking now).
   */
  fun readAloud(webView: WebView) {
    val isPaused = currentTTSTask?.paused
    val isSpeaking = tts?.isSpeaking
    if (isPaused == true) {
      onSpeakingListener.onSpeakingEnded()
      currentTTSTask = null
    } else if (isSpeaking == true) {
      if (tts!!.stop() == TextToSpeech.SUCCESS) {
        tts!!.setOnUtteranceProgressListener(null)
        onSpeakingListener.onSpeakingEnded()
      }
    } else {
      val locale = iSO3ToLocale(zimReaderContainer.language)
      var result = 0
      if ("mul" == zimReaderContainer.language) {
        Log.d(
          TAG_KIWIX, "TextToSpeech: disabled " +
            zimReaderContainer.language
        )
        context.toast(R.string.tts_not_enabled, Toast.LENGTH_LONG)
        return
      }
      if (locale == null || tts?.isLanguageAvailable(locale)
          .also {
            it?.let {
              result = it
            }
          } == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED
      ) {
        Log.d(
          TAG_KIWIX, "TextToSpeech: language not supported: " +
            zimReaderContainer.language
        )
        context.toast(R.string.tts_lang_not_supported, Toast.LENGTH_LONG)
      } else {
        tts?.language = locale
        if (getFeatures(tts).contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
          context.toast(R.string.tts_lang_not_supported, Toast.LENGTH_LONG)
          return
        }
        if (requestAudioFocus()) {
          loadURL(webView)
        }
      }
    }
  }

  private fun getFeatures(
    tts: TextToSpeech?
  ): Set<String> = tts!!.voice.features

  private fun loadURL(webView: WebView) {
    // We use JavaScript to get the content of the page conveniently, earlier making some
    // changes in the page
    webView.loadUrl(
      """
        javascript:body = document.getElementsByTagName('body')[0].cloneNode(true);
        // Remove some elements that are shouldn't be read (table of contents,
        // references numbers, thumbnail captions, duplicated title, etc.)
        "toRemove = body.querySelectorAll('sup.reference, #toc, .thumbcaption, 
        "    title, .navbox');
        "Array.prototype.forEach.call(toRemove, function(elem) {
        "    elem.parentElement.removeChild(elem);
        "});
        "tts.speakAloud(body.innerText);
        """
    )
  }

  fun stop() {
    tts?.let {
      {
        if (it.stop() == TextToSpeech.SUCCESS) {
          currentTTSTask = null
          it.setOnUtteranceProgressListener(null)
          onSpeakingListener.onSpeakingEnded()
        }
      }
    }
  }

  private fun requestAudioFocus(): Boolean {
    val audioFocusRequest = am.requestAudioFocus(
      onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
      AudioManager.AUDIOFOCUS_GAIN
    )
    Log.d(TAG_KIWIX, "Audio Focus Requested")
    synchronized(focusLock) {
      return@requestAudioFocus audioFocusRequest ==
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  fun pauseOrResume() {
    if (currentTTSTask == null) {
      return
    }
    if (currentTTSTask!!.paused) {
      if (!requestAudioFocus()) return
      currentTTSTask!!.start()
    } else {
      currentTTSTask!!.pause()
    }
  }

  fun initWebView(webView: WebView) {
    webView.addJavascriptInterface(TTSJavaScriptInterface(), "tts")
  }

  /**
   * Releases the resources used by the engine.
   *
   * @see android.speech.tts.TextToSpeech.shutdown
   */
  fun shutdown() {
    tts?.shutdown()
  }

  /**
   * The listener which is notified when initialization of the TextToSpeech engine is successfully
   * done.
   */
  internal interface OnInitSucceedListener {
    fun onInitSucceed()
  }

  /**
   * The listener that is notified when speaking starts or stops (regardless of whether it was a
   * result of error, user, or because whole text was read).
   *
   *
   * Note that the methods of this interface may not be called from the UI thread.
   */
  interface OnSpeakingListener {
    fun onSpeakingStarted()
    fun onSpeakingEnded()
  }

  inner class TTSTask
    (private val pieces: List<String>) {
    private val currentPiece =
      AtomicInteger(0)
    @JvmField var paused = true
    fun pause() {
      paused = true
      currentPiece.decrementAndGet()
      tts?.let { tts ->
        {
          tts.setOnUtteranceProgressListener(null)
          tts.stop()
        }
      }
    }

    fun start() {
      paused = if (!paused) {
        return
      } else {
        false
      }
      val params = HashMap<String, String>()
      // The utterance ID isn't actually used anywhere, the param is passed only to force
      // the utterance listener to be notified
      params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "kiwixLastMessage"
      if (currentPiece.get() < pieces.size) {
        tts?.speak(pieces[currentPiece.getAndIncrement()], TextToSpeech.QUEUE_ADD, params)
      } else {
        stop()
      }
      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        @SuppressWarnings("EmptyFunctionBlock")
        override fun onStart(s: String) {
        }

        override fun onDone(s: String) {
          val line: Int = currentPiece.toInt()
          if (line >= pieces.size && !paused) {
            stop()
            return
          }
          tts?.speak(pieces[line], TextToSpeech.QUEUE_ADD, params)
          currentPiece.getAndIncrement()
        }

        override fun onError(s: String) {
          Log.e(TAG_KIWIX, "TextToSpeech Error: $s")
          context.toast(R.string.texttospeech_error, Toast.LENGTH_SHORT)
        }
      })
    }

    fun stop() {
      currentTTSTask = null
      onSpeakingListener.onSpeakingEnded()
    }
  }

  private inner class TTSJavaScriptInterface {
    @JavascriptInterface fun speakAloud(content: String) {
      val splitted = content.split("[\\n.;]".toRegex()).toTypedArray()
      val pieces = splitted.filter(String::isNotBlank)
      if (pieces.isNotEmpty()) {
        onSpeakingListener.onSpeakingStarted()
      } else {
        return
      }
      currentTTSTask = TTSTask(pieces)
      currentTTSTask?.start()
    }
  }
}
