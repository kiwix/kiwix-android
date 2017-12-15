package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.SplashActivity;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.kiwix.kiwixmobile.utils.LanguageUtils.getCurrentLocale;

public class KiwixErrorActivity extends AppCompatActivity {

    @BindView(R.id.messageText)
    TextView messageText;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kiwix_error);
        ButterKnife.bind(this);

        Context context = this;

        Intent callingIntent = getIntent();

        Bundle extras = callingIntent.getExtras();
        Throwable exception = (Throwable) extras.getSerializable("exception");

        reportButton.setOnClickListener(v -> {


            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("vnd.android.cursor.dir/email");
            String to[] = {"joseph.reeve@googlemail.com"};
            emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Someone has reported a crash");

            String body = "Hi Kiwix Developers!\n" +
                    "The Android app crashed, here are some details to help fix it:\n\n";

            if(allowLogsCheckbox.isChecked()) {
                File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Kiwix");
                File logFile = new File(appDirectory, "logcat.txt");
                Uri path = Uri.fromFile(logFile);
                emailIntent.putExtra(Intent.EXTRA_STREAM, path);
            }

            if(allowCrashCheckbox.isChecked()) {
                body += "Exception Details:\n\n" +
                        exception.toString() +
                "\n\n";
            }

            if(allowZimsCheckbox.isChecked()) {
                BookDao bookDao = new BookDao(KiwixDatabase.getInstance(getApplicationContext()));
                ArrayList<LibraryNetworkEntity.Book> books = bookDao.getBooks();

                String allZimFiles = "";
                for(LibraryNetworkEntity.Book book: books) {
                    String bookString = book.getTitle() +
                            ":\nArticles: ["+ book.getArticleCount() +
                            "]\nCreator: [" + book.getCreator() +
                            "]\n";

                    allZimFiles += bookString;
                }

                String currentZimFile = ZimContentProvider.getZimFile();
                body += "Curent Zim File:\n" +
                        currentZimFile +
                        "\n\nAll Zim Files in DB:\n" +
                        allZimFiles +
                "\n\n";
            }

            if(allowLanguageCheckbox.isChecked()) {
                body += "Current Locale:\n" +
                        getCurrentLocale(getApplicationContext()) +
                "\n\n";
            }

            if(allowDeviceDetailsCheckbox.isChecked()) {
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

            startActivityForResult(Intent.createChooser(emailIntent , "Send email..."), 1);
        });

        restartButton.setOnClickListener(v -> restartApp());
    }

    void restartApp(){
        Context context = KiwixErrorActivity.this;
        Intent intent = new Intent(context, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(intent);

        finish();
        killCurrentProcess();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        restartApp();
    }

    private static void killCurrentProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
