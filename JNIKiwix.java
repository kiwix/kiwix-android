public class JNIKiwix {    
    public native boolean nativeLoadZIM(String path);    
    public native String nativeGetContent(String url);

    static {
        System.loadLibrary("kiwix");
    }
    
    public static void main(String[] args) {
	JNIKiwix self = new JNIKiwix();

	try {
	    self.nativeLoadZIM("test.zim");
	}
	catch (Exception e) {
	    System.out.println("Message " + e.getMessage());
	    e.printStackTrace();
	}

	return;
    }
}