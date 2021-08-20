package euphoria.psycho.share;


public class ColorShare {
    public static boolean isOpaque(int color) {
        return color >>> 24 == 0xFF;
    }
}
