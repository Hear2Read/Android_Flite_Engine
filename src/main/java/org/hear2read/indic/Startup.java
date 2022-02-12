package org.hear2read.indic;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

//import com.crashlytics.android.Crashlytics;
//import com.crashlytics.android.core.CrashlyticsCore;

//import io.fabric.sdk.android.Fabric;

import org.hear2read.indic.tts.FliteTtsService;
import org.hear2read.indic.tts.Voice;
import org.hear2read.indic.util.Utility;
import org.hear2read.indic.util.VoiceAddTask;
import org.hear2read.indic.util.VoiceRemoveTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.hear2read.indic.tts.CheckVoiceData.getCopiedVoiceList;


/**
 * Created by perilon on 6/17/16.
 */

/* The point here is to run on startup.  Copies data from assets directory to phone storage. */

/* A bunch of additions made - assets copied from a different app - the voice app
 - shyam*/

public class Startup extends Application {
    private static final String LOG_TAG = "Flite_Java_" + Startup.class.getSimpleName();

    private static String FLITE_DATA_PATH;

    /* Preference file names */
    public static final String mDefaultSharedPrefFile = "org.hear2read.indic";
    public static final String mVoicesSharedPrefFile = "org.hear2read.indic.voices";
    public static final String mPackagesSharedPrefFile = "org.hear2read.indic.packages";
    public static final String mVersionNosSharedPrefFile = "org.hear2read.indic.versions";

    /* Preference keys */
    public static final String VOICE_PACKAGES_PREF = "VoicePackages";
    public static final String VOICE_NAMES_PREF = "VoiceNames";
    //public static final String KEY_OTHER_ENG_SWITCH = "switch_other_eng";
    public static final String KEY_OTHER_ENG_ENGINE = "other_eng_engine";
    //public static final String KEY_VOLUME_NORMALIZE_SWITCH = "switch_volume_normalize";

    private TextToSpeech mTts;
    //private ArrayList<String> TtsEngines;

    /* Returns path of Hear2Read data directory */
    public static String getDataPath() {
        return FLITE_DATA_PATH;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Starts Crashlytics
        /*Fabric.with(this, new Crashlytics.Builder().core(
                new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());*/

        final String LOG_TAG = "Flite_Java_" + Startup.class.getSimpleName();

        Context mContext = getApplicationContext();
        FLITE_DATA_PATH = String.valueOf(mContext.getExternalFilesDir(null)) + "/hear2read-data/";

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Initiate a TTS instance to query available TTS Engines, for English
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Log.e(LOG_TAG, "Couldn't init TTS to query");
                }
                else
                    getOtherEnglish();
            }
        });

        int result = TextToSpeech.Engine.CHECK_VOICE_DATA_PASS;
        Intent returnData = new Intent();
        //returnData.putExtra(TextToSpeech.Engine.EXTRA_VOICE_DATA_ROOT_DIRECTORY,
        //        FLITE_DATA_PATH);

        new File(FLITE_DATA_PATH + "cg/").mkdirs();

        // Get preference files containing voice names, packages, versions of installed voice apps
        SharedPreferences mDefaultPreferences = getSharedPreferences(mDefaultSharedPrefFile, 0);
        SharedPreferences mVoicePreferences = getSharedPreferences(mVoicesSharedPrefFile, 0);
        SharedPreferences mPackagePreferences = getSharedPreferences(mPackagesSharedPrefFile, 0);
        SharedPreferences mVersionPreferences = getSharedPreferences(mVersionNosSharedPrefFile, 0);
        ArrayList<String> packageNames = new ArrayList<>();
        //SharedPreferences uninstalledPreferences;
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);


        Set<String> addedVoicePackages = mDefaultPreferences.getStringSet(VOICE_PACKAGES_PREF,
                new HashSet<String>());

        // Check if files not copied from installed voice apps per preferences
        SparseArray<Set<String>> updatedVoicePackages = Utility.syncedVoicesFromPreferences(packages,
                addedVoicePackages);

        // Add voice files from installed voices
        for(String packageName : updatedVoicePackages.get(0)) {
            VoiceAddTask voiceAddTask = new VoiceAddTask(this.getApplicationContext(),
                    new VoiceAddTask.VoiceAddAsyncResponse() {
                        @Override
                        public void voiceAddFinished(String voxName) {

                        }
                    });
            voiceAddTask.execute(packageName);
        }
        // Remove voice files from uninstalled voice apps
        Utility.removeVoicePrefs(updatedVoicePackages.get(1),
                mDefaultPreferences,
                mPackagePreferences,
                mVersionPreferences,
                mVoicePreferences);

        Set<String> persistingVoices =
                Utility.getPersistingUninstalledVoices(
                        mDefaultPreferences.getStringSet(VOICE_NAMES_PREF,
                        new HashSet<String>()),
                        getCopiedVoiceList());
        for (String voxName : persistingVoices) {
            //remove copied voices not in preferences
            VoiceRemoveTask voiceRemoveTask = new VoiceRemoveTask(this.getApplicationContext(),
                    new VoiceRemoveTask.VoiceRemoveAsyncResponse() {
                        @Override
                        public void voiceRemoveFinished(String voxName) {

                        }
                    });
            voiceRemoveTask.execute(voxName);
        }

        // Delete English voice if present (was only installed in one version of the new app)
        String engFlitevoxName = "male.cg.flitevox";
        String engVoicePath = Startup.getDataPath() + "cg/" + "eng" + "/" + "USA" + "/";

        if (Utility.pathExists(engVoicePath + engFlitevoxName)) {
            File file = new File(engVoicePath + engFlitevoxName);
            boolean deleted = file.delete();
        }
    }

    /* Returns log - for mailing purposes - shyam */
    public static String getLog(Context c) {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line);
                log.append("\n");
            }

            return log.toString();

        } catch (IOException e) {
            return null;
        }
    }

    /* Check voices in preference and returns available voices - shyam */
    public static ArrayList<Voice> getVoices(SharedPreferences sharedPreferences) {
        Set<String> voiceSet = sharedPreferences.getStringSet("VoiceNames",
                new HashSet<String>());
        ArrayList<String> voiceList = new ArrayList<String>(voiceSet);
        //Log.v(LOG_TAG, "Voicelist: " + voiceList);

        ArrayList<Voice> voices = new ArrayList<Voice>();

        for(String strLine:voiceList) {
            Voice vox = new Voice(strLine);
            if (!vox.isValid())
                continue;
            voices.add(vox);
        }

        return voices;
    }

    /* Gets an English TTS to start with. This can later be modified by the user - shyam */
    public void getOtherEnglish() {
        List<TextToSpeech.EngineInfo> engineInfoList = mTts.getEngines();
        //TtsEngines = new ArrayList<>();
        for (TextToSpeech.EngineInfo engine : engineInfoList) {
            //Log.v(LOG_TAG, "Adding engine: " + engine.name);
            // Order of preference we provided: google>espeak>pico
            if (engine.name.contains("google")) {
                //TtsEngines.add(engine.name);
                setDefaultEnglish(engine.name);
                mTts.shutdown();
                return;
            }
            if (engine.name.contains("espeak")) {
                //TtsEngines.add(engine.name);
                setDefaultEnglish(engine.name);
                mTts.shutdown();
                return;
            }
            if (engine.name.contains("svox")) {
                //TtsEngines.add(engine.name);
                setDefaultEnglish(engine.name);
                mTts.shutdown();
                return;
            }
        }
        // Shutdown the TTS instance, its work is done
        mTts.shutdown();
    }

    /* Set the default engine and voice - shyam */
    private void setDefaultEnglish(String name) {
        Locale defaultLocale = FliteTtsService.parseEngineLocalePrefFromList(
                Settings.Secure.getString(getContentResolver(), "tts_default_locale"),
                name);
        //Log.v(LOG_TAG, "setDefaultEnglish on: " +  name + ", gives locale: " + defaultLocale);
        SharedPreferences sharedPref;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if ((defaultLocale == null) || (defaultLocale.getISO3Language().isEmpty())) {
            //Log.v(LOG_TAG, "Default engine set to: " +  name);
            sharedPref.edit().putString(KEY_OTHER_ENG_ENGINE, name).apply();
            return;
        }

        String defVoice;
        if (!defaultLocale.getISO3Country().isEmpty())
            if (!defaultLocale.getVariant().isEmpty())
                defVoice = defaultLocale.getISO3Language() + "-"
                        + defaultLocale.getISO3Country() + "-"
                        + defaultLocale.getVariant();
            else
                defVoice = defaultLocale.getISO3Language() + "-"
                        + defaultLocale.getISO3Country();
        else
            defVoice = defaultLocale.getISO3Language();

        //Log.v(LOG_TAG, "Default engine set to: " +  name + "," + defVoice);
        sharedPref.edit().putString(KEY_OTHER_ENG_ENGINE, name + "," + defVoice).apply();
    }

/*
    private class GetEnglishEnginesTask extends AsyncTask <String, Void, Void> {

        @Override
        protected Void doInBackground(String... engines) {
            return null;
        }
    }
*/
}
