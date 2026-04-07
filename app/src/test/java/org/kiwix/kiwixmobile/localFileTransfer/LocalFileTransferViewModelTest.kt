package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.localFileTransfer.DialogEvent.ShowEnableLocationServices
import org.kiwix.kiwixmobile.localFileTransfer.DialogEvent.ShowEnableWifiP2p
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class LocalFileTransferViewModelTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val wifiDirectManager: WifiDirectManager = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk()
  private val locationManager: LocationManager = mockk()
  private val permissionChecker: KiwixPermissionChecker = mockk()

  private lateinit var viewModel: LocalFileTransferViewModel

  @RegisterExtension
  val dispatcherRule = MainDispatcherRule()

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    mockkObject(Log)
    mockkObject(WifiDirectManager.Companion)
    every { WifiDirectManager.getFileName(any()) } returns "test.zim"
    every { permissionChecker.isAndroid13orAbove() } returns true
    every { permissionChecker.isAndroid8OrAbove() } returns true
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns false
    every { kiwixDataStore.showShowCaseToUser } returns flowOf(false)
    createViewModel()
  }

  private fun createViewModel() {
    viewModel = LocalFileTransferViewModel(
      kiwixDataStore,
      wifiDirectManager,
      locationManager,
      permissionChecker
    )
  }

  @AfterEach
  fun tearDown() {
    unmockkObject(Log)
    unmockkObject(WifiDirectManager.Companion)
  }

  @Test
  fun `initial state is correct`() = runTest {
    val state = viewModel.uiState.value

    assertThat(state.isPeerSearching).isFalse
    assertThat(state.peers.isEmpty()).isTrue
  }

  @Test
  fun `initialize sets isReceiver true when file list is empty`() = runTest {
    viewModel.initialize(emptyList(), alertDialogShower)

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.isReceiver).isTrue()
      assertThat(state.transferFiles).isEmpty()
    }
  }

  @Test
  fun `initialize sets isReceiver false when files are provided`() = runTest {
    val uri: Uri = mockk()
    every { WifiDirectManager.getFileName(uri) } returns "test.zim"
    viewModel.initialize(listOf(uri), alertDialogShower)

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.isReceiver).isFalse()
      assertThat(state.transferFiles).hasSize(1)
    }
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
  fun `should update shouldShowShowCase when datastore emits`() = runTest {
    val flow = MutableSharedFlow<Boolean>(replay = 1)

    every { kiwixDataStore.showShowCaseToUser } returns flow
    every { permissionChecker.isAndroid13orAbove() } returns false
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns false

    createViewModel()

    flow.emit(true)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.shouldShowShowCase)

    flow.emit(false)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.shouldShowShowCase)
  }

  @Test
  fun `should set isWritePermissionRequired to true when permission required`() = runTest {
    every { kiwixDataStore.showShowCaseToUser } returns flowOf(false)
    coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns false
    every { permissionChecker.isAndroid13orAbove() } returns false
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns true

    createViewModel()

    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.isWritePermissionRequired)
  }

  @Test
  fun `should set isWritePermissionRequired to false when permission is not required`() = runTest {
    every { kiwixDataStore.showShowCaseToUser } returns flowOf(false)
    coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns true
    every { permissionChecker.isAndroid13orAbove() } returns true
    coEvery { permissionChecker.isWriteExternalStoragePermissionRequired() } returns false

    createViewModel()

    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isWritePermissionRequired)
  }

  @Test
  fun `onSearchMenuClicked requests permission when fine location is not granted on older Android`() =
    runTest {
      every { permissionChecker.isAndroid13orAbove() } returns false
      coEvery { permissionChecker.hasFineLocationPermission() } returns false
      // Re-initialize to pick up the false value for android13OrAbove property
      createViewModel()

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
  fun `onSearchMenuClicked requests write storage permission on older Android`() = runTest {
    every { permissionChecker.isAndroid13orAbove() } returns false
    coEvery { permissionChecker.hasFineLocationPermission() } returns true
    coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns false
    createViewModel()
    viewModel.events.test {
      viewModel.onSearchMenuClicked()
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.RequestPermission::class.java)
      assertThat((event as UiEvent.RequestPermission).permission)
        .isEqualTo(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
  }

  @Test
  fun `onSearchMenuClicked does not requests write storage permission on Android 13 or above`() =
    runTest {
      every { permissionChecker.isAndroid13orAbove() } returns true
      coEvery { permissionChecker.hasNearbyWifiPermission() } returns true
      coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
      createViewModel()
      viewModel.events.test {
        viewModel.onSearchMenuClicked()
        val event = awaitItem()
        assertThat(event).isNotInstanceOf(UiEvent.RequestPermission::class.java)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `onSearchMenuClicked requests requestEnableWifiP2pServices when wifiP2PIsNotEnabled`() =
    runTest {
      every { wifiDirectManager.isWifiP2pEnabled } returns false
      coEvery { permissionChecker.hasNearbyWifiPermission() } returns true
      coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
      createViewModel()
      viewModel.events.test {
        viewModel.onSearchMenuClicked()
        val event = awaitItem()
        assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
        assertThat((event as UiEvent.ShowDialog).dialog).isEqualTo(ShowEnableWifiP2p)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `onSearchMenuClicked requestEnableLocationServices when location service is not enabled`() =
    runTest {
      every { wifiDirectManager.isWifiP2pEnabled } returns true
      coEvery { permissionChecker.hasFineLocationPermission() } returns true
      coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { permissionChecker.isAndroid13orAbove() } returns false
      every { permissionChecker.isAndroid8OrAbove() } returns true
      every { locationManager.isProviderEnabled(any()) } returns false
      createViewModel()
      viewModel.events.test {
        viewModel.onSearchMenuClicked()
        val event = awaitItem()
        assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
        assertThat((event as UiEvent.ShowDialog).dialog).isEqualTo(ShowEnableLocationServices)
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `onSearchMenuClicked starts discoverPeerDevices when all conditions are true`() =
    runTest {
      every { wifiDirectManager.isWifiP2pEnabled } returns true
      coEvery { permissionChecker.hasFineLocationPermission() } returns true
      coEvery { permissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { permissionChecker.isAndroid13orAbove() } returns false
      every { permissionChecker.isAndroid8OrAbove() } returns true
      every { locationManager.isProviderEnabled(any()) } returns true
      createViewModel()
      viewModel.onSearchMenuClicked()
      advanceUntilIdle()
      viewModel.uiState.test {
        val state = awaitItem()
        assertThat(state.isPeerSearching).isTrue()
        cancelAndIgnoreRemainingEvents()
      }
      verify { wifiDirectManager.discoverPeerDevices() }
    }

  @Test
  fun `onDeviceSelected delegates to wifiDirectManager`() {
    val device: WifiP2pDevice = mockk()
    viewModel.onDeviceSelected(device)
    verify { wifiDirectManager.sendToDevice(device) }
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
      viewModel.onPermissionRationaleRequired(NEARBY_WIFI_DEVICES)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog)
        .isEqualTo(DialogEvent.ShowNearbyWifiRationale)
    }
  }

  @Test
  fun `onPermissionRationaleRequired emits ShowDialog for fine location`() = runTest {
    viewModel.events.test {
      viewModel.onPermissionRationaleRequired(ACCESS_FINE_LOCATION)
      val event = awaitItem()
      assertThat(event).isInstanceOf(UiEvent.ShowDialog::class.java)
      assertThat((event as UiEvent.ShowDialog).dialog)
        .isEqualTo(DialogEvent.ShowLocationRationale)
    }
  }

  @Test
  fun `onPermissionRationaleRequired emits ShowDialog for storage`() = runTest {
    viewModel.events.test {
      viewModel.onPermissionRationaleRequired(WRITE_EXTERNAL_STORAGE)
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
    dispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    coVerify { kiwixDataStore.setShowCaseViewForFileTransferShown() }
  }

  @Test
  fun `onUserDeviceDetailsAvailable updates deviceName`() = runTest {
    val name = "Google Pixel 7a"
    val device = mockk<WifiP2pDevice>().apply {
      deviceName = name
    }
    viewModel.onUserDeviceDetailsAvailable(device)

    viewModel.uiState.test {
      assertThat(awaitItem().deviceName).isEqualTo(name)
      cancelAndIgnoreRemainingEvents()
    }
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
  fun `onFilesForTransferAvailable updates the transferFiles list`() = runTest {
    val uri: Uri = mockk()
    val name = "status.zim"
    every { WifiDirectManager.getFileName(uri) } returns "status_test.zim"
    val fileItem = mockk<FileItem>().apply {
      every { fileName } returns name
      every { fileUri } returns uri
    }
    viewModel.onFilesForTransferAvailable(listOf(fileItem))
    viewModel.uiState.test {
      val newFiles = awaitItem().transferFiles
      assertThat(newFiles).hasSize(1)
      assertThat(newFiles.first().fileUri).isEqualTo(uri)
      assertThat(newFiles.first().fileName).isEqualTo(name)
      cancelAndIgnoreRemainingEvents()
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
  fun `stopWifiDirectManager and remove callback in onCleared`() {
    createViewModel()
    viewModel.onClearedExposed()
    verify {
      wifiDirectManager.stopWifiDirectManager()
      wifiDirectManager.callbacks = null
    }
  }
}
