package org.hear2read.indic.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SeekBarPreference;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import org.hear2read.indic.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.hear2read.indic.Startup.KEY_OTHER_ENG_ENGINE;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENGLISH_TEST;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENGLISH_ENGINE;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENGLISH_VOLUME;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENG_RATE;
import static org.hear2read.indic.util.SettingsActivity.KEY_RESET_RATE;
import static org.hear2read.indic.util.SettingsActivity.KEY_RESET_VOLUME;

/**
 * Fragment used in the settings activity containing most of the UI
 * Created by shyam on 7/6/18.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener, TextToSpeech.OnInitListener {
    private final static String LOG_TAG = "Flite_Java_" + SettingsFragment.class.getSimpleName();

    private ProgressDialog mEngVoiceProgressDialog;
    private TextToSpeech mIndTts;
    private boolean mTtsInitialized = false;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        mEngVoiceProgressDialog = null;
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));
        mIndTts = new TextToSpeech(Objects.requireNonNull(getActivity()),
                this, "org.hear2read.indic");

        showProgressDialog();

        //setPreferenceScreen(createPreferenceHierarchy());
        getEngines();
        createPreferenceHierarchy();
    }

    /* Build the screen programmatically */
    private void createPreferenceHierarchy() {

        ListPreference mEngVoiceListPreference = (ListPreference) findPreference(KEY_ENGLISH_ENGINE);
        if(mEngVoiceListPreference == null) {
            mEngVoiceListPreference = new ListPreference(Objects.requireNonNull(getActivity()));
            mEngVoiceListPreference.setKey(KEY_ENGLISH_ENGINE);
            mEngVoiceListPreference.setEntryValues(new String [0] );
            mEngVoiceListPreference.setEntries(new String [0] );
            //mEngVoiceListPreference = (ListPreference) findPreference(KEY_ENGLISH_ENGINE);
            //mEngVoiceListPreference.setTitle("English Voice List");
        }
        mEngVoiceListPreference.setTitle("Change English Voice");

        SeekBarPreference ratePreference = (SeekBarPreference) findPreference(KEY_ENG_RATE);
        if(ratePreference == null) {
            ratePreference = new SeekBarPreference(Objects.requireNonNull(getActivity()));
            ratePreference.setKey(KEY_ENG_RATE);
        }
        ratePreference.setTitle("English Speech Rate");
        //ratePreference.setSummary("Set the rate of speech of the English voice");
        ratePreference.setMax(300);
        ratePreference.setMin(50);
        ratePreference.setDefaultValue(100);
        ratePreference.setAdjustable(true);
        ratePreference.setEnabled(true);
        ratePreference.setShouldDisableView(false);


        SeekBarPreference engVolPreference = (SeekBarPreference) findPreference(KEY_ENGLISH_VOLUME);
        if(engVolPreference == null) {
            engVolPreference = new SeekBarPreference(Objects.requireNonNull(getActivity()));
            engVolPreference.setKey(KEY_ENGLISH_VOLUME);
        }
        engVolPreference.setTitle("English Speech Volume");
        //engVolPreference.setSummary("Set the speech volume of the English voice");
        engVolPreference.setMax(100);
        engVolPreference.setMin(0);
        engVolPreference.setDefaultValue(100);
        //engVolPreference.setSeekBarIncrement(10);
        engVolPreference.setAdjustable(true);
        engVolPreference.setEnabled(true);
        engVolPreference.setShouldDisableView(false);

        Preference testVoiceEng = findPreference(KEY_ENGLISH_TEST);
        if(testVoiceEng == null) {
            testVoiceEng = new Preference(Objects.requireNonNull(getActivity()));
            testVoiceEng.setKey(KEY_ENGLISH_TEST);
        }
        testVoiceEng.setTitle("Listen to English sample");
        testVoiceEng.setSummary("Play a short demonstration in English");
        testVoiceEng.setOnPreferenceClickListener(this);

        Preference resetDefaultRate = findPreference(KEY_RESET_RATE);
        if(resetDefaultRate == null) {
            resetDefaultRate = new Preference(Objects.requireNonNull(getActivity()));
            resetDefaultRate.setKey(KEY_RESET_RATE);
        }
        resetDefaultRate.setTitle("Reset English rate");
        resetDefaultRate.setSummary("Reset rate at which English text is spoken to normal");
        resetDefaultRate.setOnPreferenceClickListener(this);

        Preference resetDefaultVolume = findPreference(KEY_RESET_VOLUME);
        if(resetDefaultVolume == null) {
            resetDefaultVolume = new Preference(Objects.requireNonNull(getActivity()));
            resetDefaultVolume.setKey(KEY_RESET_VOLUME);
        }
        resetDefaultVolume.setTitle("Reset English volume");
        resetDefaultVolume.setSummary("Reset volume at which English text is spoken to default");
        resetDefaultVolume.setOnPreferenceClickListener(this);

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        /* Test English voice */
        if (Objects.equals(preference.getKey(), KEY_ENGLISH_TEST)) {
            //Log.v(LOG_TAG, "onPreferenceClick: " + KEY_ENGLISH_TEST);
            if(mTtsInitialized) {
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "test");
                mIndTts.speak("This is an example of speech synthesis in English",
                        TextToSpeech.QUEUE_FLUSH, map);
            }
            return true;
        }

        /* Reset English rate */
        if (Objects.equals(preference.getKey(), KEY_RESET_RATE)) {
            //Log.v(LOG_TAG, "onPreferenceClick: " + KEY_RESET_RATE);
            SeekBarPreference engRatePreference = (SeekBarPreference) findPreference(KEY_ENG_RATE);
            engRatePreference.setValue(100);
            return true;
        }

        /* Reset English volume */
        if (Objects.equals(preference.getKey(), KEY_RESET_VOLUME)) {
            //Log.v(LOG_TAG, "onPreferenceClick: " + KEY_RESET_VOLUME);
            SeekBarPreference engVolPreference = (SeekBarPreference)
                    findPreference(KEY_ENGLISH_VOLUME);
            engVolPreference.setValue(100);
            return true;
        }
        return false;
    }

    /* Show dialog initializing English voices */
    private void showProgressDialog() {
        if (mEngVoiceProgressDialog == null) {
            mEngVoiceProgressDialog = new ProgressDialog(getActivity());
            mEngVoiceProgressDialog.setCancelable(false);
            mEngVoiceProgressDialog.setCanceledOnTouchOutside(false);
            mEngVoiceProgressDialog.setMessage("Loading English voices");
            mEngVoiceProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        mEngVoiceProgressDialog.show();
    }

    /* Hide dialog */
    private void hideProgressDialog() {
        if (mEngVoiceProgressDialog != null && mEngVoiceProgressDialog.isShowing()) {
            mEngVoiceProgressDialog.dismiss();
            mEngVoiceProgressDialog = null;
        }
    }

    @Override
    public void onInit(int status) {
        //Log.v(LOG_TAG, "TTS init: " + status);
        mTtsInitialized = true;
    }

    /* Piece of code for querying and getting all engines supporting English and the corresponding
    * voices */
    // Based on StackOverflow, user brandall
    // https://stackoverflow.com/a/25723262

    /* Container class with engine details */
    public class ContainerVoiceEngine {

        private String label;
        private String packageName;
        private ArrayList<String> voices;
        private Intent intent;

        ContainerVoiceEngine() {

        }

        public ContainerVoiceEngine(final String label, final String packageName, final ArrayList<String> voices, final Intent intent) {

            this.label = label;
            this.packageName = packageName;
            this.voices = voices;
            this.intent = intent;
        }

        public Intent getIntent() {
            return intent;
        }

        public void setIntent(final Intent intent) {
            this.intent = intent;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(final String label) {
            this.label = label;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(final String packageName) {
            this.packageName = packageName;
        }

        public ArrayList<String> getVoices() {
            return voices;
        }

        public void setVoices(final ArrayList<String> voices) {
            this.voices = voices;
        }
    }

    private ArrayList<ContainerVoiceEngine> containerVEArray;
    // to keep track of number of intents started
    private int requestCount;

    /* Gets all engines and fires off intents to check available voices */
    private void getEngines() {
        //Log.v(LOG_TAG, "getEngines");

        requestCount = 0;

        final Intent ttsIntent = new Intent();
        ttsIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        final PackageManager pm = Objects.requireNonNull(getActivity()).getPackageManager();

        final List<ResolveInfo> list = pm.queryIntentActivities(ttsIntent, PackageManager.GET_META_DATA);

        containerVEArray = new ArrayList<ContainerVoiceEngine>(list.size());

        for (int i = 0; i < list.size(); i++) {

            final ContainerVoiceEngine cve = new ContainerVoiceEngine();

            cve.setLabel(list.get(i).loadLabel(pm).toString());
            cve.setPackageName(list.get(i).activityInfo.applicationInfo.packageName);

            final Intent getIntent = new Intent();
            getIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

            getIntent.setPackage(cve.getPackageName());
            getIntent.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
            getIntent.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES);

            cve.setIntent(getIntent);

            containerVEArray.add(cve);
        }

        //Log.d(LOG_TAG, "containerVEArray: " + containerVEArray.size());

        for (int i = 0; i < containerVEArray.size(); i++) {
            startActivityForResult(containerVEArray.get(i).getIntent(), i);
        }

    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        //Log.i(LOG_TAG, "onActivityResult: requestCount: " + " - requestCode: " + requestCode);

        // Results from the engines containing available voices
        requestCount++;

        try {
            if (data != null) {
                if (data.hasExtra("availableVoices")) {
                    final ArrayList<String> allVoices;
                    ArrayList<String> englishVoices;
                    englishVoices = new ArrayList<>();
                    allVoices = data.getStringArrayListExtra("availableVoices");

                    for (String voice : allVoices) {
                        if (voice.startsWith("en"))
                            englishVoices.add(voice);
                    }
                    containerVEArray.get(requestCode).setVoices(englishVoices);
                } else {
                    containerVEArray.get(requestCode).setVoices(new ArrayList<String>());
                }
            }

            // On finishing the final request, populate the English voice UI list
            if (requestCount == containerVEArray.size()) {
                populateEnglishVoiceList();

            }

        } catch (final IndexOutOfBoundsException e) {
            Log.e(LOG_TAG, "IndexOutOfBoundsException");
            e.printStackTrace();
        } catch (final NullPointerException e) {
            Log.e(LOG_TAG, "NullPointerException");
            e.printStackTrace();
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Exception");
            e.printStackTrace();
        }
    }

    /* Populate the list preference of English voices available */
    private void populateEnglishVoiceList() {
        ListPreference engVoicePref = (ListPreference) findPreference(KEY_ENGLISH_ENGINE);
        ArrayList<String> allEngVoices = new ArrayList<>();
        ArrayList<String> allEngVoicesValues = new ArrayList<>();

        // get the voice names from the container class array
        for (ContainerVoiceEngine cve : containerVEArray) {
            String label = cve.getLabel();
            String packageName = cve.getPackageName();
            ArrayList<String> voices = cve.getVoices();
            for (String voice : voices) {
                String fullVoiceLabel = label + ", " + voice;
                String fullVoiceValue = packageName + "," + voice;
                allEngVoices.add(fullVoiceLabel);
                allEngVoicesValues.add(fullVoiceValue);
            }
        }
        //Log.v(LOG_TAG, "eng voices: " + allEngVoices);
        String [] allEngVoicesArray = allEngVoices.toArray(new String[0]);
        String [] allEngValuesArray = allEngVoicesValues.toArray(new String[0]);
        //Log.v(LOG_TAG, "eng voices array: " + Arrays.toString(allEngVoicesArray));

        // Update preference
        engVoicePref.setEntries(allEngVoicesArray);
        engVoicePref.setEntryValues(allEngValuesArray);
        //engVoicePref.getValue();
        if (engVoicePref.getValue() == null) {
            SharedPreferences sharedPref;
            sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));
            String defEngVoice = sharedPref.getString(KEY_OTHER_ENG_ENGINE, "");
            CharSequence[] engValues = engVoicePref.getEntryValues();
            if (!defEngVoice.isEmpty() && (defEngVoice.split(",").length > 0))
                engVoicePref.setValue(defEngVoice);
        }
        bindPreferenceSummaryToValue(findPreference(KEY_ENGLISH_ENGINE));
        //setPreferenceScreen(createPreferenceHierarchy());
        //createPreferenceHierarchy();
        hideProgressDialog();
        showAlertDialog();
    }

    /* Alert informing users that Indic settings are to be accessed through Android TTS settings */
    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View checkBoxView = getLayoutInflater().inflate(R.layout.alert_settings, null);
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getActivity()));
        String skipAlert = prefs.getString("skipAlertSettings", "unchecked");
        if (Objects.equals(skipAlert, "checked"))
            return;
        final CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox_settings);
        builder.setView(checkBoxView);
        builder.setMessage("This is the settings for English speech. Use Android TTS settings to" +
                " adjust Indic speech");
        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String checkBoxStatus = "unchecked";
                if (checkBox.isChecked()) checkBoxStatus = "checked";
                prefs.edit().putString("skipAlertSettings", checkBoxStatus).apply();
            }
        });
        if (!Objects.equals(skipAlert, "checked")) builder.show();
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener =
            new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    String stringValue = value.toString();

                    // Update list preference summary to current value
                    if (preference instanceof ListPreference) {
                        //Log.v(LOG_TAG, "onPreferenceChange, stringValue: " + stringValue);
                        ListPreference engVoiceListPreference = (ListPreference) preference;
                        //Log.v(LOG_TAG, "onPreferenceChange, getEntryValues: " +
                        //        Arrays.toString(engVoiceListPreference.getEntryValues()));
                        int index = engVoiceListPreference.findIndexOfValue(stringValue);

                        preference.setSummary(
                                index >= 0
                                        ? engVoiceListPreference.getEntries()[index]
                                        : null);
                    }
                    else {
                        preference.setSummary(stringValue);
                    }
                    return true;
                }
            };

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIndTts.shutdown();
    }
}
