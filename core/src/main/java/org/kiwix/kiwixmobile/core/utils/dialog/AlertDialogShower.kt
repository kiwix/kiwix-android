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

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ui.models.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.KiwixDialogTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_BUTTONS_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_BUTTON_ROW_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_BUTTON_TEXT_LETTER_SPACING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_DEFAULT_PADDING_FOR_CONTENT
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_ICON_END_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_MESSAGE_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_TITLE_BOTTOM_PADDING
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_TITLE_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.DIALOG_URI_TEXT_SIZE
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml
import org.kiwix.kiwixmobile.core.utils.ZERO
import javax.inject.Inject

const val ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG = "alertDialogConfirmButtonTestingTag"
const val ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG = "alertDialogDismissButtonTestingTag"
const val ALERT_DIALOG_NATURAL_BUTTON_TESTING_TAG = "alertDialogNaturalButtonTestingTag"
const val ALERT_DIALOG_TITLE_TEXT_TESTING_TAG = "alertDialogTitleTextTestingTag"
const val ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG = "alertDialogMessageTextTestingTag"

@Suppress("UnusedPrivateProperty")
class AlertDialogShower @Inject constructor() : DialogShower {
  val dialogState = mutableStateOf<Triple<KiwixDialog, Array<out () -> Unit>, Uri?>?>(null)

  @OptIn(ExperimentalMaterial3Api::class)
  override fun show(dialog: KiwixDialog, vararg clickListeners: () -> Unit, uri: Uri?) {
    dialogState.value = Triple(dialog, clickListeners, uri)
  }

  fun dismiss() {
    dialogState.value = null
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogHost(alertDialogShower: AlertDialogShower) {
  val dialogData = alertDialogShower.dialogState.value

  dialogData?.let { (dialog, clickListeners, uri) ->
    KiwixBasicDialogFrame(
      onDismissRequest = { alertDialogShower.dismiss() },
      cancelable = dialog.cancelable,
    ) {
      Column(Modifier.padding(horizontal = DIALOG_DEFAULT_PADDING_FOR_CONTENT)) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          DialogIcon(dialog)
          DialogTitle(dialog.title)
        }
        DialogMessage(dialog)
        ShowUri(uri)
        ShowCustomComposeView(dialog)
      }
      ShowDialogButtons(dialog, clickListeners, alertDialogShower)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiwixBasicDialogFrame(
  onDismissRequest: () -> Unit,
  cancelable: Boolean = true,
  content: @Composable ColumnScope.() -> Unit
) {
  KiwixDialogTheme {
    BasicAlertDialog(
      onDismissRequest = {
        if (cancelable) onDismissRequest()
      },
      properties = DialogProperties(usePlatformDefaultWidth = false),
      modifier = Modifier.padding(DIALOG_PADDING)
    ) {
      Surface(
        modifier = Modifier
          .wrapContentSize()
          .wrapContentHeight(),
        shape = MaterialTheme.shapes.extraSmall,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        color = MaterialTheme.colorScheme.background
      ) {
        Column(
          modifier = Modifier
            .padding(top = DIALOG_DEFAULT_PADDING_FOR_CONTENT),
          content = content
        )
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
        .wrapContentHeight()
        .padding(bottom = dialog.customComposeViewBottomPadding),
      contentAlignment = Alignment.TopStart
    ) {
      it.invoke()
    }
  }
}

@Composable
fun DialogIcon(dialog: KiwixDialog) {
  dialog.iconItem?.let {
    Icon(
      it.toPainter(),
      contentDescription = stringResource(R.string.fav_icon),
      // Setting end padding to give space between icon and title
      modifier = Modifier
        .size(DIALOG_ICON_SIZE)
        .padding(end = DIALOG_ICON_END_PADDING),
      tint = Color.Unspecified,
    )
  }
}

@Composable
fun DialogConfirmButton(
  confirmButtonText: String,
  dialogConfirmButtonClick: (() -> Unit)?,
  startEndPadding: Dp = DIALOG_DEFAULT_PADDING_FOR_CONTENT,
  alertDialogShower: AlertDialogShower?
) {
  if (confirmButtonText.isNotEmpty()) {
    TextButton(
      onClick = {
        alertDialogShower?.dismiss()
        dialogConfirmButtonClick?.invoke()
      },
      modifier = Modifier.semantics { testTag = ALERT_DIALOG_CONFIRM_BUTTON_TESTING_TAG },
      contentPadding = PaddingValues(horizontal = startEndPadding)
    ) {
      Text(
        text = confirmButtonText.uppercase(),
        fontWeight = FontWeight.Medium,
        letterSpacing = DIALOG_BUTTON_TEXT_LETTER_SPACING,
        fontSize = DIALOG_BUTTONS_TEXT_SIZE
      )
    }
  }
}

@Composable
fun DialogDismissButton(
  dismissButtonTextId: Int?,
  dismissButtonClick: (() -> Unit)?,
  alertDialogShower: AlertDialogShower?
) {
  dismissButtonTextId?.let {
    TextButton(
      onClick = {
        alertDialogShower?.dismiss()
        dismissButtonClick?.invoke()
      },
      modifier = Modifier.semantics { testTag = ALERT_DIALOG_DISMISS_BUTTON_TESTING_TAG },
      contentPadding = PaddingValues(horizontal = ZERO.dp)
    ) {
      Text(
        text = stringResource(id = it).uppercase(),
        fontWeight = FontWeight.Medium,
        letterSpacing = DIALOG_BUTTON_TEXT_LETTER_SPACING,
        fontSize = DIALOG_BUTTONS_TEXT_SIZE
      )
    }
  }
}

@Composable
private fun DialogNaturalButton(
  dialog: KiwixDialog,
  neutralButtonClick: (() -> Unit)?,
  startEndPadding: Dp = DIALOG_DEFAULT_PADDING_FOR_CONTENT,
  alertDialogShower: AlertDialogShower
) {
  dialog.neutralButtonText?.let {
    TextButton(
      onClick = {
        alertDialogShower.dismiss()
        neutralButtonClick?.invoke()
      },
      modifier = Modifier
        .semantics { testTag = ALERT_DIALOG_NATURAL_BUTTON_TESTING_TAG },
      contentPadding = PaddingValues(horizontal = startEndPadding)
    ) {
      Text(
        text = stringResource(id = it).uppercase(),
        fontWeight = FontWeight.Medium,
        letterSpacing = DIALOG_BUTTON_TEXT_LETTER_SPACING,
        fontSize = DIALOG_BUTTONS_TEXT_SIZE
      )
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
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = DIALOG_BUTTON_ROW_BOTTOM_PADDING)
  ) {
    DialogNaturalButton(
      dialog,
      clickListeners.getOrNull(2),
      alertDialogShower = alertDialogShower
    )
    Spacer(modifier = Modifier.weight(1f))
    DialogDismissButton(
      dialog.dismissButtonText,
      clickListeners.getOrNull(1),
      alertDialogShower
    )
    DialogConfirmButton(
      stringResource(dialog.confirmButtonText),
      clickListeners.getOrNull(0),
      alertDialogShower = alertDialogShower
    )
  }
}

@Composable
fun DialogTitle(title: Int?) {
  title?.let {
    Text(
      text = stringResource(id = it),
      style = MaterialTheme.typography.titleSmall.copy(
        fontSize = DIALOG_TITLE_TEXT_SIZE,
        fontWeight = FontWeight.Medium
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = DIALOG_TITLE_BOTTOM_PADDING)
        .semantics { testTag = ALERT_DIALOG_TITLE_TEXT_TESTING_TAG }
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
        .padding(bottom = DIALOG_MESSAGE_BOTTOM_PADDING)
        .semantics { testTag = ALERT_DIALOG_MESSAGE_TEXT_TESTING_TAG },
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
      textDecoration = TextDecoration.Underline,
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
