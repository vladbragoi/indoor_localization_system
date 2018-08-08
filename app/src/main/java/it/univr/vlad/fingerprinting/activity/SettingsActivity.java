package it.univr.vlad.fingerprinting.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceFragmentCompat;


// TODO
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public static class MyPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // addPreferencesFromResource(R.xml.my_preference_screen);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }
    }
}