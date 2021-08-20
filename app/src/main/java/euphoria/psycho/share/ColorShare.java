package euphoria.psycho.share;


import android.graphics.Color;

public class ColorShare {
    public static boolean isOpaque(int color) {
        return color >>> 24 == 0xFF;
    }

    public static float[] intColorToFloatARGBArray(int from) {
        return new float[] {
                Color.alpha(from) / 255f,
                Color.red(from) / 255f,
                Color.green(from) / 255f,
                Color.blue(from) / 255f
        };
    }

}
