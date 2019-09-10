/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.di.components

import android.app.Service
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.di.ServiceScope
import org.kiwix.kiwixmobile.di.modules.ServiceModule
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotService

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
