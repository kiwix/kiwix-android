package org.kiwix.kiwixmobile.utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.kiwix.kiwixmobile.KiwixMobileActivity;

import ly.count.android.sdk.Countly;
import ly.count.android.sdk.DeviceId;


public class SplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Countly.sharedInstance().init(this, "http://159.65.37.191", "b312eaa86db23ddf49d14b641bc3a97623194b03", null, DeviceId.Type.OPEN_UDID);

    Countly.sharedInstance().initMessaging(this, SplashActivity.class, "9116215767541857492", Countly.CountlyMessagingMode.TEST);
    Countly.sharedInstance().setViewTracking(true);
    Countly.sharedInstance().enableCrashReporting();
    Countly.sharedInstance().setLoggingEnabled(true);

    Intent intent = new Intent(this, KiwixMobileActivity.class);
    startActivity(intent);
    finish();
  }
}