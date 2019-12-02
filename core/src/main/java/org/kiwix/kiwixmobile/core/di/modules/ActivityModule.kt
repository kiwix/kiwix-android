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
package org.kiwix.kiwixmobile.core.di.modules

import android.app.Activity
import android.view.Menu
import dagger.Binds
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainContract
import org.kiwix.kiwixmobile.core.main.MainMenu
import org.kiwix.kiwixmobile.core.main.MainMenu.Factory
import org.kiwix.kiwixmobile.core.main.MainMenu.MenuClickListener
import org.kiwix.kiwixmobile.core.main.MainPresenter
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.DialogShower

@Module
abstract class ActivityModule {
  @Binds
  @ActivityScope
  abstract fun bindDialogShower(alertDialogShower: AlertDialogShower): DialogShower

  @Module
  companion object {
    @JvmStatic
    @Provides
    @ActivityScope
    fun providesMainPresenter(dataSource: DataSource): MainContract.Presenter =
      MainPresenter(dataSource)

    @JvmStatic
    @Provides
    @ActivityScope
    fun providesMainMenuFactory(activity: Activity, zimReaderContainer: ZimReaderContainer):
      MainMenu.Factory = object : Factory {
      override fun create(
        menu: Menu,
        webViews: MutableList<KiwixWebView>,
        urlIsValid: Boolean,
        menuClickListener: MenuClickListener
      ): MainMenu = MainMenu(
        activity,
        zimReaderContainer.zimFileReader,
        menu,
        webViews,
        urlIsValid,
        menuClickListener
      )
    }
  }
}
