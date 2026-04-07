package org.kiwix.sharedFunctions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  private val dispatcher: TestDispatcher = StandardTestDispatcher()
) :
  TestWatcher(), BeforeEachCallback, AfterEachCallback {
  override fun starting(description: Description?) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description?) {
    Dispatchers.resetMain()
  }

  override fun beforeEach(context: ExtensionContext?) {
    Dispatchers.setMain(dispatcher)
  }

  override fun afterEach(context: ExtensionContext?) {
    Dispatchers.resetMain()
  }
}
