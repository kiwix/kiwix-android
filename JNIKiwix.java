public class JNIKiwix {    
    public native boolean nativeLoadZIM(String path);    
    public native byte[] nativeGetContent(String url, JNIKiwixString mimeType, JNIKiwixInt size);

    static {
        System.loadLibrary("kiwix");
    }
    
    public static void main(String[] args) {
	JNIKiwix self = new JNIKiwix();

	try {
	    self.nativeLoadZIM("test.zim");
	    
	    JNIKiwixString mimeTypeObj = new JNIKiwixString();
	    JNIKiwixInt sizeObj = new JNIKiwixInt();
	    self.nativeGetContent("/A/Wikipedia.html", mimeTypeObj, sizeObj);
	    System.out.println(mimeTypeObj.value);
	}
	catch (Exception e) {
	    System.out.println("Message " + e.getMessage());
	    e.printStackTrace();
	}

	return;
    }
}

class JNIKiwixString {
    String value;
}

class JNIKiwixInt {
    int value;
}