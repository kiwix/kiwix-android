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
}

stop_ci_diagnostics() {
  kill "$CI_DIAG_LOGCAT_PID" "$CI_DIAG_MONITOR_PID" 2>/dev/null || true
}
