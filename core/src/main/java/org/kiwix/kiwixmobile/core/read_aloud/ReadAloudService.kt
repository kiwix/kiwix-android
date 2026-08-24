/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.read_aloud

import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import org.kiwix.kiwixmobile.core.R
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import javax.inject.Inject

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class ReadAloudService : MediaSessionService() {
  @set:Inject
  var readAloudNotificationManager: ReadAloudNotificationManager? = null
  private val serviceBinder: IBinder = ReadAloudBinder(this)
  private var readAloudCallbacks: ReadAloudCallbacks? = null
  private var mediaSession: MediaSession? = null
  private var ttsPlayer: TtsSimplePlayer? = null
  private var isTtsPaused: Boolean = false

  override fun onCreate() {
    super.onCreate()
    runCatching {
      val player = TtsSimplePlayer()
      ttsPlayer = player

      val rewindButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS)
        .setPlayerCommand(Player.COMMAND_SEEK_BACK)
        .setDisplayName("-10s")
        .setIconResId(R.drawable.ic_replay_10)
        .build()

      val forwardButton = CommandButton.Builder(CommandButton.ICON_NEXT)
        .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
        .setDisplayName("+10s")
        .setIconResId(R.drawable.ic_forward_10)
        .build()

      val stopButton = CommandButton.Builder(CommandButton.ICON_STOP)
        .setPlayerCommand(Player.COMMAND_STOP)
        .setDisplayName(getString(R.string.stop))
        .setIconResId(R.drawable.ic_baseline_stop)
        .build()

      mediaSession = MediaSession.Builder(this, player)
        .setCustomLayout(listOf(rewindButton, forwardButton, stopButton))
        .build()
    }.onFailure { it.printStackTrace() }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
    mediaSession

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)
    when (intent?.action) {
      ACTION_STOP_TTS -> stopReadAloudAndDismissNotification()
      ACTION_PAUSE_OR_RESUME_TTS -> {
        val isPauseTTS = intent.getBooleanExtra(IS_TTS_PAUSE_OR_RESUME, false)
        startForegroundNotificationHelper(isPauseTTS)
        readAloudCallbacks?.onReadAloudPauseOrResume(isPauseTTS)
      }

      ACTION_REWIND_10 -> {
        readAloudCallbacks?.onReadAloudRewind10s()
        startForegroundNotificationHelper(isTtsPaused)
      }

      ACTION_FORWARD_10 -> {
        readAloudCallbacks?.onReadAloudForward10s()
        startForegroundNotificationHelper(isTtsPaused)
      }
    }
    return START_NOT_STICKY
  }

  private fun stopReadAloudAndDismissNotification() {
    readAloudCallbacks?.onReadAloudStop()
    readAloudNotificationManager?.dismissNotification()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun startForegroundNotificationHelper(isPauseTTS: Boolean) {
    isTtsPaused = isPauseTTS
    ttsPlayer?.updatePlaybackState(isPauseTTS)
    runCatching {
      readAloudNotificationManager?.buildForegroundNotification(isPauseTTS, mediaSession)?.let {
        startForeground(ReadAloudNotificationManager.READ_ALOUD_NOTIFICATION_ID, it)
      }
    }.onFailure { it.printStackTrace() }
  }

  @OptIn(UnstableApi::class)
  private inner class TtsSimplePlayer : SimpleBasePlayer(Looper.getMainLooper()) {
    private val iconBytes = runCatching {
      val bitmap =
        BitmapFactory.decodeResource(
          resources,
          R.mipmap.ic_launcher
        )
      val stream = ByteArrayOutputStream()
      bitmap.compress(PNG, 100, stream)
      stream.toByteArray()
    }.getOrNull()

    private val itemData = MediaItemData.Builder("kiwix_tts_track")
      .setMediaMetadata(
        MediaMetadata.Builder()
          .apply {
            if (iconBytes != null) {
              setArtworkData(
                iconBytes,
                MediaMetadata.PICTURE_TYPE_FRONT_COVER
              )
            }
          }
          .build()
      )
      .build()

    override fun getState(): State = State.Builder()
      .setAvailableCommands(
        Player.Commands.Builder().addAll(
          Player.COMMAND_PLAY_PAUSE,
          Player.COMMAND_SEEK_BACK,
          Player.COMMAND_SEEK_FORWARD,
          Player.COMMAND_STOP
        ).build()
      )
      .setPlaylist(listOf(itemData))
      .setPlaybackState(Player.STATE_READY)
      .setPlayWhenReady(!isTtsPaused, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
      .setSeekBackIncrementMs(SEEK_INCREMENT_MS)
      .setSeekForwardIncrementMs(SEEK_INCREMENT_MS)
      .build()

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
      val pause = !playWhenReady
      isTtsPaused = pause
      readAloudCallbacks?.onReadAloudPauseOrResume(pause)
      startForegroundNotificationHelper(pause)
      return Futures.immediateVoidFuture()
    }

    @Suppress("SwitchIntDef")
    override fun handleSeek(
      mediaItemIndex: Int,
      positionMs: Long,
      seekCommand: Int
    ): ListenableFuture<*> {
      when (seekCommand) {
        Player.COMMAND_SEEK_BACK -> readAloudCallbacks?.onReadAloudRewind10s()
        Player.COMMAND_SEEK_FORWARD -> readAloudCallbacks?.onReadAloudForward10s()
        else -> Unit
      }
      return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
      stopReadAloudAndDismissNotification()
      return Futures.immediateVoidFuture()
    }

    fun updatePlaybackState(paused: Boolean) {
      isTtsPaused = paused
      invalidateState()
    }
  }

  override fun onBind(intent: Intent?): IBinder? {
    if (intent?.action == SERVICE_INTERFACE) {
      return super.onBind(intent)
    }
    return serviceBinder
  }

  fun registerCallBack(readAloudCallbacks: ReadAloudCallbacks?) {
    this.readAloudCallbacks = readAloudCallbacks
  }

  override fun onDestroy() {
    mediaSession?.release()
    mediaSession = null
    super.onDestroy()
  }

  override fun onTimeout(startId: Int) {
    // stop the service if the foreground service time is about to reach.
    stopReadAloudAndDismissNotification()
    super.onTimeout(startId)
  }

  class ReadAloudBinder(readAloudService: ReadAloudService) : Binder() {
    val service = WeakReference(readAloudService)
  }

  companion object {
    const val ACTION_STOP_TTS = "ACTION_STOP_TTS"
    const val ACTION_PAUSE_OR_RESUME_TTS = "ACTION_PAUSE_OR_RESUME_TTS"
    const val ACTION_REWIND_10 = "ACTION_REWIND_10"
    const val ACTION_FORWARD_10 = "ACTION_FORWARD_10"
    const val IS_TTS_PAUSE_OR_RESUME = "IS_TTS_PAUSE_OR_RESUME"
    const val SEEK_INCREMENT_MS = 10000L
  }
}
