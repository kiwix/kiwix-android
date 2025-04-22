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

package org.kiwix.kiwixmobile.core.utils.dialog

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_DEFAULT_PADDING_FOR_CONTENT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_ICON_END_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_MESSAGE_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_TITLE_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_URI_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml
import javax.inject.Inject

@Suppress("UnusedPrivateProperty")
class AlertDialogShower @Inject constructor(private val activity: Activity?) : DialogShower {
  val dialogState = mutableStateOf<Triple<KiwixDialog, Array<out () -> Unit>, Uri?>?>(null)

  @OptIn(ExperimentalMaterial3Api::class)
  override fun show(dialog: KiwixDialog, vararg clickListeners: () -> Unit, uri: Uri?) {
    dialogState.value = Triple(dialog, clickListeners, uri)
  }

  fun dismiss() {
    dialogState.value = null
  }
}

@Preview
@Composable
private fun Preview() {
  val alertDialog = AlertDialogShower(null).apply {
    dialogState.value = Triple(
      KiwixDialog.SaveOrOpenUnsupportedFiles,
      arrayOf({}),
      "https://www.google.com".toUri()
    )
  }
  DialogHost(alertDialog)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogHost(alertDialogShower: AlertDialogShower) {
  val dialogData = alertDialogShower.dialogState.value

  dialogData?.let { (dialog, clickListeners, uri) ->
    KiwixDialogTheme {
      BasicAlertDialog(
        onDismissRequest = {
          if (dialog.cancelable) {
            alertDialogShower.dismiss()
          }
        }
      ) {
        Surface(
          modifier = Modifier.wrapContentSize(),
          shape = MaterialTheme.shapes.small,
          tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
          Column(modifier = Modifier.padding(DIALOG_DEFAULT_PADDING_FOR_CONTENT)) {
            Row {
              DialogIcon(dialog)
              DialogTitle(dialog)
            }
            DialogMessage(dialog)
            ShowUri(uri)
            ShowCustomComposeView(dialog)
            ShowDialogButtons(dialog, clickListeners, alertDialogShower)
          }
        }
      }
    }
  }
}

@Composable
fun ShowCustomComposeView(dialog: KiwixDialog) {
  dialog.customComposeView?.let {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight(),
      contentAlignment = Alignment.TopStart
    ) {
      it.invoke()
    }
  }
}

@Composable fun DialogIcon(dialog: KiwixDialog) {
  dialog.icon?.let {
    Icon(
      painterResource(id = it),
      contentDescription = null,
      // Setting end padding to give space between icon and title
      modifier = Modifier.padding(end = DIALOG_ICON_END_PADDING)
    )
  }
}

@Composable
private fun DialogConfirmButton(
  dialog: KiwixDialog,
  dialogConfirmButtonClick: (() -> Unit)?,
  alertDialogShower: AlertDialogShower
) {
  val confirmButtonText = stringResource(id = dialog.confirmButtonText)
  if (confirmButtonText.isNotEmpty()) {
    TextButton(
      onClick = {
        alertDialogShower.dismiss()
        dialogConfirmButtonClick?.invoke()
      }
    ) {
      Text(text = confirmButtonText)
    }
  }
}

@Composable
private fun DialogDismissButton(
  dialog: KiwixDialog,
  dismissButtonClick: (() -> Unit)?,
  alertDialogShower: AlertDialogShower
) {
  dialog.dismissButtonText?.let {
    TextButton(
      onClick = {
        alertDialogShower.dismiss()
        dismissButtonClick?.invoke()
      }
    ) {
      Text(text = stringResource(id = it))
    }
  }
}

@Composable
private fun DialogNaturalButton(
  dialog: KiwixDialog,
  neutralButtonClick: (() -> Unit)?,
  alertDialogShower: AlertDialogShower
) {
  dialog.neutralButtonText?.let {
    TextButton(
      onClick = {
        alertDialogShower.dismiss()
        neutralButtonClick?.invoke()
      },
      modifier = Modifier.wrapContentWidth(Alignment.Start)
    ) {
      Text(text = stringResource(id = it))
    }
  }
}

@Composable
private fun ShowDialogButtons(
  dialog: KiwixDialog,
  clickListeners: Array<out () -> Unit>,
  alertDialogShower: AlertDialogShower
) {
  Row(
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth()
  ) {
    DialogNaturalButton(
      dialog,
      clickListeners.getOrNull(2),
      alertDialogShower
    )
    Spacer(modifier = Modifier.weight(1f))
    DialogDismissButton(dialog, clickListeners.getOrNull(1), alertDialogShower)
    DialogConfirmButton(dialog, clickListeners.getOrNull(0), alertDialogShower)
  }
}

@Composable
private fun DialogTitle(dialog: KiwixDialog) {
  dialog.title?.let {
    Text(
      text = stringResource(id = it),
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = DIALOG_TITLE_BOTTOM_PADDING)
    )
  }
}

@Composable
private fun DialogMessage(dialog: KiwixDialog) {
  val context = LocalContext.current
  dialog.message?.let {
    Text(
      text = context.getString(it, *bodyArguments(dialog)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = DIALOG_MESSAGE_BOTTOM_PADDING),
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShowUri(uri: Uri?) {
  val context = LocalContext.current
  uri?.let {
    Text(
      text = "</br><a href=$uri> <b>$uri</b>".fromHtml().toString(),
      color = MaterialTheme.colorScheme.primary,
      fontSize = DIALOG_URI_TEXT_SIZE,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
          onClick = {
            // nothing to do
          },
          onLongClick = {
            val clipboard =
              ContextCompat.getSystemService(context, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("External Url", "$uri")
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(
              context,
              R.string.external_link_copied_message,
              Toast.LENGTH_SHORT
            ).show()
          }
        )
    )
  }
}

private fun bodyArguments(dialog: KiwixDialog) =
  if (dialog is HasBodyFormatArgs) {
    dialog.args.toTypedArray()
  } else {
    emptyArray()
  }
