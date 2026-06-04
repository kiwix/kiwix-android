/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils.effects

import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.appcompat.app.AppCompatActivity
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

class RequestNotificationPermissionTest {
  private val alertDialogShower: AlertDialogShower = mockk()
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk()

  private lateinit var requestNotificationPermission: RequestNotificationPermission

  @BeforeEach
  fun init() {
    requestNotificationPermission =
      RequestNotificationPermission(alertDialogShower, kiwixPermissionChecker)
  }

  @AfterEach
  fun clear() {
    clearAllMocks()
  }

  @Test
  fun invokeWith_nonCoreMainActivity_returnsNone() {
    val activity: AppCompatActivity = mockk()
    val request = requestNotificationPermission.invokeWith(activity)

    assertEquals(NotificationPermissionAction.None, request)

    verify(exactly = 0) {
      alertDialogShower.show(any(), any())
    }

    verify(exactly = 0) {
      kiwixPermissionChecker.shouldShowRationale(any(), any())
    }
  }

  @Nested
  inner class CoreActivity {
    private val activity: CoreMainActivity = mockk()

    @Test
    fun invokeWith_rationaleNotRequired_returnsRequestNotificationPermission() {
      every {
        kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      } returns false

      val request = requestNotificationPermission.invokeWith(activity)

      assertEquals(NotificationPermissionAction.RequestNotificationPermission, request)

      verify(exactly = 1) {
        kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      }

      verify(exactly = 0) {
        alertDialogShower.show(any(), any())
      }
    }

    @Test
    fun invokeWith_rationaleRequired_showsDialogAndReturnsNone() {
      every {
        kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      } returns true

      val request = requestNotificationPermission.invokeWith(activity)

      assertEquals(
        NotificationPermissionAction.None,
        request
      )

      verify(exactly = 1) {
        kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
      }

      verify(exactly = 1) {
        alertDialogShower.show(
          KiwixDialog.NotificationPermissionDialog,
          any()
        )
      }
    }
  }
}
