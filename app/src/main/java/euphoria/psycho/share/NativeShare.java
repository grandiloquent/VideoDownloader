package euphoria.psycho.share;

public class NativeShare {
    static {
        System.loadLibrary("native-lib");
    }

    public static native String getString(String uri);
}
