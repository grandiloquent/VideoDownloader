package euphoria.psycho.share;


public class MathShare {

    private static final double EARTH_RADIUS_METERS = 6367000.0;

    public static double accurateDistanceMeters(double lat1, double lng1,
                                                double lat2, double lng2) {
        double dlat = Math.sin(0.5 * (lat2 - lat1));
        double dlng = Math.sin(0.5 * (lng2 - lng1));
        double x = dlat * dlat + dlng * dlng * Math.cos(lat1) * Math.cos(lat2);
        return (2 * Math.atan2(Math.sqrt(x), Math.sqrt(Math.max(0.0,
                1.0 - x)))) * EARTH_RADIUS_METERS;
    }

    public static int ceilLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) >= value) break;
        }
        return i;
    }

    // Returns the input value x clamped to the range [min, max].
    public static float clamp(float x, float min, float max) {
        if (x > max) return max;
        return Math.max(x, min);
    }

    // Returns the input value x clamped to the range [min, max].
    public static long clamp(long x, long min, long max) {
        if (x > max) return max;
        return Math.max(x, min);
    }

    // Returns the input value x clamped to the range [min, max].
    public static int clamp(int x, int min, int max) {
        if (x > max) return max;
        return Math.max(x, min);
    }

    public static int floorLog2(float value) {
        int i;
        for (i = 0; i < 31; i++) {
            if ((1 << i) > value) break;
        }
        return i - 1;
    }

    public static float interpolateAngle(
            float source, float target, float progress) {
        // interpolate the angle from source to target
        // We make the difference in the range of [-179, 180], this is the
        // shortest path to change source to target.
        float diff = target - source;
        if (diff < 0) diff += 360f;
        if (diff > 180) diff -= 360f;
        float result = source + diff * progress;
        return result < 0 ? result + 360f : result;
    }

    public static float interpolateScale(
            float source, float target, float progress) {
        return source + progress * (target - source);
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    // Returns the previous power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0
    public static int prevPowerOf2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    public static double toMile(double meter) {
        return meter / 1609;
    }
}
