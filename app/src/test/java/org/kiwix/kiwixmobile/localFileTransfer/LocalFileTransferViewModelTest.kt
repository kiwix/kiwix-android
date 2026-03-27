package org.kiwix.kiwixmobile.localFileTransfer

import android.location.LocationManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import io.mockk.mockkObject
import io.mockk.unmockkObject

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFileTransferViewModelTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val wifiDirectManager: WifiDirectManager = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk()
  private val locationManager: LocationManager = mockk()
  private val permissionChecker: KiwixPermissionChecker = mockk()

  private lateinit var viewModel: LocalFileTransferViewModel
  private val testDispatcher = StandardTestDispatcher()

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    mockkObject(Log)
    every { permissionChecker.isAndroid13orAbove() } returns true
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns false
    viewModel = LocalFileTransferViewModel(
      kiwixDataStore,
      wifiDirectManager,
      locationManager,
      permissionChecker
    ).apply {
      initialize(listOf(), alertDialogShower)
    }
  }

  @AfterEach
  fun tearDown() {
    unmockkObject(Log)
    Dispatchers.resetMain()
  }

  @Test
  fun `initializeWifiDirectManager updates UI state with transfer files`() = runTest {
    val files = listOf(Uri.parse("test.zim"))
    viewModel.initialize(files, alertDialogShower)

    viewModel.uiState.test {
      assertThat(awaitItem().transferFiles).isEqualTo(files)
    }
  }

  @Test
  fun `onDeviceSelected delegates to wifiDirectManager`() {
    val device: WifiP2pDevice = mockk()
    viewModel.onDeviceSelected(device)
    verify { wifiDirectManager.sendToDevice(device) }
  }

  @Test
  fun `onSearchMenuClicked requests permission when fine location is not granted on older Android`() =
    runTest {
      every { permissionChecker.isAndroid13orAbove() } returns false
      coEvery { permissionChecker.hasFineLocationPermission() } returns false
      // Re-initialize to pick up the false value for android13OrAbove property
      viewModel = LocalFileTransferViewModel(
        kiwixDataStore,
        wifiDirectManager,
        locationManager,
        permissionChecker
      )

      viewModel.permissionEvent.test {
        viewModel.onSearchMenuClicked()
        val event = awaitItem()
        assertThat(event).isInstanceOf(PermissionAction.RequestPermission::class.java)
        assertThat((event as PermissionAction.RequestPermission).permission)
          .isEqualTo(android.Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }

  @Test
  fun `onSearchMenuClicked requests nearby wifi permission on Android 13 or above`() = runTest {
    // Already true from setUp()
    coEvery { permissionChecker.hasNearbyWifiPermission() } returns false

    viewModel.permissionEvent.test {
      viewModel.onSearchMenuClicked()
      val event = awaitItem()
      assertThat(event).isInstanceOf(PermissionAction.RequestPermission::class.java)
      assertThat((event as PermissionAction.RequestPermission).permission)
        .isEqualTo(android.Manifest.permission.NEARBY_WIFI_DEVICES)
    }
  }

  @Test
  fun `updateListOfAvailablePeers updates UI state and stops searching`() = runTest {
    val device: WifiP2pDevice = mockk()
    val p2pDeviceList: WifiP2pDeviceList = mockk()
    every { p2pDeviceList.deviceList } returns listOf(device)

    viewModel.updateListOfAvailablePeers(p2pDeviceList)

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.peers).contains(device)
      assertThat(state.isPeerSearching).isFalse()
    }
  }

  @Test
  fun `onFileTransferComplete emits NavigateBack event`() = runTest {
    viewModel.onFileTransferComplete()

    viewModel.navigationEvent.test {
      assertThat(awaitItem()).isEqualTo(NavigationEvent.NavigateBack)
    }
  }

  @Test
  fun `showDialog emits dialog event through channel`() = runTest {
    val event = DialogEvent.ShowEnableWifiP2p
    viewModel.showDialog(event)

    viewModel.dialogEvent.test {
      assertThat(awaitItem()).isEqualTo(event)
    }
  }
}
