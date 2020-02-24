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
package org.kiwix.kiwixmobile.core.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;

import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX;

public class KiwixTextToSpeech {

  private final Object focusLock;
  private final Context context;
  private final OnSpeakingListener onSpeakingListener;
  private final AudioManager am;
  private final OnAudioFocusChangeListener onAudioFocusChangeListener;
  private final ZimReaderContainer zimReaderContainer;
  public TTSTask currentTTSTask = null;
  private TextToSpeech tts;
  private boolean initialized = false;

  /**
   * Constructor.
   *
   * @param context the context to create TextToSpeech with
   * @param onInitSucceedListener listener that receives event when initialization of TTS is done
   * (and does not receive if it failed)
   * @param onSpeakingListener listener that receives an event when speaking just started or
   */
  KiwixTextToSpeech(Context context,
    final OnInitSucceedListener onInitSucceedListener,
    final OnSpeakingListener onSpeakingListener,
    final OnAudioFocusChangeListener onAudioFocusChangeListener,
    ZimReaderContainer zimReaderContainer) {
    this.zimReaderContainer = zimReaderContainer;
    Log.d(TAG_KIWIX, "Initializing TextToSpeech");
    this.context = context;
    this.onSpeakingListener = onSpeakingListener;
    this.onAudioFocusChangeListener = onAudioFocusChangeListener;
    am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    focusLock = new Object();
    initTTS(onInitSucceedListener);
  }

  private void initTTS(final OnInitSucceedListener onInitSucceedListener) {
    tts = new TextToSpeech(CoreApp.getInstance(), status -> {
      if (status == TextToSpeech.SUCCESS) {
        Log.d(TAG_KIWIX, "TextToSpeech was initialized successfully.");
        initialized = true;
        onInitSucceedListener.onInitSucceed();
      } else {
        Log.e(TAG_KIWIX, "Initialization of TextToSpeech Failed!");
        //TODO: Surface to user
      }
    });
  }

  /**
   * Reads the currently selected text in the WebView.
   */
  public void readSelection(WebView webView) {
    webView.loadUrl("javascript:tts.speakAloud(window.getSelection().toString());", null);
  }

  /**
   * Starts speaking the WebView content aloud (or stops it if TTS is speaking now).
   */
  public void readAloud(WebView webView) {
    if (currentTTSTask != null && currentTTSTask.paused) {
      onSpeakingListener.onSpeakingEnded();
      currentTTSTask = null;
    } else if (tts.isSpeaking()) {
      if (tts.stop() == TextToSpeech.SUCCESS) {
        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
          tts.setOnUtteranceProgressListener(null);
        }
        onSpeakingListener.onSpeakingEnded();
      }
    } else {
      Locale locale = LanguageUtils.iSO3ToLocale(zimReaderContainer.getLanguage());
      int result;
      if ("mul".equals(zimReaderContainer.getLanguage())) {
        Log.d(TAG_KIWIX, "TextToSpeech: disabled " +
          zimReaderContainer.getLanguage());
        Toast.makeText(context,
          context.getResources().getString(R.string.tts_not_enabled),
          Toast.LENGTH_LONG).show();
        return;
      }
      if (locale == null
        || (result = tts.isLanguageAvailable(locale)) == TextToSpeech.LANG_MISSING_DATA
        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        Log.d(TAG_KIWIX, "TextToSpeech: language not supported: " +
          zimReaderContainer.getLanguage());
        Toast.makeText(context,
          context.getResources().getString(R.string.tts_lang_not_supported),
          Toast.LENGTH_LONG).show();
      } else {
        tts.setLanguage(locale);

        if (getFeatures(tts, locale).contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
          ContextExtensionsKt.toast(context, R.string.tts_lang_not_supported,
            Toast.LENGTH_LONG);
          return;
        }

        if (requestAudioFocus()) {
          loadURL(webView);
        }
      }
    }
  }

  @SuppressLint("NewApi")
  private Set<String> getFeatures(TextToSpeech tts, Locale locale) {
    return VERSION.SDK_INT < VERSION_CODES.LOLLIPOP ? tts.getFeatures(locale)
      : tts.getVoice().getFeatures();
  }

  private void loadURL(WebView webView) {
    // We use JavaScript to get the content of the page conveniently, earlier making some
    // changes in the page
    webView.loadUrl("javascript:" +
      "body = document.getElementsByTagName('body')[0].cloneNode(true);" +
      // Remove some elements that are shouldn't be read (table of contents,
      // references numbers, thumbnail captions, duplicated title, etc.)
      "toRemove = body.querySelectorAll('sup.reference, #toc, .thumbcaption, " +
      "    title, .navbox');" +
      "Array.prototype.forEach.call(toRemove, function(elem) {" +
      "    elem.parentElement.removeChild(elem);" +
      "});" +
      "tts.speakAloud(body.innerText);");
  }

  public void stop() {
    if (tts.stop() == TextToSpeech.SUCCESS) {
      currentTTSTask = null;
      if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        tts.setOnUtteranceProgressListener(null);
      }
      onSpeakingListener.onSpeakingEnded();
    }
  }

  private Boolean requestAudioFocus() {
    int audioFocusRequest =
      am.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN);

    Log.d(TAG_KIWIX, "Audio Focus Requested");

    synchronized (focusLock) {
      return audioFocusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }
  }

  public void pauseOrResume() {
    if (currentTTSTask == null) {
      return;
    }
    if (currentTTSTask.paused) {
      if (!requestAudioFocus()) return;
      currentTTSTask.start();
    } else {
      currentTTSTask.pause();
    }
  }

  public void initWebView(WebView webView) {
    webView.addJavascriptInterface(new TTSJavaScriptInterface(), "tts");
  }

  /**
   * Returns whether the TTS is initialized.
   *
   * @return <code>true</code> if TTS is initialized; <code>false</code> otherwise
   */
  public boolean isInitialized() {
    return initialized;
  }

  /**
   * Releases the resources used by the engine.
   *
   * @see android.speech.tts.TextToSpeech#shutdown()
   */
  public void shutdown() {
    tts.shutdown();
  }

  /**
   * The listener which is notified when initialization of the TextToSpeech engine is successfully
   * done.
   */
  interface OnInitSucceedListener {

    void onInitSucceed();
  }

  /**
   * The listener that is notified when speaking starts or stops (regardless of whether it was a
   * result of error, user, or because whole text was read).
   * <p/>
   * Note that the methods of this interface may not be called from the UI thread.
   */
  public interface OnSpeakingListener {

    void onSpeakingStarted();

    void onSpeakingEnded();
  }

  public class TTSTask {
    private final List<String> pieces;
    private final AtomicInteger currentPiece = new AtomicInteger(0);
    public boolean paused = true;

    private TTSTask(List<String> pieces) {
      this.pieces = pieces;
      //start();
    }

    void pause() {
      paused = true;
      currentPiece.decrementAndGet();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        tts.setOnUtteranceProgressListener(null);
      }
      tts.stop();
    }

    @SuppressLint("NewApi")
    void start() {
      if (!paused) {
        return;
      } else {
        paused = false;
      }

      HashMap<String, String> params = new HashMap<>();
      // The utterance ID isn't actually used anywhere, the param is passed only to force
      // the utterance listener to be notified
      params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "kiwixLastMessage");

      if (currentPiece.get() < pieces.size()) {
        tts.speak(pieces.get(currentPiece.getAndIncrement()), TextToSpeech.QUEUE_ADD, params);
      } else {
        stop();
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
          @Override
          public void onStart(String s) {

          }

          @Override
          public void onDone(String s) {
            int line = currentPiece.intValue();

            if (line >= pieces.size() && !paused) {
              stop();
              return;
            }

            tts.speak(pieces.get(line), TextToSpeech.QUEUE_ADD, params);
            currentPiece.getAndIncrement();
          }

          @Override
          public void onError(String s) {
            Log.e(TAG_KIWIX, "TextToSpeech Error: " + s);
            //TODO: Surface to user
          }
        });
      }
    }

    void stop() {
      currentTTSTask = null;
      onSpeakingListener.onSpeakingEnded();
    }
  }

  private class TTSJavaScriptInterface {
    @JavascriptInterface
    @SuppressWarnings("unused")
    public void speakAloud(String content) {
      String[] splitted = content.split("[\\n.;]");
      List<String> pieces = new ArrayList<>();

      for (String s : splitted) {
        if (!s.trim().isEmpty()) {
          pieces.add(s.trim());
        }
      }

      if (!pieces.isEmpty()) {
        onSpeakingListener.onSpeakingStarted();
      } else {
        return;
      }

      currentTTSTask = new TTSTask(pieces);
      currentTTSTask.start();
    }
  }
}
