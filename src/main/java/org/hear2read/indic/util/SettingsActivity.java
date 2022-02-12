package org.hear2read.indic.util;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.hear2read.indic.R;

/**
 * Settings activity containing settings for the English voice
 * Created by shyam on 7/6/18.
 */


public class SettingsActivity extends AppCompatActivity {
    private final static String LOG_TAG = "Flite_Java_" + SettingsActivity.class.getSimpleName();
    public final static String KEY_ENGLISH_ENGINE = "list_eng_voice";
    public static final String KEY_ENG_RATE = "eng_rate_preference";
    public static final String KEY_ENGLISH_VOLUME = "eng_volume_preference";
    public static final String KEY_ENGLISH_TEST = "eng_test_voice";
    public static final String KEY_RESET_RATE = "eng_restore_default_rate";
    public static final String KEY_RESET_VOLUME = "eng_restore_default_volume";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = prefs.edit();
        //PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        editor.apply();

        if(savedInstanceState == null)
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.pref_content, new SettingsFragment())
                    .commit();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        /* Opens Android TTS settings */
        if (i == R.id.action_tts_settings) {
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
