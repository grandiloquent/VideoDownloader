package euphoria.psycho.explorer;

public class Native {

    static {
        System.loadLibrary("hello-libs");
    }

    public native static String fetch91Porn(String url);
}
