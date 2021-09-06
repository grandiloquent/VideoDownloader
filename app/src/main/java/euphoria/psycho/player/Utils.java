package euphoria.psycho.player;

import java.io.File;

public class Utils {
    // mBx mmBx
    public static boolean isNullOrWhiteSpace(CharSequence charSequence) {
        if (charSequence == null) return true;
        int length = charSequence.length();
        for (int i = 0; i < length; i++) {
            if (!Character.isWhitespace(charSequence.charAt(i))) return false;
        }
        return true;
    }

    public static String getFileName(String path) {
        if (path != null) {
            int length = path.length();
            for (int i = length; --i >= 0; ) {
                char ch = path.charAt(i);
                if (ch == File.separatorChar)
                    return path.substring(i + 1);
            }
        }
        return path;
    }

    public static String changeExtension(String path, String extension) {
        if (path != null) {
            String s = path;
            int length = path.length();
            for (int i = length; --i >= 0; ) {
                char ch = path.charAt(i);
                if (ch == '.') {
                    s = path.substring(0, i);
                    break;
                }
                if (ch == File.separatorChar)
                    break;
            }
            if (extension != null && path.length() != 0) {
                if (extension.length() == 0 || extension.charAt(0) != '.') {
                    s = s + ".";
                }
                s = s + extension;
            }
            return s;
        }
        return null;
    }
}