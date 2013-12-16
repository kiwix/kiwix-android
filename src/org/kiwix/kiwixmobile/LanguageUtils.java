package org.kiwix.kiwixmobile;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LanguageUtils {

    private List<LanguageContainer> mLanguageList;

    private List<String> mLocaleLanguageCodes;

    private Context mContext;

    public LanguageUtils(Context context) {

        mContext = context;

        mLanguageList = new ArrayList<LanguageContainer>();

        mLocaleLanguageCodes = new ArrayList<String>();

        getLanguageCodesFromAssets();

        setupLanguageList();

        sortLanguageList();
    }

    // Read the language codes, that are supported in this app from the locales.txt file
    private void getLanguageCodesFromAssets() {

        List<String> locales = new ArrayList<String>(new FileWriter(mContext).readFileFromAssets());

        for (String locale : locales) {

            if (!locale.isEmpty()) {
                mLocaleLanguageCodes.add(locale.trim());
            }
        }
    }

    // Create a list containing the language code and the corresponding (english) langauge name
    private void setupLanguageList() {

        for (String languageCode : mLocaleLanguageCodes) {
            mLanguageList.add(new LanguageContainer(languageCode));
        }

    }

    // Sort the language list by the language name
    private void sortLanguageList() {

        Collections.sort(mLanguageList, new Comparator<LanguageContainer>() {
            @Override
            public int compare(LanguageContainer a, LanguageContainer b) {
                return a.getLanguageName().compareToIgnoreCase(b.getLanguageName());
            }
        });
    }

    // Check, if the selected Locale is supported and weather we actually need to change our font.
    // We do this by checking, if our Locale is available in the List, that Locale.getAvailableLocales() returns.
    private boolean haveToChangeFont() {

        for (Locale s : Locale.getAvailableLocales()) {
            if (s.getLanguage().equals(Locale.getDefault().toString())) {

                return false;
            }
        }
        return true;
    }

    // Change the font of all the TextViews and its subclasses in our whole app by attaching a custom
    // Factory to the LayoutInflater of the Activity.
    // The Factory is called on each element name as the xml is parsed and we can therefore modify
    // the parsed Elements.
    // A Factory can only be set once on a LayoutInflater. And since we are using the support Library,
    // which also sets a Factory on the LayoutInflator, we have to access the private field of the
    // LayoutInflater, that handles this restriction via Java's reflection API
    // and make it accessible set it to false again.
    public void changeFont(LayoutInflater layoutInflater) {

        if (!haveToChangeFont()) {
            return;
        }

        try {
            Field field = LayoutInflater.class.getDeclaredField("mFactorySet");
            field.setAccessible(true);
            field.setBoolean(layoutInflater, false);
            layoutInflater.setFactory(new LayoutInflaterFactory(mContext, layoutInflater));

        } catch (NoSuchFieldException e) {

        } catch (IllegalArgumentException e) {

        } catch (IllegalAccessException e) {

        }
    }

    // Get a list of all the language names
    public List<String> getValues() {

        List<String> values = new ArrayList<String>();

        for (LanguageContainer value : mLanguageList) {
            values.add(value.getLanguageName());
        }

        return values;
    }

    // Get a list of all the language codes
    public List<String> getKeys() {

        List<String> keys = new ArrayList<String>();

        for (LanguageContainer key : mLanguageList) {
            keys.add(key.getLanguageCode());
        }

        return keys;
    }

    // That's the Factory, that will handle the manipulation of all our TextView's and its subcalsses
    // while the content is being parsed
    public static class LayoutInflaterFactory implements LayoutInflater.Factory {

        private Context mContext;

        private LayoutInflater mLayoutInflater;

        public LayoutInflaterFactory(Context context, LayoutInflater layoutInflater) {
            mContext = context;
            mLayoutInflater = layoutInflater;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            Log.e("kiwix", name);

            // Apply the custom font, if the xml tag equals "TextView", "EditText" or "AutoCompleteTextView"
            if (name.equalsIgnoreCase("TextView")
                    || name.equalsIgnoreCase("EditText")
                    || name.equalsIgnoreCase("AutoCompleteTextView")) {

                try {
                    LayoutInflater inflater = mLayoutInflater;
                    final View view = inflater.createView(name, null, attrs);
                    new Handler().post(new Runnable() {
                        public void run() {
                            ((TextView) view).setTypeface(Typeface.createFromAsset(
                                    mContext.getAssets(), "DejaVuSansCondensed.ttf"));
                        }
                    });

                    return view;

                } catch (InflateException e) {

                } catch (ClassNotFoundException e) {

                }
            }
            return null;
        }
    }

    private class LanguageContainer {

        private String mLanguageCode;

        private String mLanguageName;

        // This constructor will take care of creating a language name for the given ISO 639-1 language code.
        // The language name will always be in english to ensure user friendliness and to prevent
        // possible incompatibilities, since not all language names are available in all languages.
        private LanguageContainer(String languageCode) {
            mLanguageCode = languageCode;
            mLanguageName = new Locale(languageCode).getDisplayLanguage(new Locale("en"));
        }

        public String getLanguageCode() {
            return mLanguageCode;
        }

        public String getLanguageName() {
            return mLanguageName;
        }
    }
}
