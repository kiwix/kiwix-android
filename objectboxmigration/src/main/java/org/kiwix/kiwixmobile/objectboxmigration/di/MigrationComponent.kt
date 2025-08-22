/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.objectboxmigration.di

import dagger.Component
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.objectboxmigration.data.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.objectboxmigration.data.ObjectBoxToRoomMigrator
import org.kiwix.kiwixmobile.objectboxmigration.di.module.DatabaseModule
import org.kiwix.kiwixmobile.objectboxmigration.di.module.ObjectBoxMigrationModule

@MigrationScope
@Component(
  dependencies = [CoreComponent::class],
  modules = [ObjectBoxMigrationModule::class, DatabaseModule::class]
)
interface MigrationComponent {
  fun objectBoxToRoomMigrator(): ObjectBoxToRoomMigrator
  fun objectBoxToLibkiwixMigrator(): ObjectBoxToLibkiwixMigrator

  @Component.Factory
  interface Factory {
    fun create(coreComponent: CoreComponent): MigrationComponent
  }
}
