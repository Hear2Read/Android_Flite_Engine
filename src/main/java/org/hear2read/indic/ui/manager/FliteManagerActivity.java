package org.hear2read.indic.ui.manager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.hear2read.indic.tts.CheckVoiceData;
import org.hear2read.indic.ui.FliteInfoActivity;
import org.hear2read.indic.R;
import org.hear2read.indic.tts.Voice;
import org.hear2read.indic.ui.downloader.VoiceDownloadActivity;
import org.hear2read.indic.util.SettingsActivity;
import org.hear2read.indic.util.Utility;
import org.hear2read.indic.util.VoiceAddTask;
import org.hear2read.indic.util.VoiceRemoveTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hear2read.indic.Startup.VOICE_NAMES_PREF;
import static org.hear2read.indic.Startup.VOICE_PACKAGES_PREF;
import static org.hear2read.indic.Startup.mDefaultSharedPrefFile;
import static org.hear2read.indic.Startup.mPackagesSharedPrefFile;
import static org.hear2read.indic.Startup.mVersionNosSharedPrefFile;
import static org.hear2read.indic.Startup.mVoicesSharedPrefFile;
import static org.hear2read.indic.tts.CheckVoiceData.getCopiedVoiceList;
import static org.hear2read.indic.util.Utility.BROADCAST_END;

/**
 * Home activity
 * Contains list of voices installed
 * Does multiple checks for installed voices and copies voice files as well
 * Also has the Add Voices button to download more voices
 * Created by shyam on 16/12/17.
 */


public class FliteManagerActivity extends AppCompatActivity
        implements VoiceAddTask.VoiceAddAsyncResponse, VoiceRemoveTask.VoiceRemoveAsyncResponse {
    private static final String LOG_TAG = "Flite_Java_" +
            FliteManagerActivity.class.getSimpleName();

    private static List<LanguageListItem> languagesList = new ArrayList<>();
    private static AlertDialog mAlert = null;
    private RecyclerView mLanguagesListView;
    private LanguagesAdapter mLanguagesAdapter;
    private static ArrayList<Voice> mCopiedVoices;
    private static String copyText;
    private static String updateText;
    private static String warningText;
    private static String warningMessage;
    private static ProgressDialog mVoiceLoadProgressDialog = null;
    private static ProgressDialog mVoiceCopyProgressDialog = null;
    private static boolean progressDialogShowing = false;
    private static boolean alertDialogShowing = false;
    private AlertDialog mOpeningDialog = null;
    private int mAsyncTaskCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        registerUpdateReceiver();

        setContentView(R.layout.activity_flite_manager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mLanguagesListView = (RecyclerView) findViewById(R.id.list_languages);
        Button mVoiceAddButton = (Button) findViewById(R.id.button_add_voice);

        // Get text prompts
        updateText = getString(R.string.updating_voices);
        copyText = getString(R.string.copying_voices);
        warningText = getString(R.string.no_voices_background);
        warningMessage = getString(R.string.no_voices_dialog);

        mLanguagesAdapter = new LanguagesAdapter(this, languagesList);
        mCopiedVoices = new ArrayList<>();

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLanguagesListView.setLayoutManager(mLayoutManager);
        mLanguagesListView.setItemAnimator(new DefaultItemAnimator());
        mLanguagesListView.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        mLanguagesListView.setAdapter(mLanguagesAdapter);

        mVoiceAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FliteManagerActivity.this, VoiceDownloadActivity.class);
                FliteManagerActivity.this.startActivity(intent);
            }
        });

        showAlertMessage();//Shyam 20190402
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideNoVoiceAlert();
        syncVoices();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unregisterUpdateReceiver();
        hideProgressDialogs(); //Shyam 20190402
        super.onDestroy();
    }


    /* Do a bunch of (redundant) checks verifying copied and installed voices*/
    private void syncVoices() {
        SharedPreferences preferences = getSharedPreferences(mDefaultSharedPrefFile, 0);
        PackageManager pm = getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_META_DATA);


        Set<String> addedVoicePackages = preferences.getStringSet(VOICE_PACKAGES_PREF,
                new HashSet<String>());
        SharedPreferences voicePreferences =
                getSharedPreferences(mVoicesSharedPrefFile, 0);
        SharedPreferences packagePreferences =
                getSharedPreferences(mPackagesSharedPrefFile, 0);
        SharedPreferences versionPreferences =
                getSharedPreferences(mVersionNosSharedPrefFile, 0);
        SparseArray<Set<String>> updatedVoicePackages = Utility.syncedVoicesFromPreferences(packages,
                addedVoicePackages);
        //add voice files from installed voices
        Set<String> missingVoices = updatedVoicePackages.get(0);
        Set<String> uninstalledVoices = updatedVoicePackages.get(1);
        Utility.removeVoicePrefs(uninstalledVoices,
                preferences,
                packagePreferences,
                versionPreferences,
                voicePreferences);

        Set<String> uncopiedVoices = CheckVoiceData.getUncopiedVoices(addedVoicePackages,
                voicePreferences);

        Set<String> updatedVoices = CheckVoiceData.getUpdatedVoices(addedVoicePackages,
                versionPreferences, getPackageManager());

        missingVoices.addAll(uncopiedVoices);
        missingVoices.addAll(updatedVoices);

        if(missingVoices.isEmpty()) {
            checkCopiedVoices();
        }
        else {
            copyVoices(missingVoices);
        }
    }


    /* Populate list with copied voices, first making sure no persisting voices are present */
    private void checkCopiedVoices() {
        SharedPreferences preferences = getSharedPreferences(mDefaultSharedPrefFile, 0);
        Set<String> persistingVoices =
                Utility.getPersistingUninstalledVoices(preferences.getStringSet(VOICE_NAMES_PREF,
                                new HashSet<String>()),
                        getCopiedVoiceList());
        if (persistingVoices.isEmpty()) {
            buildVoiceList();
        }
        else {
            for (String vox : persistingVoices) {
                VoiceRemoveTask voiceRemoveTask =
                        new VoiceRemoveTask(this.getApplicationContext(), this);
                mAsyncTaskCounter++;
                voiceRemoveTask.execute(vox);
            }
        }
    }

    private synchronized static void toggleProgressDialogShowing(boolean value) {
        //Log.d(LOG_TAG, "progressDialogShowing setting to: " + value);
        progressDialogShowing = value;
    }

    /* Start generating the voice list*/
    private void buildVoiceList() {
        mVoiceLoadProgressDialog = null;
        CheckVoicesTask populateVoicesTask = new CheckVoicesTask(this);

        showVoiceLoadProgressDialog();
        populateVoicesTask.execute();
    }

    /* Copy voices for a set of voice package names */
    private void copyVoices(Set<String> missingVoices) {
        mVoiceCopyProgressDialog = null;

        showVoiceCopyProgressDialog();
        mAsyncTaskCounter = 0;
        for (String packageName : missingVoices) {
            mAsyncTaskCounter++;
            VoiceAddTask voiceAddTask = new VoiceAddTask(this.getApplicationContext(),
                    this);
            //Log.v(LOG_TAG, "mAsyncTaskCounter: " + mAsyncTaskCounter);
            voiceAddTask.execute(packageName);
        }
    }

    /* Dialog box until voice list gets initialised */
    private void showVoiceLoadProgressDialog() {
        //Log.d(LOG_TAG, "showVoiceLoadProgressDialog");
        toggleProgressDialogShowing(true);

        if (FliteManagerActivity.this.isFinishing()) return;

        if (mOpeningDialog == null || !mOpeningDialog.isShowing()) {
            if (mVoiceLoadProgressDialog == null) {
                mVoiceLoadProgressDialog = new ProgressDialog(this);
            }
            mVoiceLoadProgressDialog.setCancelable(false);
            mVoiceLoadProgressDialog.setCanceledOnTouchOutside(false);
            mVoiceLoadProgressDialog.setMessage(updateText);
            mVoiceLoadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mVoiceLoadProgressDialog.show();
            //Log.d(LOG_TAG, "showVoiceLoadProgressDialog showing");
        }
    }

    /* Hides dialog unused since -Shyam 20190402
    private static void hideVoiceLoadProgressDialog() {
        toggleProgressDialogShowing(false);
        if (mVoiceLoadProgressDialog != null && mVoiceLoadProgressDialog.isShowing()) {
            mVoiceLoadProgressDialog.dismiss();
            mVoiceLoadProgressDialog = null;
        }
    } */

    /* Hides both progress dialogs -Shyam 20190402 */
    private static void hideProgressDialogs() {
        toggleProgressDialogShowing(false);

        if (mVoiceLoadProgressDialog != null && mVoiceLoadProgressDialog.isShowing()) {
            mVoiceLoadProgressDialog.dismiss();
            mVoiceLoadProgressDialog = null;
        }

        if (mVoiceCopyProgressDialog != null && mVoiceCopyProgressDialog.isShowing()) {
            mVoiceCopyProgressDialog.dismiss();
            mVoiceCopyProgressDialog = null;
        }
    }

    /* Shows dialog */
    private void showVoiceCopyProgressDialog() {
        //Log.d(LOG_TAG, "showVoiceCopyProgressDialog");

        // to deal with new alertdialog -Shyam 20190402
        toggleProgressDialogShowing(true);

        if (mOpeningDialog == null || !mOpeningDialog.isShowing()) {
            if (FliteManagerActivity.this.isFinishing()) return;
            if (mVoiceCopyProgressDialog == null) {
                mVoiceCopyProgressDialog = new ProgressDialog(this);
            }
            mVoiceCopyProgressDialog.setCancelable(false);
            mVoiceCopyProgressDialog.setCanceledOnTouchOutside(false);
            mVoiceCopyProgressDialog.setMessage(copyText);
            mVoiceCopyProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mVoiceCopyProgressDialog.show();
        }
    }

    /* Hides dialog unused since -Shyam 20190402
    private static void hideVoiceCopyProgressDialog() {
        toggleProgressDialogShowing(false);
        if (mVoiceCopyProgressDialog != null && mVoiceCopyProgressDialog.isShowing()) {
            mVoiceCopyProgressDialog.dismiss();
            mVoiceCopyProgressDialog = null;
        }
    } */


    /* Alert informing users that Indic settings are to be accessed through Android TTS settings
     * -Shyam 20190402 */
    private void showAlertMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(FliteManagerActivity.this);
        //mOpeningDialog = new AlertDialog.Builder(FliteManagerActivity.this);
        View checkBoxView = getLayoutInflater().inflate(R.layout.alert_main, null);
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(this));
        String skipMainAlert = prefs.getString("skipAlertMain", "unchecked");
        if (Objects.equals(skipMainAlert, "checked"))
            return;
        final CheckBox checkBox = checkBoxView.findViewById(R.id.checkbox_main);
        builder.setView(checkBoxView);
        //builder.setMessage(getText(R.string.main_popup));
        builder.setTitle("Using Hear2Read TTS");
        builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Log.d(LOG_TAG, "showAlertMessage OK");
                String checkBoxStatus = "unchecked";
                if (checkBox.isChecked()) checkBoxStatus = "checked";
                prefs.edit().putString("skipAlertMain", checkBoxStatus).apply();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                // Check if one of the progress dialogs needs to be shown
                if (progressDialogShowing)
                    showVoiceLoadProgressDialog();
                mOpeningDialog = null;
            }
        });
        if (!Objects.equals(skipMainAlert, "checked")) {
            mOpeningDialog = builder.create();
            mOpeningDialog.show();
        }
    }


    /* Function for VoiceAddTask interface VoiceAddAsyncResponse interface */
    @Override
    public void voiceAddFinished(String voxName) {
        mAsyncTaskCounter--;
        //Log.v(LOG_TAG, "voiceAddFinished, mAsyncTaskCounter: " + mAsyncTaskCounter);
        if (mAsyncTaskCounter <1) {
            hideProgressDialogs();
            //start the voice list building for the activity
            checkCopiedVoices();
        }

    }

    /* Function for VoiceRemoveTask interface VoiceRemoveAsyncResponse interface */
    @Override
    public void voiceRemoveFinished(String voxName) {
        mAsyncTaskCounter--;
        //Log.v(LOG_TAG, "voiceRemoveFinished, mAsyncTaskCounter: " + mAsyncTaskCounter);
        if (mAsyncTaskCounter <1) {
            buildVoiceList();
        }

    }

    /* AsyncTask to build voice list */
    private static class CheckVoicesTask extends AsyncTask<Void, Void, ArrayList<Voice>> {

        private WeakReference<FliteManagerActivity> activityReference;

        CheckVoicesTask(FliteManagerActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {

            // get a reference to the activity if it is still there
            FliteManagerActivity activity = activityReference.get();
            if (activity == null) return;

            // modify UI
            activity.findViewById(R.id.list_languages).setVisibility(View.INVISIBLE);
            TextView backgoundText = (TextView) activity.findViewById(R.id.text_background_manager);
            backgoundText.setText(updateText);
            backgoundText.setVisibility(View.VISIBLE);

        }
        @Override
        protected ArrayList<Voice> doInBackground(Void... voids) {
            return getCopiedVoiceList();
        }

        @Override
        protected void onCancelled(ArrayList<Voice> voices) {
            hideProgressDialogs();

        }

        @Override
        protected void onPostExecute(ArrayList<Voice> voiceArrayList) {

            FliteManagerActivity activity = activityReference.get();
            if (activity == null) return;

            if (voiceArrayList.isEmpty()) {
                activity.findViewById(R.id.list_languages).setVisibility(View.INVISIBLE);
                TextView backgoundText = (TextView) activity.findViewById(R.id.text_background_manager);
                backgoundText.setText(warningText.toUpperCase());
                backgoundText.setVisibility(View.VISIBLE);

                createNoVoiceAlert(activityReference);
            }
            else
                setCopiedVoices(activity, voiceArrayList);
            hideProgressDialogs();

        }
    }

    /* create the alert dialog for no voices */
    private static void createNoVoiceAlert(WeakReference<FliteManagerActivity> activityReference) {

        final FliteManagerActivity activity = activityReference.get();
        if (activity == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(warningMessage);
        builder.setPositiveButton("Add Voice", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(activity, VoiceDownloadActivity.class);
                activity.startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        mAlert = builder.create();

        // Check again to make sure
        if (activityReference.get() == null) return;

        mAlert.show();
    }

    /* Hide alert */
    private void hideNoVoiceAlert() {
        if ((mAlert != null) && (mAlert.isShowing())) {
            mAlert.dismiss();
            mAlert = null;
        }
    }

    /* Populate the field (and the UI list) */
    private static void setCopiedVoices(FliteManagerActivity context, ArrayList<Voice> copiedVoices) {
        mCopiedVoices = copiedVoices;
        if (mCopiedVoices != null)
            populateLanguageList(context);
    }

    /* Populate the UI list */
    private static void populateLanguageList(FliteManagerActivity context) {

        languagesList.clear();

        for (Voice vox : mCopiedVoices) {
            String lang = vox.getName().split("-")[0];
            String icon = getKa(lang);
            if (!icon.isEmpty()){
                //if (DBG) Log.v(LOG_TAG, "populateLanguageList: adding language: " + lang);

                languagesList.add(new LanguageListItem(icon, lang, vox));
            }
        }

        WeakReference<FliteManagerActivity> activityWeakReference = new WeakReference<>(context);
        FliteManagerActivity activity = activityWeakReference.get();
        Collections.sort(languagesList, new Comparator<LanguageListItem>() {
            @Override
            public int compare(LanguageListItem o1, LanguageListItem o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });


        if (!languagesList.isEmpty()){
            activity.findViewById(R.id.list_languages).setVisibility(View.VISIBLE);
            activity.findViewById(R.id.text_background_manager).setVisibility(View.INVISIBLE);
            activity.mLanguagesAdapter.notifyDataSetChanged();
        }

        //if (DBG) Log.v(LOG_TAG, "populateLanguageList: mLanguagesAdapter item count: " +
        //        activity.mLanguagesAdapter.getItemCount());
    }

    /* Get the icon displayed next to the language name in the UI list */
    private static String getKa(String iso3) {
        switch (iso3) {
            case "asm":
                return "ক";
            case "ben":
                return "ক";
            case "guj":
                return "ક";
            case "hin":
            case "san":
                return "क";
            case "kan":
                return "ಕ";
            case "mal":
                return "ക";
            case "mar":
                return "क";
            case "pan":
                return "ਕ";
            case "tam":
                return "க";
            case "tel":
                return "క";
            case "ori":
                return "କ";
            default: return "";
        }
    }

    private void unregisterUpdateReceiver() {
        //if (DBG) Log.v(LOG_TAG, "unregisterUpdateReceiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void registerUpdateReceiver() {
        //if (DBG) Log.v(LOG_TAG, "registerUpdateReceiver");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(BROADCAST_END));
    }

    /* Listens for updates in installed voices */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String newLang = intent.getStringExtra("VoiceName");
            //if (DBG) Log.v(LOG_TAG, "onReceive: received local broadcast: " + newLang);

            //populateLanguageList();

            CheckVoicesTask populateVoicesTask = new CheckVoicesTask(FliteManagerActivity.this);
            populateVoicesTask.execute();
            if (!languagesList.isEmpty()) mLanguagesAdapter.notifyDataSetChanged();
            mLanguagesListView.setAdapter(mLanguagesAdapter);

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_manager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, FliteInfoActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_settings) {
            Log.d(LOG_TAG, "sttings clicked");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
