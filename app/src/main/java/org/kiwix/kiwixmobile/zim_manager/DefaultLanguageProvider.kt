package org.kiwix.kiwixmobile.zim_manager

import android.content.Context
import javax.inject.Inject

class DefaultLanguageProvider @Inject constructor(private val context: Context) {
  fun provide() = Language(
    context.resources.configuration.locale.isO3Language,
    true,
    1
  )
}
