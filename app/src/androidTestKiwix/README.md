This is the readme for testing Kiwix Android. As you can see there are already some tests in this directory and others.
The Directory structure works as follows:

* test - unit tests
* androidTest - instrumentation tests
* androidTestKiwix - instrumentation tests only for the Kiwix flavour (no custom apps)

Instrumentation tests mainly utilize [Espresso](https://developer.android.com/training/testing/ui-testing/espresso-testing.html)
although [UI Automator](https://developer.android.com/training/testing/ui-testing/uiautomator-testing.html) is used for some
workarounds such as accepting permissions where out of app interaction is required.

Creating a new test is simple.

1. I advice using the test recorder in Android Studio to get the basic flow of your test down.
2. Note down any interactions which were not recordered.
3. Generate the test using the tool and give it an appropriate name.
4. Reimplement in code the interactions that were not recordered.
5. Listview interactions should be handled delicately view examples in DownloadTest.java
6. Idling resources e.g network and file system should be identified and hooked up to the test suite. There is a static method to call
binding your resource at the start and unbinding at the end. Espresso will pause itself while the resource is active. Call 
TestingUtils.bindResource(MyClass.class); and TestingUtils.unbindResource(MyClass.class);
7. Other issues may arise so test.

Once generated tests with the correct annotations (created by creation tool) should be run automatically.

Once Travis builds an APK it pushes it to [testdroid](https://cloud.testdroid.com/) where it is run on a variety of devices.
The result is passed back to Travis and the build either passes or fails.

There is a possiblity of unrelated issues causing a test to fail e.g network failure, broken device. A rebuild in travis can be
triggered to try again and individual devices can be retried on testdroid.
