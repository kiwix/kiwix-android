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

package org.kiwix.kiwixmobile.di.components

import android.app.Service
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.di.ServiceScope
import org.kiwix.kiwixmobile.di.modules.ServiceModule
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService

@Subcomponent(modules = [ServiceModule::class])
@ServiceScope
interface ServiceComponent {
  fun inject(hotspotService: HotspotService)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance fun service(service: Service): Builder

    fun build(): ServiceComponent
  }
}
