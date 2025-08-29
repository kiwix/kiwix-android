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

package org.kiwix.kiwixmobile.migration

import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.data.ObjectBoxDataMigrationHandler
import org.kiwix.kiwixmobile.migration.data.ObjectBoxToLibkiwixMigrator
import org.kiwix.kiwixmobile.migration.data.ObjectBoxToRoomMigrator
import org.kiwix.kiwixmobile.migration.di.component.DaggerMigrationComponent
import javax.inject.Inject

class ObjectBoxMigrationHandler @Inject constructor(
  private val objectBoxToRoomMigrator: ObjectBoxToRoomMigrator,
  private val objectBoxToLibkiwixMigrator: ObjectBoxToLibkiwixMigrator
) : ObjectBoxDataMigrationHandler {
  override suspend fun migrate() {
    val migrationComponent = DaggerMigrationComponent.builder()
      .coreComponent(CoreApp.coreComponent)
      .build()

    // Inject dependencies into migrators
    migrationComponent.inject(objectBoxToRoomMigrator)
    migrationComponent.inject(objectBoxToLibkiwixMigrator)
    objectBoxToRoomMigrator.migrateObjectBoxDataToRoom()
    objectBoxToLibkiwixMigrator.migrateObjectBoxDataToLibkiwix()
  }
}
