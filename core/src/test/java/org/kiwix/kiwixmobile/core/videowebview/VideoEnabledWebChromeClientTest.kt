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
 */

package org.kiwix.kiwixmobile.core.videowebview

import android.media.MediaPlayer
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.widget.FrameLayout
import android.widget.VideoView
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.videowebview.VideoEnabledWebChromeClient
import org.kiwix.videowebview.VideoEnabledWebView

class VideoEnabledWebChromeClientTest {
  private val activityVideoView: ViewGroup = mockk(relaxed = true)
  private val loadingView: View = mockk(relaxed = true)
  private val webView: VideoEnabledWebView = mockk(relaxed = true)
  private val callback: WebChromeClient.CustomViewCallback = mockk(relaxed = true)
  private val mediaPlayer: MediaPlayer = mockk(relaxed = true)

  @BeforeEach
  fun setUp() {
    clearAllMocks()
  }

  @Nested
  inner class ConstructorTests {
    @Test
    fun `should create instance with isVideoFullscreen false by default`() {
      val client = VideoEnabledWebChromeClient()
      assertThat(client.isVideoFullscreen()).isFalse()
    }
  }

  @Nested
  inner class OnShowCustomViewTests {
    @Test
    fun `should set video fullscreen to true when onShowCustomView is called with FrameLayout`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      client.onShowCustomView(frameLayout, callback)

      assertThat(client.isVideoFullscreen()).isTrue()
    }

    @Test
    fun `should add video container to activity video view when onShowCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      client.onShowCustomView(frameLayout, callback)

      verify { activityVideoView.addView(frameLayout, any<ViewGroup.LayoutParams>()) }
    }

    @Test
    fun `should make activity video view visible when onShowCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      client.onShowCustomView(frameLayout, callback)

      verify { activityVideoView.visibility = View.VISIBLE }
    }

    @Test
    fun `should set media listeners when onShowCustomView is called with VideoView focused child`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val videoView: VideoView = mockk(relaxed = true)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns videoView

      client.onShowCustomView(frameLayout, callback)

      verify { videoView.setOnPreparedListener(client) }
      verify { videoView.setOnCompletionListener(client) }
      verify { videoView.setOnErrorListener(client) }
    }

    @Test
    fun `should inject JS for video end detection when SurfaceView is used and JS is enabled`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val surfaceView: SurfaceView = mockk(relaxed = true)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      val settings: WebSettings = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns surfaceView
      every { webView.settings } returns settings
      every { settings.javaScriptEnabled } returns true

      client.onShowCustomView(frameLayout, callback)

      verify { webView.loadUrl(match { it.startsWith("javascript:") }) }
    }

    @Test
    fun `should not inject JS when JavaScript is disabled`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val surfaceView: SurfaceView = mockk(relaxed = true)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      val settings: WebSettings = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns surfaceView
      every { webView.settings } returns settings
      every { settings.javaScriptEnabled } returns false

      client.onShowCustomView(frameLayout, callback)

      verify(exactly = 0) { webView.loadUrl(any<String>()) }
    }

    @Test
    fun `should not inject JS for unsupported view types`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val genericView: View = mockk(relaxed = true)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      val settings: WebSettings = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns genericView
      every { webView.settings } returns settings
      every { settings.javaScriptEnabled } returns true

      client.onShowCustomView(frameLayout, callback)

      verify(exactly = 0) { webView.loadUrl(any<String>()) }
    }

    @Test
    fun `should notify toggled fullscreen callback with true when video is shown`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)
      client.setOnToggledFullscreen(toggleCallback)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      client.onShowCustomView(frameLayout, callback)

      verify { toggleCallback.toggledFullscreen(true) }
    }

    @Test
    fun `should not crash when webView is null and SurfaceView is used`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, null)
      val surfaceView: SurfaceView = mockk(relaxed = true)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns surfaceView

      client.onShowCustomView(frameLayout, callback)

      assertThat(client.isVideoFullscreen()).isTrue()
      verify(exactly = 0) { webView.loadUrl(any<String>()) }
    }

    @Test
    fun `should delegate to primary overload in deprecated onShowCustomView`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      @Suppress("DEPRECATION")
      client.onShowCustomView(frameLayout, 0, callback)

      assertThat(client.isVideoFullscreen()).isTrue()
      verify { activityVideoView.addView(frameLayout, any<ViewGroup.LayoutParams>()) }
    }
  }

  @Nested
  inner class OnHideCustomViewTests {
    private fun enterFullscreen(client: VideoEnabledWebChromeClient): FrameLayout {
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, callback)
      return frameLayout
    }

    @Test
    fun `should set video fullscreen to false when onHideCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      enterFullscreen(client)

      client.onHideCustomView()

      assertThat(client.isVideoFullscreen()).isFalse()
    }

    @Test
    fun `should make activity video view invisible when onHideCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      enterFullscreen(client)

      client.onHideCustomView()

      verify { activityVideoView.visibility = View.INVISIBLE }
    }

    @Test
    fun `should remove video container from activity video view when onHideCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout = enterFullscreen(client)

      client.onHideCustomView()

      verify { activityVideoView.removeView(frameLayout) }
    }

    @Test
    fun `should not call onCustomViewHidden for non-chromium callbacks`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val nonChromiumCallback: WebChromeClient.CustomViewCallback = mockk(relaxed = true)

      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, nonChromiumCallback)

      client.onHideCustomView()

      verify(exactly = 0) { nonChromiumCallback.onCustomViewHidden() }
    }

    @Test
    fun `should notify toggled fullscreen callback with false when onHideCustomView is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)
      client.setOnToggledFullscreen(toggleCallback)
      enterFullscreen(client)

      client.onHideCustomView()

      verify { toggleCallback.toggledFullscreen(false) }
    }

    @Test
    fun `should do nothing when onHideCustomView is called and not in fullscreen`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)

      client.onHideCustomView()

      assertThat(client.isVideoFullscreen()).isFalse()
      verify(exactly = 0) { activityVideoView.visibility = View.INVISIBLE }
      verify(exactly = 0) { activityVideoView.removeView(any()) }
    }

    @Test
    fun `should process only first call when onHideCustomView is called multiple times`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)
      client.setOnToggledFullscreen(toggleCallback)
      enterFullscreen(client)

      client.onHideCustomView()
      client.onHideCustomView()

      verify(exactly = 1) { toggleCallback.toggledFullscreen(false) }
    }
  }

  @Nested
  inner class OnBackPressedTests {
    @Test
    fun `should return true and hide custom view when onBackPressed is called in fullscreen`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, callback)

      val result = client.onBackPressed()

      assertThat(result).isTrue()
      assertThat(client.isVideoFullscreen()).isFalse()
    }

    @Test
    fun `should return false when onBackPressed is called and not in fullscreen`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)

      val result = client.onBackPressed()

      assertThat(result).isFalse()
    }

    @Test
    fun `should call onHideCustomView when onBackPressed is called in fullscreen`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)
      client.setOnToggledFullscreen(toggleCallback)

      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, callback)

      client.onBackPressed()

      verify { toggleCallback.toggledFullscreen(false) }
      verify { activityVideoView.visibility = View.INVISIBLE }
    }
  }

  @Nested
  inner class GetVideoLoadingProgressViewTests {
    @Test
    fun `should return loading view when set`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)

      val result = client.videoLoadingProgressView

      assertThat(result).isEqualTo(loadingView)
      verify { loadingView.visibility = View.VISIBLE }
    }

    @Test
    fun `should return null when loading view is null`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, null, webView)

      val result = client.videoLoadingProgressView

      assertThat(result).isNull()
    }
  }

  @Nested
  inner class MediaPlayerCallbackTests {
    @Test
    fun `should hide loading view when onPrepared is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)

      client.onPrepared(mediaPlayer)

      verify { loadingView.visibility = View.GONE }
    }

    @Test
    fun `should call onHideCustomView when onCompletion is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, callback)
      assertThat(client.isVideoFullscreen()).isTrue()

      client.onCompletion(mediaPlayer)

      assertThat(client.isVideoFullscreen()).isFalse()
    }

    @Test
    fun `should return false when onError is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)

      val result = client.onError(mediaPlayer, 0, 0)

      assertThat(result).isFalse()
    }
  }

  @Nested
  inner class ToggledFullscreenCallbackTests {
    @Test
    fun `should register callback when setOnToggledFullscreen is called`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)

      client.setOnToggledFullscreen(toggleCallback)

      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()
      client.onShowCustomView(frameLayout, callback)

      verify { toggleCallback.toggledFullscreen(true) }
    }

    @Test
    fun `should trigger both callbacks in order for full show-hide lifecycle`() {
      val client = VideoEnabledWebChromeClient(activityVideoView, loadingView, webView)
      val toggleCallback: VideoEnabledWebChromeClient.ToggledFullscreenCallback =
        mockk(relaxed = true)
      client.setOnToggledFullscreen(toggleCallback)
      val frameLayout: FrameLayout = mockk(relaxed = true)
      every { frameLayout.focusedChild } returns mockk<View>()

      client.onShowCustomView(frameLayout, callback)
      client.onHideCustomView()

      verify(ordering = io.mockk.Ordering.ORDERED) {
        toggleCallback.toggledFullscreen(true)
        toggleCallback.toggledFullscreen(false)
      }
    }
  }
}
