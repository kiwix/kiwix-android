/*
 * Copyright 2013
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

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
