/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults.drawStopIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.downloader.downloadManager.HUNDERED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.ui.theme.DenimBlue400
import org.kiwix.kiwixmobile.core.ui.theme.MineShaftGray350

@Composable
fun ContentLoadingProgressBar(
  modifier: Modifier = Modifier,
  progressBarStyle: ProgressBarStyle = ProgressBarStyle.CIRCLE,
  progress: Int = ZERO,
  progressBarColor: Color = DenimBlue400,
  progressBarTrackColor: Color = MineShaftGray350
) {
  when (progressBarStyle) {
    ProgressBarStyle.CIRCLE -> {
      CircularProgressIndicator(
        modifier = modifier,
        color = progressBarColor,
        trackColor = progressBarTrackColor
      )
    }

    ProgressBarStyle.HORIZONTAL -> {
      LinearProgressIndicator(
        modifier = modifier.fillMaxWidth(),
        progress = { progress.toFloat() / HUNDERED },
        color = progressBarColor,
        trackColor = progressBarTrackColor,
        gapSize = ZERO.dp,
        strokeCap = StrokeCap.Butt,
        drawStopIndicator = {
          drawStopIndicator(
            drawScope = this,
            stopSize = ZERO.dp,
            color = progressBarTrackColor,
            strokeCap = StrokeCap.Butt
          )
        }
      )
    }
  }
}

enum class ProgressBarStyle {
  HORIZONTAL,
  CIRCLE
}
