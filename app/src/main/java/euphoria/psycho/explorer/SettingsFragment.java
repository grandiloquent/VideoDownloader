package euphoria.psycho.explorer;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Process;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import euphoria.psycho.videos.Porn91;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    public static final String KEY_TENCENT = "key_tencent";
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("videos_max")) {
            ProgressDialog dialog = new ProgressDialog(getContext());
            dialog.setMessage("更新视频列表...");
            dialog.show();
            new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                Porn91.fetchVideos(Integer.parseInt(sharedPreferences.getString("videos_max", "1")));
                getActivity().runOnUiThread(() -> dialog.dismiss());
            }).start();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        EditTextPreference tencent = (EditTextPreference) findPreference(KEY_TENCENT);
        tencent.setText(defaultPreferences.getString(KEY_TENCENT, null));
    }

}
