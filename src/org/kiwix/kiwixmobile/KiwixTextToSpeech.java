package org.kiwix.kiwixmobile;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;

public class KiwixTextToSpeech {

    public static final String TAG_KIWIX = "kiwix";

    private Context context;

    private OnSpeakingListener onSpeakingListener;

    private WebView webView;

    private TextToSpeech tts;

    private boolean initialized = false;

    /**
     * Constructor.
     *
     * @param context               the context to create TextToSpeech with
     * @param webView               {@link android.webkit.WebView} to take contents from
     * @param onInitSucceedListener listener that receives event when initialization of TTS is done
     *                              (and does not receive if it failed)
     * @param onSpeakingListener    listener that receives an event when speaking just started or
     *                              ended
     */
    public KiwixTextToSpeech(Context context, WebView webView,
            final OnInitSucceedListener onInitSucceedListener,
            final OnSpeakingListener onSpeakingListener) {
        Log.d(TAG_KIWIX, "Initializing TextToSpeech");

        this.context = context;
        this.onSpeakingListener = onSpeakingListener;
        this.webView = webView;
        this.webView.addJavascriptInterface(new TTSJavaScriptInterface(), "tts");

        initTTS(onInitSucceedListener);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void initTTS(final OnInitSucceedListener onInitSucceedListener) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG_KIWIX, "TextToSpeech was initialized successfully.");
                    initialized = true;
                    onInitSucceedListener.onInitSucceed();
                } else {
                    Log.e(TAG_KIWIX, "Initilization of TextToSpeech Failed!");
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                Log.e(TAG_KIWIX, "TextToSpeech: " + utteranceId);
                onSpeakingListener.onSpeakingEnded();
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG_KIWIX, "TextToSpeech: " + utteranceId);
                onSpeakingListener.onSpeakingEnded();
            }
        });
    }

    /**
     * Reads the currently selected text in the WebView.
     */
    public void readSelection() {
      webView.loadUrl("javascript:tts.speakAloud(window.getSelection().toString());", null);
    }

    /**
     * Starts speaking the WebView content aloud (or stops it if TTS is speaking now).
     */
    public void readAloud() {
        if (tts.isSpeaking()) {
            if (tts.stop() == TextToSpeech.SUCCESS) {
                onSpeakingListener.onSpeakingEnded();
            }
        } else {
            Locale locale = LanguageUtils.ISO3ToLocale(ZimContentProvider.getLanguage());
            int result;
            if (locale == null
                    || (result = tts.isLanguageAvailable(locale)) == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG_KIWIX, "TextToSpeech: language not supported: " +
                        ZimContentProvider.getLanguage() + " (" + locale.getLanguage() + ")");
                Toast.makeText(context,
                        context.getResources().getString(R.string.tts_lang_not_supported),
                        Toast.LENGTH_LONG).show();
            } else {
                tts.setLanguage(locale);

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
        }
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
    public interface OnInitSucceedListener {

        public void onInitSucceed();
    }

    /**
     * The listener that is notified when speaking starts or stops (regardless of whether it was a
     * result of error, user, or because whole text was read).
     *
     * Note that the methods of this interface may not be called from the UI thread.
     */
    public interface OnSpeakingListener {

        public void onSpeakingStarted();

        public void onSpeakingEnded();
    }

    private class TTSJavaScriptInterface {

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void speakAloud(String content) {
            String[] lines = content.split("\n");
            for (int i = 0; i < lines.length - 1; i++) {
                String line = lines[i];
                tts.speak(line, TextToSpeech.QUEUE_ADD, null);
            }

            HashMap<String, String> params = new HashMap<>();
            // The utterance ID isn't actually used anywhere, the param is passed only to force
            // the utterance listener to be notified
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "kiwixLastMessage");
            tts.speak(lines[lines.length - 1], TextToSpeech.QUEUE_ADD, params);

            if (lines.length > 0) {
                onSpeakingListener.onSpeakingStarted();
            }
        }
    }
}
