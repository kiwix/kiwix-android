package org.kiwix.kiwixmobile.tests;

import android.content.Context;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.pressKey;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withInputType;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.os.Bundle;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.SearchActivity;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.utils.ActivityTestRuleHelper;

import static android.app.Instrumentation.ActivityResult;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasShortClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;
import android.widget.EditText;

import java.io.File;


/**
 * Created by user on 22/09/2017.
 */

public class SearchActivityTest {

    private String MESSAGE = "Test";

    @Rule
    public ActivityTestRule<SearchActivity> testRule = new ActivityTestRule<>(SearchActivity.class, false, false);


    @Test
    public void FirstTest() throws Exception {

        //TODO: Use a test ZIM
        ZimContentProvider.setZimFile("sdcard/Download/wikipedia_en_simple_all_03_2014.zim");
        final String zimFile = ZimContentProvider.getZimFile();
        Intent i = new Intent();
        i.putExtra("zimFile", zimFile);

        testRule.launchActivity(i);

        onView(isAssignableFrom(EditText.class))
                .perform(typeText(MESSAGE), closeSoftKeyboard());

        // Clicks first item
        onData(anything())
                .inAdapterView(allOf(withId(R.id.search_list), isCompletelyDisplayed()))
                .atPosition(0).perform(click());

        ActivityResult result = testRule.getActivityResult();
        Bundle resultDataExtras = result.getResultData().getExtras();

        assert resultDataExtras.getString(KiwixMobileActivity.TAG_FILE_SEARCHED) == MESSAGE;
    }

}
