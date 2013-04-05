package org.kiwix.kiwixmobile;
public class JNIKiwix {
    public native String getMainPage();
    public native boolean loadZIM(String path);
    public native byte[] getContent(String url, JNIKiwixString mimeType, JNIKiwixInt size);
    public native boolean searchSuggestions(String prefix, int count);
    public native boolean getNextSuggestion(JNIKiwixString title);
    public native boolean getPageUrlFromTitle(String title, JNIKiwixString url);

    static {
        System.loadLibrary("kiwix");
    }
}

class JNIKiwixString {
    String value;
}

class JNIKiwixInt {
    int value;
}

class JNIKiwixBool {
    boolean value;
}
