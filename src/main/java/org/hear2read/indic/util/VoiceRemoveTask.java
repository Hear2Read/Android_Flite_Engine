package org.hear2read.indic.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

//1106 import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.Startup;

import java.io.File;
import java.lang.ref.WeakReference;

import static org.hear2read.indic.util.Utility.BROADCAST_END;

/**
 * Remove uninstalled voices from data directory
 * Created by shyam on 14/12/17.
 */

public class VoiceRemoveTask extends AsyncTask <String, Void, Boolean > {
    public static final String LOG_TAG = "Flite_Java_" + VoiceRemoveTask.class.getSimpleName();
    private final WeakReference<Context> mWeakContext;

    private Uri BASE_CONTENT_URI;

    private String mISO3;
    private String mCountry;
    private String mVariant;
    private String mVoiceName;
    private VoiceRemoveAsyncResponse mDelegate = null;

    public interface VoiceRemoveAsyncResponse {
        void voiceRemoveFinished(String voxName);
    }


    public VoiceRemoveTask(Context context, VoiceRemoveAsyncResponse delegate) {
        mWeakContext = new WeakReference<>(context);
        mDelegate = delegate;
    }

    @Override
    protected Boolean doInBackground(String... strings) {

        //Changing to remove voices based on voicename alone
        //Shyam 2018.08.23
        mVoiceName = strings[0];

        if (mVoiceName == null) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Invalid package array null");
            return false;
        }

        //mVoiceName = mVoicePreferences.getString(mPackage, "");
        String [] voiceParams = mVoiceName.split("-");

        if (mVoiceName.isEmpty() ||
                voiceParams.length != 3) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Invalid voice to delete: " + mVoiceName);
            return false;
        }

        mISO3 = voiceParams[0];
        mCountry = voiceParams[1];
        mVariant = voiceParams[2];

        //BASE_CONTENT_URI = Uri.parse("content://" + mPackage);

        String flitevoxName = mVariant + ".cg.flitevox";
        String voicePath = Startup.getDataPath() + "cg/" + mISO3 + "/" + mCountry + "/";

        if (Utility.pathExists(voicePath + flitevoxName)) {
            File file = new File(voicePath + flitevoxName);
            boolean deleted = file.delete();
            //TODO
        }

        //Log.v(LOG_TAG, "Current voice set: " + mDefaultPreferences.getStringSet(
        //        "VoiceNames",
        //        new HashSet<String>()));

        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {

        if (aBoolean) sendMessage(mWeakContext.get());
        mDelegate.voiceRemoveFinished(mVoiceName);

    }

    /* Sends broadcast on completion, mainly for FliteManager activity list */
    private void sendMessage(Context context) {
        //Log.d(LOG_TAG, "Broadcasting message");
        Intent intent = new Intent(BROADCAST_END);
        intent.putExtra("VoiceName", mVoiceName);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}