package euphoria.psycho.share;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

public class StringShare {

    //
    public static boolean isNullOrEmpty(String exifMake) {
        return TextUtils.isEmpty(exifMake);
    }

    public static float parseFloatSafely(String content, float defaultValue) {
        if (content == null) return defaultValue;
        try {
            return Float.parseFloat(content);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseIntSafely(String content, int defaultValue) {
        if (content == null) return defaultValue;
        try {
            return Integer.parseInt(content);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String substring(String string, String first, String second) {
        int start = string.indexOf(first);
        if (start == -1) return null;
        start += first.length();
        int end = string.indexOf(second, start);
        if (end == -1) return null;
        return string.substring(start, end);
    }

    public static String substring(String string, String first, String second, boolean afterLast) {
        int start = afterLast ? string.lastIndexOf(first) : string.indexOf(first);
        if (start == -1) return null;
        start += first.length();
        int end = string.indexOf(second, start);
        if (end == -1) return null;
        return string.substring(start, end);
    }

    public static String substringAfter(String s, String delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return s;
        return s.substring(index + delimiter.length());
    }

    public static String substringAfter(String s, char delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return s;
        return s.substring(index + 1);
    }

    public static String substringAfterLast(String s, String delimiter) {
        int index = s.lastIndexOf(delimiter);
        if (index == -1) return s;
        return s.substring(index + delimiter.length());
    }

    public static String substringAfterLast(String s, char delimiter) {
        int index = s.lastIndexOf(delimiter);
        if (index == -1) return s;
        return s.substring(index + 1);
    }

    public static String substringBefore(String s, String delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return s;
        return s.substring(0, index);
    }

    public static String substringBefore(String s, char delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return s;
        return s.substring(0, index);
    }

    public static String substringBeforeLast(String s, String delimiter) {
        int index = s.lastIndexOf(delimiter);
        if (index == -1) return s;
        return s.substring(0, index);
    }

    public static String substringBeforeLast(String s, char delimiter) {
        int index = s.lastIndexOf(delimiter);
        if (index == -1) return s;
        return s.substring(0, index);
    }

    public static List<String> substringCodes(String string, String pattern, int max) {
        int index = string.indexOf(pattern);
        if (index == -1) return null;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            int start = string.lastIndexOf("\"", index);
            if (start == -1) break;
            int end = string.indexOf("\"", index + pattern.length());
            if (end == -1) break;
            list.add(string.substring(start + 1, end));
            index = string.indexOf(pattern, end + 1);
            if (index == -1) break;
        }
        return list;
    }

    public static String substringLine(String s, String delimiter) {
        int index = s.indexOf(delimiter);
        if (index == -1) return null;
        int end = index + delimiter.length();
        while (index - 1 > -1) {
            if (s.charAt(index - 1) == '\n') break;
            index--;
        }
        while (end + 1 < s.length()) {
            if (s.charAt(end + 1) == '\n')
                break;
            end++;
        }
        return s.substring(index, end);
    }

}
