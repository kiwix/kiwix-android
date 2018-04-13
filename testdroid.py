#!/usr/bin/env python
import requests
import time
import sys
import os
PROJECT_ID = 143848482

if os.environ.get('BITBAR_API_KEY') == None:
  print("Tests could not run on insecure fork")
  sys.exit(0)
else:
  print("Running tests")
  sys.stdout.flush()

runID = 10
# os.environ['TRAVIS_BUILD_NUMBER']
apiKey = os.environ['BITBAR_API_KEY']
testName = "Auto Test {}".format(runID)

for x in range(0, 50):
  r = requests.get('https://cloud.testdroid.com/api/me/projects/{}/runs'.format(PROJECT_ID), auth=(apiKey, ''), headers={"Accept" : "application/json"})
  print(r.json())
  result = list(filter(lambda run: run.get("displayName") == testName, r.json().get("data")))
  if len(result) > 0 and result[0].get("state") == "FINISHED":
    ratio = result[0].get("successRatio")
    if ratio == 1.0:
      print("All tests passed")
      sys.exit(0)
    else:
      raise AssertionError("Success ratio only: {}".format(ratio))
  else:
    print("Waiting for results")
    sys.stdout.flush()
    time.sleep(30)
raise TimeoutError("Test not found/complete")
