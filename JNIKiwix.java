public class JNIKiwix {    
    public native Boolean nativeLoadZIM();    
    public native String nativeGetContent(String url);

    static {
        System.loadLibrary("kiwix");
    }        
    
    public static void main(String[] args) {
	return;
    }
}