/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.error;

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
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.splash.SplashActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk;

import static org.kiwix.kiwixmobile.utils.LanguageUtils.getCurrentLocale;

public class ErrorActivity extends BaseActivity {

  @Inject
  NewBookDao bookDao;

  @BindView(R.id.reportButton)
  Button reportButton;

  @BindView(R.id.restartButton)
  Button restartButton;

  @BindView(R.id.allowLanguage)
  CheckBox allowLanguageCheckbox;

  @BindView(R.id.allowZims)
  CheckBox allowZimsCheckbox;

  @BindView(R.id.allowCrash)
  CheckBox allowCrashCheckbox;

  @BindView(R.id.allowLogs)
  CheckBox allowLogsCheckbox;

  @BindView(R.id.allowDeviceDetails)
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
        if(logFile.exists()) {
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

      if(allowZimsCheckbox.isChecked()) {
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

        String currentZimFile = ZimContentProvider.getZimFile();
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
