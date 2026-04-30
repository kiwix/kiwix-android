package org.kiwix.kiwixmobile.core.utils.shadows

import android.content.Context
import com.getkeepsafe.relinker.ReLinker
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(ReLinker::class)
class ShadowReLinker {
  companion object {
    @Implementation
    @JvmStatic
    fun loadLibrary(context: Context, library: String) {
      // No-op for Robolectric tests
    }

    @Implementation
    @JvmStatic
    fun loadLibrary(context: Context, library: String, version: String) {
      // No-op for Robolectric tests
    }

    @Implementation
    @JvmStatic
    fun force(): ReLinker.ReLinkerInstance {
      return ReLinker.force()
    }
  }
}
