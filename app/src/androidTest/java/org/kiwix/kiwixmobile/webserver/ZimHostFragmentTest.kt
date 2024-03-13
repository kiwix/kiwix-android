/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.webserver

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ZimHostFragmentTest {
  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private lateinit var activityScenario: ActivityScenario<KiwixMainActivity>

  private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    arrayOf(
      Manifest.permission.POST_NOTIFICATIONS,
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.NEARBY_WIFI_DEVICES,
      Manifest.permission.ACCESS_NETWORK_STATE
    )
  } else {
    arrayOf(
      Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_NETWORK_STATE
    )
  }

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)
  private var context: Context? = null

  @Before
  fun waitForIdle() {
    context = InstrumentationRegistry.getInstrumentation().targetContext
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context)
      }
      waitForIdle()
    }
    context?.let {
      sharedPreferenceUtil = SharedPreferenceUtil(it).apply {
        setIntroShown()
        putPrefWifiOnly(false)
        setIsPlayStoreBuildType(true)
        prefIsTest = true
        playStoreRestrictionPermissionDialog = false
      }
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun testZimHostFragment() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      activityScenario.onActivity {
        it.navigate(R.id.libraryFragment)
      }
      zimHost(ZimHostRobot::refreshLibraryList)
      // delete all the ZIM files showing in the LocalLibrary
      // screen to properly test the scenario.
      library {
        deleteZimIfExists()
        assertNoFilesTextDisplayed()
      }
      loadZimFileInApplication("testzim.zim")
      loadZimFileInApplication("small.zim")
      zimHost {
        refreshLibraryList()
        assertZimFilesLoaded()
        openZimHostFragment()

        // Check if server is already started
        stopServerIfAlreadyStarted()

        // Check if both zim file are selected or not to properly run our test case
        selectZimFileIfNotAlreadySelected()

        clickOnTestZim()

        // Start the server with one ZIM file
        startServer()
        assertServerStarted()

        // Check that only one ZIM file is hosted on the server
        assertItemHostedOnServer(1)

        // Stop the server
        stopServer()
        assertServerStopped()

        // Select the test ZIM file to host on the server
        clickOnTestZim()

        // Start the server with two ZIM files
        startServer()
        assertServerStarted()

        // Check that both ZIM files are hosted on the server
        assertItemHostedOnServer(2)

        // Unselect the test ZIM to test restarting server functionality
        clickOnTestZim()

        // Check if the server is running
        assertServerStarted()

        // Check that only one ZIM file is hosted on the server after unselecting
        assertItemHostedOnServer(1)

        // finally close the server at the end of test case
        stopServer()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  private fun loadZimFileInApplication(zimFileName: String) {
    val loadFileStream =
      ZimHostFragmentTest::class.java.classLoader.getResourceAsStream(zimFileName)
    val zimFile = File(sharedPreferenceUtil.prefStorage, zimFileName)
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
  }

  @After
  fun setIsTestPreference() {
    sharedPreferenceUtil.apply {
      setIsPlayStoreBuildType(false)
      prefIsTest = false
    }
  }
}
