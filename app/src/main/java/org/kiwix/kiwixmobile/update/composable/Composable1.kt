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

package org.kiwix.kiwixmobile.update.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kiwix.kiwixmobile.R.drawable
import org.kiwix.kiwixmobile.core.ui.components.ContentLoadingProgressBar
import org.kiwix.kiwixmobile.core.ui.components.KiwixButton
import org.kiwix.kiwixmobile.core.ui.components.ProgressBarStyle
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FIVE_DP
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.ONE_DP

@Composable
fun UpdateCard() {
  Column(
    modifier = Modifier
      .padding(16.dp)
      .fillMaxSize(),
    verticalArrangement = Arrangement.Center
  ) {
    Row(
      verticalAlignment = Alignment.Top
    ) {
      Image(
        modifier = Modifier.size(50.dp),
        painter = painterResource(drawable.kiwix_icon),
        contentDescription = null
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        LabelText(
          label = "Update available for Kiwix",
          style = MaterialTheme.typography.titleMedium,
          color = Color.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        LabelText(
          label = "Download size: 1.4 MB",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))

    LabelText(
      label = "Download the latest version",
      style = MaterialTheme.typography.bodyMedium,
      color = Color.DarkGray
    )
    KiwixButton(
      modifier = Modifier.fillMaxWidth(),
      clickListener = { /* Handle update */ },
      buttonText = "UPDATE"
    )

    DownloadInfoRow(
      currentSize = "73.73 kB",
      totalSize = "1.46 MB",
      progress = 0.05f,
      onCancel = {}
    )
  }
}

@Composable
fun DownloadInfoRow(
  currentSize: String,
  totalSize: String,
  progress: Float,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier
) {
  val contentColor = Color.Gray
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        DownloadText(
          text = "$currentSize/$totalSize",
          contentColor = contentColor
        )
        DownloadText(
          text = "${(progress * 100).toInt()}%",
          contentColor = contentColor
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      ContentLoadingProgressBar(
        progressBarStyle = ProgressBarStyle.HORIZONTAL,
        modifier = Modifier
          .padding(horizontal = ONE_DP, vertical = FIVE_DP),
        progress = 5,
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Icon(
      imageVector = Icons.Default.Close,
      contentDescription = "Cancel upload",
      tint = contentColor,
      modifier = Modifier
        .size(20.dp)
        .clickable(onClick = onCancel)
    )
  }
}

@Composable
fun LabelText(
  label: String,
  style: TextStyle = MaterialTheme.typography.bodyMedium,
  color: Color = Color.DarkGray
) {
  Text(
    text = label,
    style = style,
    color = color
  )
}

@Composable
fun DownloadText(
  text: String,
  contentColor: Color
) {
  Text(
    text = text,
    fontSize = 12.sp,
    color = contentColor,
    fontWeight = FontWeight.Medium
  )
}

@Preview(showBackground = true)
@Composable
fun PreviewUpdateCard() {
  MaterialTheme {
    UpdateCard()
  }
}
