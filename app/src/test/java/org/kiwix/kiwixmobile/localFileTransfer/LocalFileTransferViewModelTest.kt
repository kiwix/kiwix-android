package org.kiwix.kiwixmobile.localFileTransfer

import android.location.LocationManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    mockkObject(WifiDirectManager.Companion)
    every { WifiDirectManager.getFileName(any()) } returns "test.zim"
    every { permissionChecker.isAndroid13orAbove() } returns true
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns false
    every { kiwixDataStore.showShowCaseToUser } returns flowOf(false)
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
    unmockkObject(WifiDirectManager.Companion)
    Dispatchers.resetMain()
  }

  @Test
  fun `initializeWifiDirectManager updates UI state with transfer files`() = runTest {
    val uri: Uri = mockk()
    every { WifiDirectManager.getFileName(uri) } returns "test.zim"
    viewModel.initialize(listOf(uri), alertDialogShower)

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.transferFiles).hasSize(1)
      assertThat(state.transferFiles.first().fileUri).isEqualTo(uri)
      assertThat(state.transferFiles.first().fileName).isEqualTo("test.zim")
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

      viewModel.events.test {
        viewModel.onSearchMenuClicked()
        val event = awaitItem()
        assertThat(event).isInstanceOf(UiEvent.RequestPermission::class.java)
        assertThat((event as UiEvent.RequestPermission).permission)
          .isEqualTo(android.Manifest.permission.ACCESS_FINE_LOCATION)
      }
    }

  @Test
  fun `onSearchMenuClicked requests nearby wifi permission on Android 13 or above`() = runTest {
    // Already true from setUp()
    coEvery { permissionChecker.hasNearbyWifiPermission() } returns false

    viewModel.events.test {
      viewModel.onSearchMenuClicked()
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.RequestPermission::class.java)
      assertThat((event as UiEvent.RequestPermission).permission)
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
    viewModel.events.test {
      viewModel.onFileTransferComplete()
      assertThat(awaitItem()).isEqualTo(UiEvent.NavigateBack)
    }
  }

  @Test
  fun `showDialog emits ShowDialog event through events flow`() = runTest {
    val dialog = DialogEvent.ShowEnableWifiP2p
    viewModel.events.test {
      viewModel.showDialog(dialog)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog).isEqualTo(dialog)
    }
  }

  @Test
  fun `onPermissionGranted triggers search menu click`() = runTest {
    coEvery { permissionChecker.hasNearbyWifiPermission() } returns false

    viewModel.events.test {
      viewModel.onPermissionGranted()
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.RequestPermission::class.java)
    }
  }

  @Test
  fun `onPermissionRationaleRequired emits ShowDialog for nearby wifi`() = runTest {
    viewModel.events.test {
      viewModel.onPermissionRationaleRequired(android.Manifest.permission.NEARBY_WIFI_DEVICES)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog)
        .isEqualTo(DialogEvent.ShowNearbyWifiRationale)
    }
  }

  @Test
  fun `onPermissionRationaleRequired emits ShowDialog for fine location`() = runTest {
    viewModel.events.test {
      viewModel.onPermissionRationaleRequired(android.Manifest.permission.ACCESS_FINE_LOCATION)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog)
        .isEqualTo(DialogEvent.ShowLocationRationale)
    }
  }

  @Test
  fun `onPermissionRationaleRequired emits ShowDialog for storage`() = runTest {
    viewModel.events.test {
      viewModel.onPermissionRationaleRequired(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog)
        .isEqualTo(DialogEvent.ShowStorageRationale)
    }
  }

  @Test
  fun `onShowCaseDisplayed marks showcase as shown in data store`() = runTest {
    coEvery { kiwixDataStore.setShowCaseViewForFileTransferShown() } returns Unit
    viewModel.onShowCaseDisplayed()
    testDispatcher.scheduler.advanceUntilIdle()
    coVerify { kiwixDataStore.setShowCaseViewForFileTransferShown() }
  }

  @Test
  fun `onConnectionToPeersLost clears peers list`() = runTest {
    // First add some peers
    val device: WifiP2pDevice = mockk()
    val p2pDeviceList: WifiP2pDeviceList = mockk()
    every { p2pDeviceList.deviceList } returns listOf(device)
    viewModel.updateListOfAvailablePeers(p2pDeviceList)

    // Now simulate connection lost
    viewModel.onConnectionToPeersLost()

    viewModel.uiState.test {
      assertThat(awaitItem().peers).isEmpty()
    }
  }

  @Test
  fun `onFileStatusChanged updates file status at given index`() = runTest {
    val uri: Uri = mockk()
    every { WifiDirectManager.getFileName(uri) } returns "status_test.zim"
    viewModel.initialize(listOf(uri), alertDialogShower)

    viewModel.onFileStatusChanged(0, FileItem.FileStatus.SENT)

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.transferFiles.first().fileStatus).isEqualTo(FileItem.FileStatus.SENT)
    }
  }

  @Test
  fun `isLocationServiceEnabled returns true when Android 13 or above`() {
    // isAndroid13orAbove returns true from setUp()
    assertThat(viewModel.isLocationServiceEnabled).isTrue()
  }

  @Test
  fun `initialize sets isReceiver true when file list is empty`() = runTest {
    viewModel.initialize(emptyList(), alertDialogShower)

    viewModel.uiState.test {
      assertThat(awaitItem().isReceiver).isTrue()
    }
  }

  @Test
  fun `initialize sets isReceiver false when files are provided`() = runTest {
    val uri: Uri = mockk()
    every { WifiDirectManager.getFileName(uri) } returns "test.zim"
    viewModel.initialize(listOf(uri), alertDialogShower)

    viewModel.uiState.test {
      assertThat(awaitItem().isReceiver).isFalse()
    }
  }
}
