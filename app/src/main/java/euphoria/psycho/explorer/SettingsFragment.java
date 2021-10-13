package euphoria.psycho.explorer;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

public class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {

    public static final String KEY_TENCENT = "key_tencent";

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
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
