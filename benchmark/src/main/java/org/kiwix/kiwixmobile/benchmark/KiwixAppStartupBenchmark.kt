package org.kiwix.kiwixmobile.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiwixAppStartupBenchmark {
  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  // Cold start with no compilation: App process is not in memory.
  @Test
  fun startupColdNoCompilation() = benchmarkRule.measureRepeated(
    packageName = "org.kiwix.kiwixmobile",
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.COLD,
    compilationMode = CompilationMode.None()
  ) {
    pressHome()
    killProcess()
    startActivityAndWait()
  }

  // Cold start: App process is not in memory.
  @Test
  fun startupCold() = benchmarkRule.measureRepeated(
    packageName = "org.kiwix.kiwixmobile",
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.COLD,
    compilationMode = CompilationMode.Partial()
  ) {
    pressHome()
    killProcess()
    startActivityAndWait()
  }

  // Warm start: App process exists but activity is recreated.
  @Test
  fun startupWarm() = benchmarkRule.measureRepeated(
    packageName = "org.kiwix.kiwixmobile",
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.WARM,
    compilationMode = CompilationMode.Partial()
  ) {
    pressHome()
    startActivityAndWait()
  }

  // Hot start: App process exists just activity needs to be brought from background to foreground.
  @Test
  fun startupHot() = benchmarkRule.measureRepeated(
    packageName = "org.kiwix.kiwixmobile",
    metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.HOT,
    compilationMode = CompilationMode.Partial()
  ) {
    startActivityAndWait()
  }
}
