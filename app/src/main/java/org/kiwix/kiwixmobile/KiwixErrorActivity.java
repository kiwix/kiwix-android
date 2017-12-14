package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.kiwix.kiwixmobile.utils.SplashActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.services.common.Crash;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kiwix_error);
        ButterKnife.bind(this);

        Context that = this;

        Intent callingIntent = getIntent();

        Bundle extras = callingIntent.getExtras();
        Throwable exception = (Throwable) extras.getSerializable("exception");

        reportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Fabric.with(that, new Crashlytics());
                Crashlytics.logException(exception);
                restartApp();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApp();
            }
        });
    }

    void restartApp(){
        Intent intent = new Intent(getApplicationContext(), KiwixMobileActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (intent.getComponent() != null) {
            //If the class name has been set, we force it to simulate a Launcher launch.
            //If we don't do this, if you restart from the error activity, then press home,
            //and then launch the activity from the launcher, the main activity appears twice on the backstack.
            //This will most likely not have any detrimental effect because if you set the Intent component,
            //if will always be launched regardless of the actions specified here.
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        this.finish();
        this.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
