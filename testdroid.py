#!/usr/bin/env python
import os
import requests
import sys
import time

PROJECT_ID = 116910522

print "Running tests"
sys.stdout.flush()

runID = os.environ['TRAVIS_BUILD_NUMBER']
resultsUrl = os.environ['TESTDROID_RUNNER_RESULTS']
testName = "Auto Test {}".format(runID)

for x in range(0, 200):
  r = requests.get(resultsUrl)
  result = list(filter(lambda run: run.get("displayName") == testName, r.json().get("data")))
  if len(result) > 0 and result[0].get("state") == "FINISHED":
    ratio = result[0].get("successRatio")
    if ratio == 1.0:
      print "All tests passed"
      sys.exit(0)
    else:
      raise AssertionError("Success ratio only: {}".format(ratio))
  else:
    print "Waiting for results"
    sys.stdout.flush()
    time.sleep(30)
raise TimeoutError("Test not found/complete")
