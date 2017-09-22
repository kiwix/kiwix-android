package org.kiwix.kiwixmobile.utils;

// https://jabknowsnothing.wordpress.com/2015/11/05/activitytestrule-espressos-test-lifecycle/

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;

public class ActivityTestRuleHelper<A extends AppCompatActivity> extends ActivityTestRule<A> {
    public ActivityTestRuleHelper(Class<A> activityClass) {
        super(activityClass);
    }

    @Override
    protected void beforeActivityLaunched() {
        super.beforeActivityLaunched();
        // Maybe prepare some mock service calls
        // Maybe override some depency injection modules with mocks
    }

    @Override
    protected Intent getActivityIntent() {
        Intent customIntent = new Intent();
        // add some custom extras and stuff
        return customIntent;
    }

    @Override
    protected void afterActivityLaunched() {
        super.afterActivityLaunched();
        // maybe you want to do something here
    }

    @Override
    protected void afterActivityFinished() {
        super.afterActivityFinished();
        // Clean up mocks
    }
}