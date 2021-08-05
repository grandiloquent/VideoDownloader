package euphoria.psycho.share;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceShare {
    static SharedPreferences sPreferences;

    public static void initialize(Context context) {
        if (sPreferences == null)
            sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static SharedPreferences getPreferences() {
        return sPreferences;
    }

    public static SharedPreferences.Editor getEditor() {
        return sPreferences.edit();
    }

    public static void putString(String key, String value) {
        getEditor().putString(key, value).apply();
    }
}
