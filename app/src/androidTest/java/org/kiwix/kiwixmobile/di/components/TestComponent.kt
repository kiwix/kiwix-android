/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.di.components

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import org.kiwix.kiwixmobile.data.DataModule
import org.kiwix.kiwixmobile.di.modules.ApplicationModule
import org.kiwix.kiwixmobile.di.modules.JNIModule
import org.kiwix.kiwixmobile.di.modules.TestNetworkModule
import org.kiwix.kiwixmobile.NetworkTest
import org.kiwix.kiwixmobile.ZimTest
import javax.inject.Singleton

/**
 * Created by mhutti1 on 13/04/17.
 */

@Singleton
@Component(modules = [ApplicationModule::class, TestNetworkModule::class, JNIModule::class, DataModule::class])
interface TestComponent : ApplicationComponent {

  @Component.Builder
  interface Builder {

    @BindsInstance fun context(context: Context): Builder
    fun build(): TestComponent
  }

  fun inject(zimTest: ZimTest)
  fun inject(networkTest: NetworkTest)
}
