/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.error;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import butterknife.BindView;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.dao.NewBookDao;
import org.kiwix.kiwixmobile.core.di.components.CoreComponent;
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.utils.files.FileLogger;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk;
import org.kiwix.kiwixmobile.zim_manager.MountInfo;
import org.kiwix.kiwixmobile.zim_manager.MountPointProducer;

import static androidx.core.content.FileProvider.getUriForFile;
import static org.kiwix.kiwixmobile.core.utils.LanguageUtils.getCurrentLocale;

public class ErrorActivity extends BaseActivity {

  public static final String EXCEPTION_KEY = "exception";

  @Inject
  NewBookDao bookDao;
  @Inject
  ZimReaderContainer zimReaderContainer;
  @Inject
  MountPointProducer mountPointProducer;
  @Inject
  FileLogger fileLogger;

  @BindView(R2.id.reportButton)
  Button reportButton;

  @BindView(R2.id.restartButton)
  Button restartButton;

  @BindView(R2.id.allowLanguage)
  CheckBox allowLanguageCheckbox;

  @BindView(R2.id.allowZims)
  CheckBox allowZimsCheckbox;

  @BindView(R2.id.allowCrash)
  CheckBox allowCrashCheckbox;

  @BindView(R2.id.allowLogs)
  CheckBox allowLogsCheckbox;

  @BindView(R2.id.allowDeviceDetails)
  CheckBox allowDeviceDetailsCheckbox;

  @BindView(R2.id.allowFileSystemDetails)
  CheckBox allowFileSystemDetailsCheckbox;

  private static void killCurrentProcess() {
    android.os.Process.killProcess(android.os.Process.myPid());
    System.exit(10);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_kiwix_error);
    Intent callingIntent = getIntent();

    Bundle extras = callingIntent.getExtras();
    final Throwable exception;
    if (extras != null && safeContains(extras, EXCEPTION_KEY)) {
      exception = (Throwable) extras.getSerializable(EXCEPTION_KEY);
    } else {
      exception = null;
    }

    reportButton.setOnClickListener(v -> {

      Intent emailIntent = new Intent(Intent.ACTION_SEND);
      emailIntent.setType("vnd.android.cursor.dir/email");
      emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { "android-crash-feedback@kiwix.org" });
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, getSubject());

      String body = getBody();


      if (allowLogsCheckbox.isChecked()) {
        File file = fileLogger.writeLogFile(this);
        Log.d("SEARCHING.....", "onCreate: Searching @ "+ getPackageName()+ " .fileProvider"+ file);
        Uri path = getUriForFile(this,
          getPackageName() + ".fileProvider", file);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.putExtra(Intent.EXTRA_STREAM, path);
      }

      if (allowCrashCheckbox.isChecked() && exception != null) {
        body += "Exception Details:\n\n" +
          toStackTraceString(exception) +
          "\n\n";
      }

      if (allowZimsCheckbox.isChecked()) {
        List<BookOnDisk> books = bookDao.getBooks();

        StringBuilder sb = new StringBuilder();
        for (BookOnDisk bookOnDisk : books) {
          final LibraryNetworkEntity.Book book = bookOnDisk.getBook();
          String bookString = book.getTitle() +
            ":\nArticles: [" + book.getArticleCount() +
            "]\nCreator: [" + book.getCreator() +
            "]\n";

          sb.append(bookString);
        }
        String allZimFiles = sb.toString();

        String currentZimFile = zimReaderContainer.getZimCanonicalPath();
        body += "Curent Zim File:\n" +
          currentZimFile +
          "\n\nAll Zim Files in DB:\n" +
          allZimFiles +
          "\n\n";
      }

      if (allowLanguageCheckbox.isChecked()) {
        body += "Current Locale:\n" +
          getCurrentLocale(getApplicationContext()) +
          "\n\n";
      }

      if (allowDeviceDetailsCheckbox.isChecked()) {
        body += "Device Details:\n" +
          "Device:[" + Build.DEVICE
          + "]\nModel:[" + Build.MODEL
          + "]\nManufacturer:[" + Build.MANUFACTURER
          + "]\nTime:[" + Build.TIME
          + "]\nAndroid Version:[" + Build.VERSION.RELEASE
          + "]\nApp Version:[" + getVersionName() + " " + getVersionCode()
          + "]" +
          "\n\n";
      }

      if (allowFileSystemDetailsCheckbox.isChecked()) {
        body += "Mount Points\n";
        for (MountInfo mountInfo : mountPointProducer.produce()) {
          body += mountInfo + "\n";
        }

        body += "\nExternal Directories\n";
        for (File externalFilesDir : ContextCompat.getExternalFilesDirs(this, null)) {
          body += (externalFilesDir != null ? externalFilesDir.getPath() : "null") + "\n";
        }
      }

      emailIntent.putExtra(Intent.EXTRA_TEXT, body);

      startActivityForResult(Intent.createChooser(emailIntent, "Send email..."), 1);
    });

    restartButton.setOnClickListener(v -> onRestartClicked());
  }

  private boolean safeContains(Bundle extras, String key) {
    try {
      return extras.containsKey(key);
    } catch (RuntimeException ignore) {
      return false;
    }
  }

  private void onRestartClicked() {
    restartApp();
  }

  @NotNull protected String getSubject() {
    return "Someone has reported a crash";
  }

  @NotNull protected String getBody() {
    return "Hi Kiwix Developers!\n" +
      "The Android app crashed, here are some details to help fix it:\n\n";
  }

  private int getVersionCode() {
    try {
      return getPackageManager()
        .getPackageInfo(getPackageName(), 0).versionCode;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String getVersionName() {
    try {
      return getPackageManager()
        .getPackageInfo(getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String toStackTraceString(Throwable exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  void restartApp() {
    startActivity(getPackageManager().getLaunchIntentForPackage(getPackageName()));
    finish();
    killCurrentProcess();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    restartApp();
  }

  @Override protected void injection(CoreComponent coreComponent) {
    coreComponent.inject(this);
  }
}
