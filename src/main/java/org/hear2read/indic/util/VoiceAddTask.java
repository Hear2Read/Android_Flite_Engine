package org.hear2read.indic.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

//1106 import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.Startup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hear2read.indic.Startup.VOICE_NAMES_PREF;
import static org.hear2read.indic.Startup.VOICE_PACKAGES_PREF;
import static org.hear2read.indic.Startup.mDefaultSharedPrefFile;
import static org.hear2read.indic.Startup.mPackagesSharedPrefFile;
import static org.hear2read.indic.Startup.mVersionNosSharedPrefFile;
import static org.hear2read.indic.Startup.mVoicesSharedPrefFile;
import static org.hear2read.indic.util.Utility.BROADCAST_END;

/**
 * AsyncTask to copy voice file from voice app to data directory
 * Created by shyam on 14/12/17.
 */

public class VoiceAddTask extends AsyncTask <String, Void, Boolean > {
    private static final String LOG_TAG = "Flite_Java_" + VoiceAddTask.class.getSimpleName();
    private static final String BROADCAST_START = "org.hear2read.indic.VOICE_UPDATE_START";

    private String mPackage;
    private Uri BASE_CONTENT_URI;

    private ContentResolver mContentResolver;

    private String mISO3;
    private String mCountry;
    private String mVariant;
    private String mVoiceName;

    private SharedPreferences mDefaultPreferences;
    private SharedPreferences mVoicePreferences;
    private SharedPreferences mPackagePreferences;
    private SharedPreferences mVersionPreferences;

    private WeakReference<Context> mWeakContext;
    private VoiceAddAsyncResponse mDelegate = null;

    public interface VoiceAddAsyncResponse {
        void voiceAddFinished(String voxName);
    }

    public VoiceAddTask(Context context, VoiceAddAsyncResponse delegate) {
        mWeakContext = new WeakReference<>(context);
        mDelegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        //Log.v(LOG_TAG, "Starting new addVoice task: ");
    }

    @Override
    protected Boolean doInBackground(String... strings) {

        mPackage = strings[0];
        Context context = mWeakContext.get();
        if(context == null) return false;

        mDefaultPreferences = context.getSharedPreferences(mDefaultSharedPrefFile, 0);
        mVoicePreferences = context.getSharedPreferences(mVoicesSharedPrefFile, 0);
        mPackagePreferences = context.getSharedPreferences(mPackagesSharedPrefFile, 0);
        mVersionPreferences = context.getSharedPreferences(mVersionNosSharedPrefFile, 0);

        if (mPackage == null) {
            //1106  Crashlytics.log(Log.ERROR, LOG_TAG, "Invalid package array: " + Arrays.toString(strings));
            return false;
        }

        BASE_CONTENT_URI = Uri.parse("content://" + mPackage);

        if (!readSharedVoiceparams()) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Unable to read voice.params");
            return false;
        }

        String flitevoxName = mVariant + ".cg.flitevox";
        String voicePath = Startup.getDataPath() + "cg/" + mISO3 + "/" + mCountry + "/";

        if (Utility.pathExists(voicePath + flitevoxName)) {
            File file = new File(voicePath + flitevoxName);
            boolean deleted = file.delete();
        }

        new File(voicePath).mkdirs();
        if (!copySharedAssets(flitevoxName, voicePath))
        {
           //1106  Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to copy: " + mVoiceName);
            return false;
        }

        //Log.v(LOG_TAG, "Current voice set: " + mDefaultPreferences.getStringSet("VoiceNames",
        //        new HashSet<String>()));

        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {

        SharedPreferences.Editor sharedprefeditor = mVoicePreferences.edit();
        sharedprefeditor.putString(mPackage, mVoiceName);
        sharedprefeditor.apply();

        sharedprefeditor = mPackagePreferences.edit();
        sharedprefeditor.putString(mVoiceName, mPackage);
        sharedprefeditor.apply();

        sharedprefeditor = mVersionPreferences.edit();
        Context context = mWeakContext.get();
        try {
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(mPackage, 0);
            sharedprefeditor.putInt(mPackage, pinfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            sharedprefeditor.putInt(mPackage, -1);
        }
        sharedprefeditor.apply();

        Set<String> packages = mDefaultPreferences.getStringSet(VOICE_PACKAGES_PREF,
                new HashSet<String>());
        packages.add(mPackage);
        sharedprefeditor = mDefaultPreferences.edit();
        sharedprefeditor.putStringSet(VOICE_PACKAGES_PREF, packages);
        sharedprefeditor.apply();

        Set<String> voices = mDefaultPreferences.getStringSet(VOICE_NAMES_PREF,
                new HashSet<String>());
        voices.add(mVoiceName);
        sharedprefeditor = mDefaultPreferences.edit();
        sharedprefeditor.putStringSet(VOICE_NAMES_PREF, voices);
        sharedprefeditor.apply();

        //getCopiedVoices(mDefaultPreferences);
        if (aBoolean) sendMessage();
        mDelegate.voiceAddFinished(mVoiceName);

        /*Intent broadcastEnd = new Intent();
        broadcastEnd.setAction(BROADCAST_END);
        mWeakContext.get().sendBroadcast(broadcastEnd);
        mUpdateListener.update();*/
    }

    @Override
    protected void onCancelled(Boolean aBoolean) {
        mDelegate.voiceAddFinished("false");
    }

    /* Sends broadcast on completion, mainly for FliteManager activity list */
    private void sendMessage() {
        //Log.d(LOG_TAG, "Broadcasting message");
        Intent intent = new Intent(BROADCAST_END);
        intent.putExtra("VoiceName", mVoiceName);

        Context context = mWeakContext.get();
        if(context == null) return;

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /* Helper to copy file */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    /* Helper to copy shared asset file */
    private boolean copySharedAssets(String assetFileName, String outFilePath) {
        AssetFileDescriptor sharedAssetFd;
        //Log.v(LOG_TAG, "copySharedAssets: " + assetFileName + outFilePath);

        Context context = mWeakContext.get();
        if(context == null) return false;

        mContentResolver = context.getContentResolver();

        try{
            Uri CONTENT_URI_FLITEVOX = Uri.withAppendedPath(BASE_CONTENT_URI, assetFileName);
            sharedAssetFd = mContentResolver.openAssetFileDescriptor(CONTENT_URI_FLITEVOX,
                    "r");
        }
        catch (FileNotFoundException e)
        {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to get asset file: " + assetFileName
              //      + ", " + e.getMessage());
            return false;
        }
        InputStream in = null;
        OutputStream out = null;
        try {
            assert sharedAssetFd != null;

            in = sharedAssetFd.createInputStream();
            File outFile = new File(outFilePath, assetFileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);

        } catch (IOException e) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to copy asset file: " + assetFileName +
                  //  ", " + e.getMessage());
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
        return true;
    }

    /* Helper to read voice parameters from voice app */
    private boolean readSharedVoiceparams() {

        String assetFileName = "voices.list";

        AssetFileDescriptor sharedAssetFd;

        Context context = mWeakContext.get();
        if(context == null) return false;

        mContentResolver = context.getContentResolver();

        try {
            Uri CONTENT_URI_FLITEVOX = Uri.withAppendedPath(BASE_CONTENT_URI, assetFileName);
            sharedAssetFd = mContentResolver.openAssetFileDescriptor(CONTENT_URI_FLITEVOX,
                    "r");
        } catch (FileNotFoundException e)
        {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to get asset file: " + assetFileName +
                   // " to read, " + e.getMessage());
            return false;
        }

        InputStream in = null;

        try {
            assert sharedAssetFd != null;

            in = sharedAssetFd.createInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line = bufferedReader.readLine().split("\t")[0];
            String [] params = line.split("-");

            if (params.length != 3) {
                //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Incorrect voiceparams: " + line);
                return false;
            }

            mVoiceName = line;
            mISO3 = params[0];
            mCountry = params[1];
            mVariant = params[2];
            //if(DBG) Log.v(LOG_TAG, "Read voice params: " + Arrays.toString(params));

        } catch (IOException e) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Failed to read asset file: " + assetFileName +
                   // ", " + e.getMessage());
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
        return true;
    }
}