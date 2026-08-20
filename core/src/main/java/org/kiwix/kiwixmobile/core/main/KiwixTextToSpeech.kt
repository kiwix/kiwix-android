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
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.TextToSpeech.LANG_MISSING_DATA
import android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.iSO3ToLocale
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_TTS_SPEED
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
  private val onInitSucceedListener: OnInitSucceedListener,
  val onSpeakingListener: OnSpeakingListener,
  private var onAudioFocusChangeListener: OnAudioFocusChangeListener? = null,
  private val zimReaderContainer: ZimReaderContainer,
  private val kiwixDataStore: KiwixDataStore,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
  private var focusRequest: AudioFocusRequest? = null
  private val focusLock: Any = Any()
  private val am: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  @JvmField var currentTTSTask: TTSTask? = null
  private lateinit var tts: TextToSpeech

  var speechRate: Float = DEFAULT_TTS_SPEED
    set(value) {
      field = value
      if (isInitialized) {
        tts.setSpeechRate(value)
        currentTTSTask?.let { task ->
          if (!task.paused) {
            task.pause()
            task.start()
          }
        }
      }
    }

  val currentPositionMs: Long
    get() = currentTTSTask?.currentPositionMs ?: 0L

  val currentVoiceName: String?
    get() = runCatching { if (::tts.isInitialized) tts.voice?.name else null }.getOrNull()

  val totalDurationMs: Long
    get() = currentTTSTask?.totalDurationMs ?: 0L

  fun seekTo(positionMs: Long) {
    currentTTSTask?.seekTo(positionMs)
  }

  fun rewind10s() {
    currentTTSTask?.rewind10s()
  }

  fun forward10s() {
    currentTTSTask?.forward10s()
  }

  @Suppress("Deprecation", "ReturnCount", "CyclomaticComplexMethod")
  fun getAvailableVoices(): List<Voice> {
    if (!isInitialized || !::tts.isInitialized) return emptyList()
    val activeLocale = runCatching { tts.voice?.locale ?: tts.language }.getOrNull()
      ?: zimReaderContainer.language?.let {
        iSO3ToLocale(it) ?: runCatching { java.util.Locale(it) }.getOrNull()
      }
      ?: java.util.Locale.getDefault()

    val allVoices = runCatching { tts.voices }.getOrNull().orEmpty()
    val nonNetworkVoices = allVoices.filter { !it.isNetworkConnectionRequired }
    val candidateVoices = if (nonNetworkVoices.isNotEmpty()) nonNetworkVoices else allVoices

    val uniqueVoices = candidateVoices.distinctBy { voice ->
      voice.name
        .substringBefore("-local")
        .substringBefore("-network")
        .substringBefore("-embedded")
    }

    val preferredCountry = if (activeLocale.country.isNotBlank()) {
      activeLocale.country
    } else {
      val defaultLoc = java.util.Locale.getDefault()
      if (defaultLoc.language.equals(activeLocale.language, ignoreCase = true)) {
        defaultLoc.country
      } else {
        ""
      }
    }

    if (preferredCountry.isNotBlank()) {
      val dialectMatches = uniqueVoices.filter { voice ->
        voice.locale.language.equals(activeLocale.language, ignoreCase = true) &&
          voice.locale.country.equals(preferredCountry, ignoreCase = true)
      }
      if (dialectMatches.isNotEmpty()) {
        return dialectMatches.sortedBy { it.name }
      }
    }

    // 2. Fallback: match language
    val languageMatches = uniqueVoices.filter { voice ->
      voice.locale.language.equals(activeLocale.language, ignoreCase = true)
    }

    return if (languageMatches.isNotEmpty()) {
      languageMatches.sortedBy { it.name }
    } else {
      uniqueVoices.sortedBy { it.name }
    }
  }

  @Suppress("InjectDispatcher")
  fun setVoiceByName(voiceName: String) {
    if (!isInitialized || !::tts.isInitialized) return
    val voice = getAvailableVoices().find { it.name == voiceName }
    if (voice != null) {
      tts.voice = voice
      coroutineScope.launch {
        kiwixDataStore.setSelectedTtsVoice(voiceName)
      }
    }
  }

  /**
   * Initializes the TextToSpeech object.
   */
  @Suppress("InjectDispatcher")
  fun initializeTTS() {
    tts =
      TextToSpeech(
        context
      ) { status: Int ->
        if (status == TextToSpeech.SUCCESS) {
          Log.d(TAG_KIWIX, "TextToSpeech was initialized successfully.")
          isInitialized = true
          coroutineScope.launch {
            val rate = runCatching { kiwixDataStore.ttsSpeed.firstOrNull() }.getOrNull()
            if (rate != null) speechRate = rate
            val savedVoice =
              runCatching { kiwixDataStore.selectedTtsVoice.firstOrNull() }.getOrNull()
            if (!savedVoice.isNullOrBlank()) {
              val savedVoiceObj = tts.voices?.find { it.name == savedVoice }
              if (savedVoiceObj != null) {
                tts.voice = savedVoiceObj
              }
            }
            onInitSucceedListener.onInitSucceed()
          }
        } else {
          Log.e(TAG_KIWIX, "Initialization of TextToSpeech Failed!")
          context.toast(
            R.string.texttospeech_initialization_failed,
            Toast.LENGTH_SHORT
          )
        }
      }
  }

  /**
   * Returns whether the TTS is initialized.
   *
   * @return `true` if TTS is initialized; `false` otherwise
   */
  var isInitialized = false

  init {
    Log.d(TAG_KIWIX, "Initializing TextToSpeech")
  }

  /**
   * Reads the currently selected text in the WebView.
   */
  fun readSelection(webView: WebView) {
    initWebView(webView)
    webView.loadUrl("javascript:tts.speakAloud(window.getSelection().toString());")
  }

  /**
   * Starts speaking the WebView content aloud (or stops it if TTS is speaking now).
   */
  fun readAloud(webView: WebView, showTtsLanguageDownloadDialog: () -> Unit) {
    if (currentTTSTask?.paused == true) {
      onSpeakingListener.onSpeakingEnded()
      currentTTSTask = null
    } else if (tts.isSpeaking) {
      if (tts.stop() == SUCCESS) {
        tts.setOnUtteranceProgressListener(null)
        onSpeakingListener.onSpeakingEnded()
      }
    } else {
      val locale = iSO3ToLocale(zimReaderContainer.language)
      if ("mul" == zimReaderContainer.language) {
        Log.d(TAG_KIWIX, "TextToSpeech: disabled " + zimReaderContainer.language)
        context.toast(R.string.tts_not_enabled, Toast.LENGTH_LONG)
        return
      }
      val availability = locale?.let { tts.isLanguageAvailable(it) } ?: LANG_NOT_SUPPORTED
      when {
        availability == LANG_MISSING_DATA || getFeatures(tts).contains(Engine.KEY_FEATURE_NOT_INSTALLED) -> {
          showTtsLanguageDownloadDialog.invoke()
        }

        availability == LANG_NOT_SUPPORTED -> {
          Log.d(
            TAG_KIWIX,
            "TextToSpeech: language not supported: ${zimReaderContainer.language}"
          )
          context.toast(R.string.tts_lang_not_supported, Toast.LENGTH_LONG)
        }

        else -> {
          tts.language = locale
          if (requestAudioFocus()) {
            initWebView(webView)
            loadURL(webView)
          }
        }
      }
    }
  }

  private fun getFeatures(tts: TextToSpeech?): Set<String> = tts?.voice?.features.orEmpty()

  private fun loadURL(webView: WebView) {
    // We use JavaScript to get the content of the page conveniently, earlier making some
    // changes in the page
    webView.loadUrl(
      """
      javascript:
      body = document.getElementsByTagName('body')[0].cloneNode(true);
      toRemove = body.querySelectorAll('sup.reference, #toc, .thumbcaption, title, .navbox, [role="navigation"], script, noscript, style');
      Array.prototype.forEach.call(toRemove, function(elem) { elem.parentElement.removeChild(elem); });
      tts.speakAloud(body.innerText);
      """.trimIndent()
    )
  }

  fun stop() {
    if (tts.stop() == SUCCESS) {
      currentTTSTask = null
      tts.setOnUtteranceProgressListener(null)
      onSpeakingListener.onSpeakingEnded()
      onAudioFocusChangeListener = null
    }
  }

  private fun requestAudioFocus(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (focusRequest == null) {
        focusRequest =
          onAudioFocusChangeListener?.let {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
              .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
              )
              .setAcceptsDelayedFocusGain(true)
              .setOnAudioFocusChangeListener(it)
              .setWillPauseWhenDucked(true)
              .build()
          }
      }
      Log.d(TAG_KIWIX, "Audio Focus Requested")
      val focusGain = focusRequest?.let(am::requestAudioFocus)
      synchronized(focusLock) {
        return@requestAudioFocus focusGain == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
      }
    }
    @Suppress("DEPRECATION")
    val audioFocusRequest =
      am.requestAudioFocus(
        onAudioFocusChangeListener,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN
      )
    Log.d(TAG_KIWIX, "Audio Focus Requested")
    synchronized(focusLock) {
      return@requestAudioFocus audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
  }

  fun pauseOrResume() {
    currentTTSTask?.let {
      if (it.paused) {
        if (!requestAudioFocus()) return@pauseOrResume
        it.start()
      } else {
        it.pause()
      }
    }
  }

  fun initWebView(webView: WebView) {
    webView.addJavascriptInterface(TTSJavaScriptInterface(), "tts")
  }

  /**
   * Releases the resources and [OnAudioFocusChangeListener] used by the engine.
   *
   * @see android.speech.tts.TextToSpeech.shutdown
   * {@link https://developer.android.com/guide/topics/media-apps/audio-focus#audio-focus-change }
   */
  fun shutdown() {
    if (::tts.isInitialized) {
      tts.shutdown()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      focusRequest?.let(am::abandonAudioFocusRequest)
      focusRequest = null
    } else {
      @Suppress("DEPRECATION")
      am.abandonAudioFocus(onAudioFocusChangeListener)
    }
    onAudioFocusChangeListener = null
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

  @Suppress("MagicNumber")
  inner class TTSTask(val pieces: List<String>) {
    private val currentPiece = AtomicInteger(0)
    private val pieceDurationsMs: LongArray = LongArray(pieces.size) { i ->
      val baseMs = (pieces[i].length * 65L).coerceIn(1500L, 8000L)
      (baseMs / speechRate.coerceAtLeast(0.1f)).toLong()
    }
    private val pieceStartOffsetsMs: LongArray = LongArray(pieces.size)

    val totalDurationMs: Long

    init {
      var acc = 0L
      for (i in pieces.indices) {
        pieceStartOffsetsMs[i] = acc
        acc += pieceDurationsMs[i]
      }
      totalDurationMs = acc
    }

    private var currentUtteranceStartMs: Long = 0L

    @JvmField var paused = true

    val currentPositionMs: Long
      get() {
        val index = (currentPiece.get() - 1).coerceIn(0, (pieces.size - 1).coerceAtLeast(0))
        if (index < 0 || index >= pieceStartOffsetsMs.size) return 0L
        val baseOffset = pieceStartOffsetsMs[index]
        val elapsedInPiece = if (!paused && currentUtteranceStartMs > 0) {
          (System.currentTimeMillis() - currentUtteranceStartMs).coerceAtLeast(0L)
        } else {
          0L
        }
        return (baseOffset + elapsedInPiece).coerceAtMost(totalDurationMs)
      }

    fun seekTo(targetPositionMs: Long) {
      val clampedTarget = targetPositionMs.coerceIn(0L, totalDurationMs)
      var targetIndex = 0
      for (i in pieceStartOffsetsMs.indices) {
        if (pieceStartOffsetsMs[i] <= clampedTarget) {
          targetIndex = i
        } else {
          break
        }
      }
      jumpToPiece(targetIndex)
    }

    fun rewind10s() {
      val currentIndex = (currentPiece.get() - 1).coerceIn(0, (pieces.size - 1).coerceAtLeast(0))
      val clampedTarget = (currentPositionMs - 10000L).coerceAtLeast(0L)
      var targetIndex = 0
      for (i in pieceStartOffsetsMs.indices) {
        if (pieceStartOffsetsMs[i] <= clampedTarget) {
          targetIndex = i
        } else {
          break
        }
      }
      if (targetIndex >= currentIndex && currentIndex > 0) {
        targetIndex = currentIndex - 1
      }
      jumpToPiece(targetIndex)
    }

    fun forward10s() {
      val currentIndex = (currentPiece.get() - 1).coerceIn(0, (pieces.size - 1).coerceAtLeast(0))
      val clampedTarget = (currentPositionMs + 10000L).coerceAtMost(totalDurationMs)
      var targetIndex = currentIndex
      for (i in pieceStartOffsetsMs.indices) {
        if (pieceStartOffsetsMs[i] <= clampedTarget) {
          targetIndex = i
        } else {
          break
        }
      }
      if (targetIndex <= currentIndex && currentIndex < pieces.size - 1) {
        targetIndex = currentIndex + 1
      }
      jumpToPiece(targetIndex)
    }

    private fun jumpToPiece(targetIndex: Int) {
      val wasPaused = paused
      tts.setOnUtteranceProgressListener(null)
      tts.stop()
      paused = true
      currentPiece.set(targetIndex.coerceIn(0, (pieces.size - 1).coerceAtLeast(0)))
      if (!wasPaused) {
        start()
      }
    }

    fun pause() {
      paused = true
      if (currentPiece.get() > ZERO) {
        currentPiece.decrementAndGet()
      }
      tts.setOnUtteranceProgressListener(null)
      tts.stop()
    }

    fun start() {
      if (!paused) {
        return
      }
      paused = false
      currentUtteranceStartMs = System.currentTimeMillis()
      val bundle =
        Bundle().apply {
          putString(Engine.KEY_PARAM_UTTERANCE_ID, "kiwixLastMessage")
        }
      if (currentPiece.get() < pieces.size) {
        tts.speak(
          pieces[currentPiece.getAndIncrement()],
          TextToSpeech.QUEUE_FLUSH,
          bundle,
          bundle.getString(Engine.KEY_PARAM_UTTERANCE_ID)
        )
      } else {
        stop()
      }
      tts.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          @SuppressWarnings("EmptyFunctionBlock")
          override fun onStart(s: String) {
            currentUtteranceStartMs = System.currentTimeMillis()
          }

          override fun onDone(s: String) {
            val line: Int = currentPiece.toInt()
            if (line >= pieces.size && !paused) {
              stop()
            } else {
              currentUtteranceStartMs = System.currentTimeMillis()
              tts.speak(
                pieces[currentPiece.getAndIncrement()],
                TextToSpeech.QUEUE_ADD,
                bundle,
                bundle.getString(Engine.KEY_PARAM_UTTERANCE_ID)
              )
            }
          }

          @Deprecated("Deprecated in Java")
          override fun onError(s: String) {
            Log.e(TAG_KIWIX, "TextToSpeech Error: $s")
            context.toast(R.string.texttospeech_error, Toast.LENGTH_SHORT)
          }
        }
      )
    }

    fun stop() {
      currentTTSTask = null
      onSpeakingListener.onSpeakingEnded()
    }
  }

  private inner class TTSJavaScriptInterface {
    @Suppress("unused", "MagicNumber", "NestedBlockDepth")
    @JavascriptInterface
    fun speakAloud(content: String) {
      val rawSentences = content.split("(?<=[.?!;:\\n])\\s+".toRegex())
        .filter(String::isNotBlank)
        .map(String::trim)

      val pieces = mutableListOf<String>()
      for (sentence in rawSentences) {
        if (sentence.length <= 120) {
          pieces.add(sentence)
        } else {
          val subParts = sentence.split("(?<=[,])\\s+".toRegex())
          for (part in subParts) {
            if (part.isNotBlank()) pieces.add(part.trim())
          }
        }
      }

      if (pieces.isNotEmpty()) {
        val task = TTSTask(pieces)
        currentTTSTask = task
        onSpeakingListener.onSpeakingStarted()
        task.start()
      }
    }
  }
}
