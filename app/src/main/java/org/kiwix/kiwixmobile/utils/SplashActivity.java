package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;

import org.kiwix.kiwixmobile.KiwixErrorActivity;
import org.kiwix.kiwixmobile.KiwixMobileActivity;

import java.io.Serializable;


public class SplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Context appContext = this;
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
        //Crashlytics.logException(paramThrowable);

        final Intent intent = new Intent(appContext, KiwixErrorActivity.class);

        Bundle extras = new Bundle();
        extras.putSerializable("exception", (Serializable) paramThrowable);

        intent.putExtras(extras);

        appContext.startActivity(intent);

      }
    });

    Intent intent = new Intent(this, KiwixMobileActivity.class);
    startActivity(intent);
    finish();
  }
}