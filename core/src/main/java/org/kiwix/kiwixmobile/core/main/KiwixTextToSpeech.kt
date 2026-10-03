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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.iSO3ToLocale
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_TTS_SPEED
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.lang.ref.WeakReference
import java.util.Locale
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
  private var webViewReference: WeakReference<WebView>? = null
  private var isSentenceHighlightingEnabled = false

  var speechRate: Float = DEFAULT_TTS_SPEED
    set(value) {
      field = value
      if (isInitialized) {
        tts.setSpeechRate(value)
        // Android's TTS engine only applies a new rate to the NEXT speak() call, so replay the
        // in-flight sentence to actually hear (and display) the new speed immediately.
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
        iSO3ToLocale(it) ?: runCatching { Locale(it) }.getOrNull()
      }
      ?: Locale.getDefault()

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
      val defaultLoc = Locale.getDefault()
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
    } else if (uniqueVoices.isNotEmpty()) {
      uniqueVoices.sortedBy { it.name }
    } else {
      runCatching { tts.voice }.getOrNull()?.let { listOf(it) }.orEmpty()
    }
  }

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
    webViewReference = WeakReference(webView)
    if (currentTTSTask?.paused == true) {
      onSpeakingListener.onSpeakingEnded()
      currentTTSTask = null
      clearSentenceHighlighting()
    } else if (tts.isSpeaking) {
      if (tts.stop() == SUCCESS) {
        tts.setOnUtteranceProgressListener(null)
        onSpeakingListener.onSpeakingEnded()
        clearSentenceHighlighting()
      }
    } else {
      val locale = iSO3ToLocale(zimReaderContainer.language)
        ?: runCatching { Locale(zimReaderContainer.language.orEmpty()) }.getOrNull()
        ?: Locale.getDefault()
      if (MULTILINGUAL_LANGUAGE_CODE == zimReaderContainer.language) {
        Log.d(TAG_KIWIX, "TextToSpeech: disabled " + zimReaderContainer.language)
        context.toast(R.string.tts_not_enabled, Toast.LENGTH_LONG)
        return
      }
      val availability = tts.isLanguageAvailable(locale)
      if (availability == LANG_NOT_SUPPORTED) {
        Log.d(
          TAG_KIWIX,
          "TextToSpeech: language not supported: ${zimReaderContainer.language}"
        )
        context.toast(R.string.tts_lang_not_supported, Toast.LENGTH_LONG)
        return
      }
      tts.language = locale
      if (availability == LANG_MISSING_DATA ||
        getFeatures(tts).contains(Engine.KEY_FEATURE_NOT_INSTALLED)
      ) {
        // Show download dialog so user can install the missing voice pack
        showTtsLanguageDownloadDialog.invoke()
      } else {
        if (requestAudioFocus()) {
          initWebView(webView)
          loadURL(webView)
        }
      }
    }
  }

  private fun getFeatures(tts: TextToSpeech?): Set<String> = tts?.voice?.features.orEmpty()

  private fun loadURL(webView: WebView) {
    webView.loadUrl(PREPARE_SENTENCES_SCRIPT)
  }

  fun stop() {
    if (tts.stop() == SUCCESS) {
      currentTTSTask = null
      tts.setOnUtteranceProgressListener(null)
      onSpeakingListener.onSpeakingEnded()
      clearSentenceHighlighting()
      onAudioFocusChangeListener = null
    }
  }

  private fun highlightSentence(index: Int) {
    if (!isSentenceHighlightingEnabled) return
    evaluateJavaScript(highlightScript(index))
  }

  private fun clearSentenceHighlighting() {
    if (!isSentenceHighlightingEnabled) return
    isSentenceHighlightingEnabled = false
    evaluateJavaScript(RESTORE_SCRIPT)
  }

  private fun evaluateJavaScript(script: String) {
    val webView = webViewReference?.get() ?: return
    webView.post {
      runCatching { webView.evaluateJavascript(script, null) }
        .onFailure { Log.e(TAG_KIWIX, "Could not run the read aloud script. Exception = $it") }
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
    webViewReference = WeakReference(webView)
    webView.addJavascriptInterface(TTSJavaScriptInterface(), "tts")
  }

  /**
   * Releases the resources and [OnAudioFocusChangeListener] used by the engine.
   *
   * @see android.speech.tts.TextToSpeech.shutdown
   * {@link https://developer.android.com/guide/topics/media-apps/audio-focus#audio-focus-change }
   */
  fun shutdown() {
    coroutineScope.cancel()
    clearSentenceHighlighting()
    webViewReference = null
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
      (pieces[i].length * 65L).coerceAtLeast(1500L)
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

    private fun currentDisplayIndex(): Int {
      val raw = if (paused) currentPiece.get() else currentPiece.get() - 1
      return raw.coerceIn(0, (pieces.size - 1).coerceAtLeast(0))
    }

    val currentPositionMs: Long
      get() {
        val index = currentDisplayIndex()
        if (index < 0 || index >= pieceStartOffsetsMs.size) return 0L
        val baseOffset = pieceStartOffsetsMs[index]
        val elapsedInPiece = if (!paused && currentUtteranceStartMs > 0) {
          val realElapsedMs = (System.currentTimeMillis() - currentUtteranceStartMs)
            .coerceAtLeast(0L)
          (realElapsedMs * speechRate).toLong()
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
      val currentIndex = currentDisplayIndex()
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
      val currentIndex = currentDisplayIndex()
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

    private fun speakPiece(index: Int, queueMode: Int) {
      val utteranceId = "$UTTERANCE_ID_PREFIX$index"
      val bundle =
        Bundle().apply {
          putString(Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
      currentUtteranceStartMs = System.currentTimeMillis()
      tts.speak(pieces[index], queueMode, bundle, utteranceId)
    }

    fun start() {
      if (!paused) {
        return
      }
      paused = false
      if (currentPiece.get() >= pieces.size) {
        stop()
        return
      }
      speakPiece(currentPiece.getAndIncrement(), TextToSpeech.QUEUE_FLUSH)
      tts.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(s: String) {
            currentUtteranceStartMs = System.currentTimeMillis()
            s.substringAfter(UTTERANCE_ID_PREFIX, "")
              .toIntOrNull()
              ?.let(::highlightSentence)
          }

          override fun onDone(s: String) {
            val line: Int = currentPiece.toInt()
            if (line >= pieces.size && !paused) {
              stop()
            } else {
              speakPiece(currentPiece.getAndIncrement(), TextToSpeech.QUEUE_ADD)
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
      clearSentenceHighlighting()
    }
  }

  private fun startSpeaking(pieces: List<String>) {
    if (pieces.isEmpty()) {
      clearSentenceHighlighting()
      return
    }
    val task = TTSTask(pieces)
    currentTTSTask = task
    onSpeakingListener.onSpeakingStarted()
    task.start()
  }

  private inner class TTSJavaScriptInterface {
    @Suppress("unused", "MagicNumber", "NestedBlockDepth")
    @JavascriptInterface
    fun speakAloud(content: String) {
      clearSentenceHighlighting()
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

      startSpeaking(pieces)
    }

    @JavascriptInterface fun speakAloudSentences(sentencesJson: String) {
      val pieces = parseSentences(sentencesJson)
      isSentenceHighlightingEnabled = pieces.isNotEmpty()
      startSpeaking(pieces)
    }

    private fun parseSentences(sentencesJson: String): List<String> =
      runCatching {
        val jsonArray = JSONArray(sentencesJson)
        (0 until jsonArray.length()).map { jsonArray.optString(it).trim() }
      }.getOrElse {
        Log.e(TAG_KIWIX, "Could not parse the sentences of the page. Exception = $it")
        emptyList()
      }
  }

  companion object {
    private const val MULTILINGUAL_LANGUAGE_CODE = "mul"
    private const val UTTERANCE_ID_PREFIX = "kiwixTtsUtterance-"

    private const val SKIP_SELECTOR =
      "sup.reference, #toc, .thumbcaption, title, .navbox, " +
        "[role=\"navigation\"], script, noscript, style"

    private const val HIGHLIGHT_STYLE =
      ".kiwix-tts-active-sentence{" +
        "background-color:rgba(255,193,7,0.45);" +
        "border-radius:3px;" +
        "box-shadow:0 0 0 2px rgba(255,193,7,0.45);" +
        "}"

    private const val RESTORE_SCRIPT =
      "if (window.__kiwixTts) { window.__kiwixTts.restore(); }"

    private fun highlightScript(index: Int) =
      "if (window.__kiwixTts) { window.__kiwixTts.highlight($index); }"

    private val PREPARE_SENTENCES_SCRIPT =
      """
      javascript:(function() {
        if (window.__kiwixTts) { window.__kiwixTts.restore(); }
        var body = document.body;
        if (!body) { return; }
        var skipped = body.querySelectorAll('$SKIP_SELECTOR');
        Array.prototype.forEach.call(skipped, function(element) {
          element.setAttribute('data-kiwix-tts-skip', '');
        });
        var walker = document.createTreeWalker(body, NodeFilter.SHOW_TEXT, {
          acceptNode: function(node) {
            if (!node.nodeValue || !node.nodeValue.trim()) {
              return NodeFilter.FILTER_REJECT;
            }
            var parent = node.parentElement;
            if (!parent || parent.closest('[data-kiwix-tts-skip]')) {
              return NodeFilter.FILTER_REJECT;
            }
            if (parent !== body && !parent.offsetParent) {
              return NodeFilter.FILTER_REJECT;
            }
            return NodeFilter.FILTER_ACCEPT;
          }
        });
        var textNodes = [];
        while (walker.nextNode()) { textNodes.push(walker.currentNode); }
        var sentences = [];
        var spans = [];
        textNodes.forEach(function(node) {
          var parts = node.nodeValue.match(
            /[^]+?(?:[.?!;:](?=\s|${'$'})|\n|${'$'})/g
          );
          if (!parts) { return; }
          var fragment = document.createDocumentFragment();
          var wrapped = false;
          parts.forEach(function(part) {
            var text = part.trim();
            if (!text) {
              fragment.appendChild(document.createTextNode(part));
              return;
            }
            var span = document.createElement('span');
            span.className = 'kiwix-tts-sentence';
            span.setAttribute('data-kiwix-tts-index', String(sentences.length));
            span.appendChild(document.createTextNode(part));
            fragment.appendChild(span);
            sentences.push(text);
            spans.push(span);
            wrapped = true;
          });
          if (wrapped && node.parentNode) {
            node.parentNode.replaceChild(fragment, node);
          }
        });
        Array.prototype.forEach.call(skipped, function(element) {
          element.removeAttribute('data-kiwix-tts-skip');
        });
        if (!document.getElementById('kiwix-tts-style')) {
          var style = document.createElement('style');
          style.id = 'kiwix-tts-style';
          style.textContent = '$HIGHLIGHT_STYLE';
          (document.head || body).appendChild(style);
        }
        window.__kiwixTts = {
          spans: spans,
          current: -1,
          highlight: function(index) {
            if (this.current === index) { return; }
            var previous = this.spans[this.current];
            if (previous) {
              previous.classList.remove('kiwix-tts-active-sentence');
            }
            this.current = index;
            var span = this.spans[index];
            if (!span) { return; }
            span.classList.add('kiwix-tts-active-sentence');
            var rect = span.getBoundingClientRect();
            var height = window.innerHeight || document.documentElement.clientHeight;
            if (rect.top < 0 || rect.bottom > height) {
              span.scrollIntoView({ block: 'center', behavior: 'smooth' });
            }
          },
          clear: function() {
            var span = this.spans[this.current];
            if (span) {
              span.classList.remove('kiwix-tts-active-sentence');
            }
            this.current = -1;
          },
          restore: function() {
            this.clear();
            this.spans.forEach(function(span) {
              var parent = span.parentNode;
              if (!parent) { return; }
              while (span.firstChild) {
                parent.insertBefore(span.firstChild, span);
              }
              parent.removeChild(span);
              parent.normalize();
            });
            this.spans = [];
          }
        };
        tts.speakAloudSentences(JSON.stringify(sentences));
      })();
      """.trimIndent()
  }
}
