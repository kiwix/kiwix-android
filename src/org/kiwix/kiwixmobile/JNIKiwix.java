package org.kiwix.kiwixmobile;
public class JNIKiwix {
    public native String getMainPage();
    public native String getId();
    public native boolean loadZIM(String path);
    public native byte[] getContent(String url, JNIKiwixString mimeType, JNIKiwixInt size);
    public native boolean searchSuggestions(String prefix, int count);
    public native boolean getNextSuggestion(JNIKiwixString title);
    public native boolean getPageUrlFromTitle(String title, JNIKiwixString url);
    public native boolean getTitle(JNIKiwixString title);
    public native boolean getDescription(JNIKiwixString title);
    public native boolean getRandomPage(JNIKiwixString url);

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
