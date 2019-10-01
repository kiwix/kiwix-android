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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.core.content.FileProvider;
import butterknife.BindView;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.core.newdb.dao.NewBookDao;
import org.kiwix.kiwixmobile.core.splash.SplashActivity;
import org.kiwix.kiwixmobile.core.zim_manager.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk;

import static org.kiwix.kiwixmobile.core.utils.LanguageUtils.getCurrentLocale;

public class ErrorActivity extends BaseActivity {

  @Inject
  NewBookDao bookDao;
  @Inject
  ZimReaderContainer zimReaderContainer;

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
    Throwable exception = (Throwable) extras.getSerializable("exception");

    reportButton.setOnClickListener(v -> {

      Intent emailIntent = new Intent(Intent.ACTION_SEND);
      emailIntent.setType("vnd.android.cursor.dir/email");
      String[] to = { "android-crash-feedback@kiwix.org" };
      emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Someone has reported a crash");

      String body = "Hi Kiwix Developers!\n" +
        "The Android app crashed, here are some details to help fix it:\n\n";

      if (allowLogsCheckbox.isChecked()) {
        File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Kiwix");
        File logFile = new File(appDirectory, "logcat.txt");
        if (logFile.exists()) {
          Uri path =
            FileProvider.getUriForFile(this,
              getApplicationContext().getPackageName() + ".fileprovider",
              logFile);
          emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        }
      }

      if (allowCrashCheckbox.isChecked()) {
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
          + "]" +
          "\n\n";
      }

      emailIntent.putExtra(Intent.EXTRA_TEXT, body);

      startActivityForResult(Intent.createChooser(emailIntent, "Send email..."), 1);
    });

    restartButton.setOnClickListener(v -> restartApp());
  }

  private String toStackTraceString(Throwable exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }

  void restartApp() {
    Context context = this;
    Intent intent = new Intent(context, SplashActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
      | Intent.FLAG_ACTIVITY_CLEAR_TASK
      | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    intent.setAction(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    context.startActivity(intent);

    finish();
    killCurrentProcess();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    restartApp();
  }
}
