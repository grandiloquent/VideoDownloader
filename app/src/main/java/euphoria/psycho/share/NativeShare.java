package euphoria.psycho.share;

public class NativeShare {
    static {
        System.loadLibrary("native-lib");
    }


    public static native boolean getString(byte[] data, int length, byte[] buffer);
}
