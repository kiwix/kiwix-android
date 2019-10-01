package org.kiwix.kiwixmobile.core.di.components

import android.app.Service
import dagger.BindsInstance
import dagger.Subcomponent
import org.kiwix.kiwixmobile.core.di.ServiceScope
import org.kiwix.kiwixmobile.core.di.modules.ServiceModule
import org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotService

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
