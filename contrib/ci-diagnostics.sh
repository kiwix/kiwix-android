#!/usr/bin/env bash

#
# Kiwix Android
# Copyright (c) 2026 Kiwix <android.kiwix.org>
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
#

# Shared by the instrumentation-*.sh scripts (kiwix/kiwix-android#5155): when
# the emulator itself hangs (e.g. "detected a hanging thread 'QEMU2 main
# loop'"), adb stops responding and the job eventually gets force-killed by
# android-emulator-runner's own watchdog. The GitHub Actions step log is the
# only copy of what happened in that case, so whatever hadn't been flushed to
# it yet is lost - which is why these hangs show up with no clue about their
# actual cause. Mirror the full logcat and periodic memory/process snapshots
# to local files instead: those survive an abrupt kill and get uploaded as a
# build artifact unconditionally (see the "Upload CI diagnostics" step in
# ci.yml), so the next hang leaves real evidence instead of a dead end.
CI_DIAGNOSTICS_DIR="ci-diagnostics"
mkdir -p "$CI_DIAGNOSTICS_DIR"

start_ci_diagnostics() {
  adb logcat -v threadtime >>"$CI_DIAGNOSTICS_DIR/logcat-full.log" 2>&1 &
  CI_DIAG_LOGCAT_PID=$!

  (
    while true; do
      {
        echo "=== $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="
        free -h
        echo "--- device process count ---"
        adb shell ps 2>/dev/null | wc -l
        echo "--- dumpsys meminfo (summary) ---"
        adb shell dumpsys meminfo 2>/dev/null | head -40
      } >>"$CI_DIAGNOSTICS_DIR/resource-snapshot.log" 2>&1
      sleep 15
    done
  ) &
  CI_DIAG_MONITOR_PID=$!

  start_adb_watchdog
}

# kiwix/kiwix-android#5155: when the emulator hangs hard enough (e.g. the
# "QEMU2 main loop" case), adb stops responding and, left alone, the *host*
# runner VM itself eventually becomes unresponsive too - at which point
# GitHub's backend loses contact with it and marks the job "cancelled" from
# the outside. Once that happens the runner process is gone, so nothing in
# the job can run anymore, not even "if: always()" steps - the diagnostics
# gathered above never get uploaded.
# So: watch adb ourselves, and if it stays unresponsive for a few minutes,
# proactively kill gradle so the step fails *on its own*, while the runner
# is still alive. That gives the "Upload CI diagnostics" step in ci.yml an
# actual chance to run.
start_adb_watchdog() {
  (
    failures=0
    while true; do
      sleep 30
      if timeout 10 adb shell true >/dev/null 2>&1; then
        failures=0
        continue
      fi
      failures=$(( failures + 1 ))
      echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) adb unresponsive ($failures/6)" >>"$CI_DIAGNOSTICS_DIR/watchdog.log"
      if [ "$failures" -ge 6 ]; then
        echo "$(date -u +%Y-%m-%dT%H:%M:%SZ) adb unresponsive for 3+ minutes, killing gradle so the step fails cleanly" >>"$CI_DIAGNOSTICS_DIR/watchdog.log"
        timeout 10 adb exec-out screencap -p >"$CI_DIAGNOSTICS_DIR/screencap-watchdog.png" 2>/dev/null
        pkill -9 -f gradle 2>/dev/null || true
        exit 0
      fi
    done
  ) &
  CI_DIAG_WATCHDOG_PID=$!
}

stop_ci_diagnostics() {
  kill "$CI_DIAG_LOGCAT_PID" "$CI_DIAG_MONITOR_PID" "$CI_DIAG_WATCHDOG_PID" 2>/dev/null || true
}
