package org.kiwix.kiwixmobile.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.downloader.model.Base64String
import org.kiwix.kiwixmobile.core.downloader.model.toPainter
import org.kiwix.kiwixmobile.core.ui.theme.IconBeigeBackground
import org.kiwix.kiwixmobile.core.ui.theme.KiwixTheme
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BOOK_ICON_SIZE
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.FOUR_DP

const val ONLINE_LIBRARY_IMAGE_TEST_TAG = "onlineLibraryImageTestingTag"
const val OFFLINE_IMAGE_TEST_TAG = "localLibraryImageTestingTag"

@Composable
fun BookIcon(
  iconSource: String?,
  isOnlineLibrary: Boolean = false,
  modifier: Modifier = Modifier,
  size: Dp = BOOK_ICON_SIZE,
  contentDescription: String? = null,
  testTag: String? = if (isOnlineLibrary) ONLINE_LIBRARY_IMAGE_TEST_TAG else OFFLINE_IMAGE_TEST_TAG
) {
  val defaultContentDescription =
    stringResource(R.string.fav_icon) + (iconSource?.hashCode() ?: "")
  val imageModifier = modifier
    .size(size)
    .clip(RoundedCornerShape(FOUR_DP))
    .background(IconBeigeBackground)
    .let { if (testTag != null) it.testTag(testTag) else it }

  if (isOnlineLibrary) {
    AsyncImage(
      model = iconSource,
      contentDescription = contentDescription ?: defaultContentDescription,
      modifier = imageModifier,
      error = painterResource(R.drawable.default_zim_file_icon)
    )
  } else {
    Image(
      painter = Base64String(iconSource).toPainter(),
      contentDescription = contentDescription ?: defaultContentDescription,
      modifier = imageModifier
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun BookIconPreview() {
  KiwixTheme {
    BookIcon(
      iconSource = null,
      isOnlineLibrary = false
    )
  }
}
