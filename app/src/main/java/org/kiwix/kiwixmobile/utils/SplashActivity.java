/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.kiwix.kiwixmobile.KiwixErrorActivity;
import org.kiwix.kiwixmobile.main.MainActivity;

import java.io.Serializable;


public class SplashActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Context appContext = this;
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread paramThread, Throwable paramThrowable) {

        final Intent intent = new Intent(appContext, KiwixErrorActivity.class);

        Bundle extras = new Bundle();
        extras.putSerializable("exception", (Serializable) paramThrowable);

        intent.putExtras(extras);

        appContext.startActivity(intent);

        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
      }
    });

    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
    finish();
  }
}