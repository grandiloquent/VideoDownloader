package euphoria.psycho.explorer;

public class NativeShare {
    static {
        System.loadLibrary("native-lib");
    }


    public static native boolean getString(byte[] data, int length, byte[] buffer);
    public static native int get91Porn(byte[] data, int length, byte[] buffer);

}
