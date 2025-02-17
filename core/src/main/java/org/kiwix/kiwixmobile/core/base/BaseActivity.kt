/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.base

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {
  @Inject
  lateinit var kiwixDataStore: KiwixDataStore
  //
  // /**
  //  * Apply the currently selected language to the base context
  //  * so that Compose can properly localize everything.
  //  */
  // override fun attachBaseContext(newBase: Context) {
  //   val kiwixDataStore = KiwixDataStore(newBase)
  //   val localizedContext = runBlocking {
  //     LanguageUtils.handleLocaleChange(
  //       newBase,
  //       kiwixDataStore.prefLanguage.first(),
  //       kiwixDataStore
  //     )
  //   }
  //   super.attachBaseContext(localizedContext)
  // }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(Color.WHITE, Color.BLACK)
    )
    super.onCreate(savedInstanceState)
  }
}
