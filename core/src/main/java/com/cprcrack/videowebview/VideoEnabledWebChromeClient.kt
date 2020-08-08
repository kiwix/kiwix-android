/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package com.cprcrack.videowebview

import android.media.MediaPlayer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import android.widget.VideoView
import org.kiwix.kiwixmobile.core.main.KiwixWebView

/**
 * This class serves as a WebChromeClient to be set to a WebView, allowing it to play video.
 * Video will play differently depending on target API level (in-line, fullscreen, or both).
 *
 * It has been tested with the following video classes:
 * - android.widget.VideoView (typically API level <11)
 * - android.webkit.HTML5VideoFullScreen$VideoSurfaceView/VideoTextureView (typically API level
 * 11-18)
 * - com.android.org.chromium.content.browser.ContentVideoView$VideoSurfaceView (typically API level
 * 19+)
 *
 * Important notes:
 * - For API level 11+, android:hardwareAccelerated="true" must be set in the application manifest.
 * - The invoking activity must call VideoEnabledWebChromeClient's onBackPressed() inside of its own
 * onBackPressed().
 * - Tested in Android API levels 8-19. Only tested on http://m.youtube.com.
 *
 * @author Cristian Perez (http://cpr.name)
 */

open class VideoEnabledWebChromeClient constructor() : WebChromeClient(),
    MediaPlayer.OnCompletionListener,
    MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {
  constructor(
    nonVideoView: ViewGroup,
    videoView: ViewGroup,
    arg3: Any,
    webView: KiwixWebView
  ) : this()

  interface ToggledFullScreenCallback {

    fun toggledFullscreen(fullscreen: Boolean)
  }

  private var activityNonVideoView: View? = null
  private var activityVideoView: ViewGroup? = null
  private var loadingView: View? = null
  private var webView: VideoEnabledWebView? = null

  // Indicates if the video is being displayed using a custom view (typically full-screen)
  private var isVideoFullscreen = false
  private var videoViewContainer: FrameLayout? = null
  private var videoViewCallback: CustomViewCallback? = null

  private var toggledFullScreenCallback: ToggledFullScreenCallback? = null

  // * Never use this constructor alone.
  // * This constructor allows this class to be defined as an inline inner class in which the user can
  // * override methods

  // * Builds a video enabled WebChromeClient.
  // *
  // * @param activityNonVideoView A View in the activity's layout that contains every other view that
  // * should be hidden when the video goes full-screen.
  // * @param activityVideoView A ViewGroup in the activity's layout that will display the video.
  // * Typically you would like this to fill the whole layout.

  fun View?.videoEnabledWebChromeClient(activityVideoView: ViewGroup?) {
    activityNonVideoView = this
    this@VideoEnabledWebChromeClient.activityVideoView = activityVideoView
    loadingView = null
    webView = null
    isVideoFullscreen = false
  }

  /*
* Builds a video enabled WebChromeClient.
*
* @param activityNonVideoView A View in the activity's layout that contains every other view that
* should be hidden when the video goes full-screen.
* @param activityVideoView A ViewGroup in the activity's layout that will display the video.
* Typically you would like this to fill the whole layout.
* @param loadingView A View to be shown while the video is loading (typically only used in API
* level <11). Must be already inflated and not attached to a parent view.
*/

  fun ViewGroup?.videoEnabledWebChromeClient(
    activityNonVideoView: View?,
    loadingView: View?
  ) {
    this@VideoEnabledWebChromeClient.activityNonVideoView = activityNonVideoView
    this@VideoEnabledWebChromeClient.activityVideoView = this
    this@VideoEnabledWebChromeClient.loadingView = loadingView
    webView = null
    isVideoFullscreen = false
  }

  // * Builds a video enabled WebChromeClient.
  // *
  // * @param activityNonVideoView A View in the activity's layout that contains every other view that
  // * should be hidden when the video goes full-screen.
  // * @param activityVideoView A ViewGroup in the activity's layout that will display the video.
  // * Typically you would like this to fill the whole layout.
  // * @param loadingView A View to be shown while the video is loading (typically only used in API
  // * level <11). Must be already inflated and not attached to a parent view.
  // * @param webView The owner VideoEnabledWebView. Passing it will enable the
  // * VideoEnabledWebChromeClient to detect the HTML5 video ended event and exit full-screen.
  // * Note: The web page must only contain one video tag in order for the HTML5 video ended event to
  // * work. This could be improved if needed (see Javascript code).

  fun ViewGroup?.videoEnabledWebChromeClient(
    activityNonVideoView: View?,
    loadingView: View?,
    webView: VideoEnabledWebView?
  ) {
    this@VideoEnabledWebChromeClient.activityNonVideoView = activityNonVideoView
    this@VideoEnabledWebChromeClient.activityVideoView = this
    this@VideoEnabledWebChromeClient.loadingView = loadingView
    this@VideoEnabledWebChromeClient.webView = webView
    isVideoFullscreen = false
  }

  /**
   * Indicates if the video is being displayed using a custom view (typically full-screen)
   *
   * @return true it the video is being displayed using a custom view (typically full-screen)
   */
  fun isVideoFullscreen(): Boolean = isVideoFullscreen

  /**
   * Set a callback that will be fired when the video starts or finishes displaying using a custom
   * view (typically full-screen)
   *
   * @param callback A VideoEnabledWebChromeClient.ToggledFullscreenCallback callback
   */
  @SuppressWarnings("unused")
  fun setOnToggledFullscreen(callback: VideoEnabledWebChromeClient.ToggledFullScreenCallback?) {
    toggledFullScreenCallback = callback
  }

  override fun onShowCustomView(
    view: View,
    callback: CustomViewCallback
  ) {
    if (view is FrameLayout) {

      /*
      A video wants to be shown
      A video wants to be shown
      */

      val focusedChild: View = view.focusedChild

      // Save video related variables
      isVideoFullscreen = true
      videoViewContainer = view
      videoViewCallback = callback

      // Hide the non-video view, add the video view, and show it
      activityNonVideoView!!.visibility = View.INVISIBLE
      activityVideoView!!.addView(
          videoViewContainer,
          ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
          )
      )
      activityVideoView!!.visibility = View.VISIBLE

      if (focusedChild is VideoView) {

        // android.widget.VideoView (typically API level <11)

        // Handle all the required events
        focusedChild.setOnPreparedListener(this)
        focusedChild.setOnCompletionListener { this }
        focusedChild.setOnErrorListener(this)
      } else {
        if (webView != null && webView!!.settings.javaScriptEnabled &&
            focusedChild is SurfaceView
        ) {
          // Run javascript code that detects the video end and notifies the Javascript interface
          var js = "javascript:"
          js += "var _ytrp_html5_video_last;"
          js += "var _ytrp_html5_video = document.getElementsByTagName('video')[0];"
          js += "if (_ytrp_html5_video != undefined && _ytrp_html5_video != " +
              "_ytrp_html5_video_last) {"
          run {
            js += "_ytrp_html5_video_last = _ytrp_html5_video;"
            js += "function _ytrp_html5_video_ended() {"
            run {
              js += "_VideoEnabledWebView.notifyVideoEnd();"
            }
            js += "}"
            js += "_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);"
          }
          js += "}"
          webView!!.loadUrl(js)
        }
      }

      // android.widget.VideoView (typically API level <11)

      // Notify full-screen change
      if (toggledFullScreenCallback != null) {
        toggledFullScreenCallback!!.toggledFullscreen(true)
      }
    }
  }

  @SuppressWarnings("deprecation")
  override fun onShowCustomView(
    view: View?,
    requestedOrientation: Int,
    callback: CustomViewCallback? // Available in API level 14+, deprecated in API level 18+
  ) {
    onShowCustomView(view!!, callback!!)
  }

  override fun onHideCustomView() {
    // This method should be manually called on video end in all cases because it's not always called automatically.
    // This method must be manually called on back key press (from this class' onBackPressed() method).
    if (isVideoFullscreen) {
      // Hide the video view, remove it, and show the non-video view
      activityVideoView!!.visibility = View.INVISIBLE
      activityVideoView!!.removeView(videoViewContainer)
      activityNonVideoView!!.visibility = View.VISIBLE

      /* Call back (only in API level <19, because in API level 19+ with chromium webview it crashes) */
      if (videoViewCallback != null && videoViewCallback!!::class.java.name.contains(".chromium.")
      ) {
        videoViewCallback!!.onCustomViewHidden()
      }

      // Reset video related variables
      isVideoFullscreen = false
      videoViewContainer = null
      videoViewCallback = null

      // Notify full-screen change
      if (toggledFullScreenCallback != null) {
        toggledFullScreenCallback!!.toggledFullscreen(false)
      }
    }
  }

  override fun getVideoLoadingProgressView(): View? {
    return if (loadingView != null) // Video will start loading
    {
      loadingView!!.visibility = View.VISIBLE
      loadingView
    } else {
      super.getVideoLoadingProgressView()
    }
  }

  // Video will start playing, only called in the case of
  // android.widget.VideoView (typically API level <11)

  override fun onPrepared(
    mp: MediaPlayer?
  ) {
    if (loadingView != null) {
      loadingView!!.visibility = View.GONE
    }
  }

  // Video finished playing, only called in the case of
  // android.widget.VideoView (typically API level <11)

  override fun onCompletion(
    mp: MediaPlayer?
  ) {
    onHideCustomView()
  }

  // Error while playing video, only called in the case of
  // android.widget.VideoView (typically API level <11)
  // By returning false, onCompletion() will be called
  override fun onError(
    mp: MediaPlayer?,
    what: Int,
    extra: Int
  ): Boolean =
    false

  fun onBackPressed(): Boolean {
    return if (isVideoFullscreen) {
      onHideCustomView()
      true
    } else {
      false
    }
  }
}
