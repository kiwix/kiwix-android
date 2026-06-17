package org.kiwix.kiwixmobile.zimManager

import android.os.Build
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.TestApplication
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class Fat32CheckerTest {
  abstract class BaseTest {
    protected val kiwixDataStore: KiwixDataStore = mockk()
    protected val fileSystemChecker: FileSystemChecker = mockk()
    protected lateinit var fat32Checker: Fat32Checker

    @Rule
    @JvmField
    val dispatcherRule = MainDispatcherRule()

    protected val pathWithSpace: String = File(System.getProperty("java.io.tmpdir")!!).absolutePath
    protected val pathWithoutSpace: String = File("/nonexistent_kiwix_test_storage_path").absolutePath

    protected lateinit var selectedStorage: MutableStateFlow<String>

    @Before
    fun setup() {
      assumeTrue(File(pathWithSpace).freeSpace > Fat32Checker.FOUR_GIGABYTES_IN_BYTES)
      assumeTrue(File(pathWithoutSpace).freeSpace == 0L)
    }

    @After
    fun teardown() {
      if (::fat32Checker.isInitialized) fat32Checker.dispose()
      unmockkAll()
      clearAllMocks()
    }

    protected fun createChecker(
      initialPath: String,
      checkers: List<FileSystemChecker> = listOf(fileSystemChecker)
    ): Fat32Checker {
      selectedStorage = MutableStateFlow(initialPath)
      every { kiwixDataStore.selectedStorage } returns selectedStorage

      return Fat32Checker(kiwixDataStore, checkers, dispatcherRule.dispatcher)
        .also { fat32Checker = it }
    }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class LowSpaceTests : BaseTest() {
    @Test
    fun `emits detecting then NotEnoughSpace when free space is less than 4GB`() =
      runTest(dispatcherRule.dispatcher) {
        val checker = createChecker(pathWithoutSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun `does not invoke file system checker when space is insufficient`() =
      runTest(dispatcherRule.dispatcher) {
        val checker = createChecker(pathWithoutSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
          cancelAndIgnoreRemainingEvents()
        }

        verify(exactly = 0) {
          fileSystemChecker.checkFilesystemSupports4GbFiles(any())
        }
      }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class CoreBehaviorTests : BaseTest() {
    @Test
    fun `emits CanWrite4GbFile when checker returns CAN_WRITE_4GB`() =
      runTest(dispatcherRule.dispatcher) {
        every { fileSystemChecker.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

        val checker = createChecker(pathWithSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
        }
      }

    @Test
    fun `emits CannotWrite4GbFile when checker returns CANNOT_WRITE_4GB`() =
      runTest(dispatcherRule.dispatcher) {
        every { fileSystemChecker.checkFilesystemSupports4GbFiles(any()) } returns CANNOT_WRITE_4GB

        val checker = createChecker(pathWithSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
        }
      }

    @Test
    fun `emits CannotWrite4GbFile when checker returns INCONCLUSIVE`() =
      runTest(dispatcherRule.dispatcher) {
        every { fileSystemChecker.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE

        val checker = createChecker(pathWithSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
        }
      }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class CheckerChainTests : BaseTest() {
    @Test
    fun `returns CanWrite when second checker succeeds`() = runTest(dispatcherRule.dispatcher) {
      val c1 = mockk<FileSystemChecker>()
      val c2 = mockk<FileSystemChecker>()

      every { c1.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
      every { c2.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

      val checker = createChecker(pathWithSpace, listOf(c1, c2))

      checker.fileSystemStates.test {
        awaitItem()
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }
    }

    @Test
    fun `short-circuits when first checker returns CAN_WRITE`() =
      runTest(dispatcherRule.dispatcher) {
        val c1 = mockk<FileSystemChecker>()
        val c2 = mockk<FileSystemChecker>()

        every { c1.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

        val checker = createChecker(pathWithSpace, listOf(c1, c2))

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
        }

        verify(exactly = 0) { c2.checkFilesystemSupports4GbFiles(any()) }
      }

    @Test
    fun `returns CannotWrite when all checkers inconclusive`() =
      runTest(dispatcherRule.dispatcher) {
        val c1 = mockk<FileSystemChecker>()
        val c2 = mockk<FileSystemChecker>()

        every { c1.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
        every { c2.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE

        val checker = createChecker(pathWithSpace, listOf(c1, c2))

        checker.fileSystemStates.test {
          awaitItem()
          assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
        }
      }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class FlowTests : BaseTest() {
    @Test
    fun `re-evaluates when storage changes`() = runTest(dispatcherRule.dispatcher) {
      every { fileSystemChecker.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

      val checker = createChecker(pathWithSpace)

      checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)

        selectedStorage.emit(pathWithoutSpace)

        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
      }
    }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class InteractionTests : BaseTest() {
    @Test
    fun `invokes checker with correct path`() = runTest(dispatcherRule.dispatcher) {
      every { fileSystemChecker.checkFilesystemSupports4GbFiles(pathWithSpace) } returns CAN_WRITE_4GB

      val checker = createChecker(pathWithSpace)

      checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }

      verify(atLeast = 1) {
        fileSystemChecker.checkFilesystemSupports4GbFiles(pathWithSpace)
      }
    }
  }

  class UnitTests {
    @Test
    fun `constants are correct`() {
      assertThat(Fat32Checker.FOUR_GIGABYTES_IN_BYTES)
        .isEqualTo(4L * 1024 * 1024 * 1024)

      assertThat(Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES)
        .isEqualTo(4L * 1024 * 1024)
    }

    @Test
    fun `FileSystemState instances are unique`() {
      val states = listOf(
        NotEnoughSpaceFor4GbFile,
        CanWrite4GbFile,
        CannotWrite4GbFile,
        DetectingFileSystem
      )

      assertThat(states.distinct()).hasSize(4)
    }

    @Test
    fun `FileSystemState are singletons`() {
      assertThat(NotEnoughSpaceFor4GbFile).isSameAs(NotEnoughSpaceFor4GbFile)
      assertThat(CanWrite4GbFile).isSameAs(CanWrite4GbFile)
    }
  }

  @RunWith(RobolectricTestRunner::class)
  @Config(
    sdk = [Build.VERSION_CODES.R],
    manifest = Config.NONE,
    application = TestApplication::class
  )
  class FileObserverTests : BaseTest() {
    @Test
    fun `starts observing when storage has insufficient space`() =
      runTest(dispatcherRule.dispatcher) {
        val checker = createChecker(pathWithoutSpace)

        checker.fileSystemStates.test {
          assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
          assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun `stops observing when storage becomes valid`() = runTest(dispatcherRule.dispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns CAN_WRITE_4GB

      val checker = createChecker(pathWithoutSpace)

      checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)

        selectedStorage.emit(pathWithSpace)

        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }
    }
  }
}
