/*************************************************************************/
/*                                                                       */
/*                  Language Technologies Institute              */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2010                            */
/*                        All Rights Reserved.                           */
/*                                                                       */
/*  Permission is hereby granted, free of charge, to use and distribute  */
/*  this software and its documentation without restriction, including   */
/*  without limitation the rights to use, copy, modify, merge, publish,  */
/*  distribute, sublicense, and/or sell copies of this work, and to      */
/*  permit persons to whom this work is furnished to do so, subject to   */
/*  the following conditions:                                            */
/*   1. The code must retain the above copyright notice, this list of    */
/*      conditions and the following disclaimer.                         */
/*   2. Any modifications must be clearly marked as such.                */
/*   3. Original authors' names are not deleted.                         */
/*   4. The authors' names are not used to endorse or promote products   */
/*      derived from this software without specific prior written        */
/*      permission.                                                      */
/*                                                                       */
/*  CARNEGIE MELLON UNIVERSITY AND THE CONTRIBUTORS TO THIS WORK         */
/*  DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING      */
/*  ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT   */
/*  SHALL CARNEGIE MELLON UNIVERSITY NOR THE CONTRIBUTORS BE LIABLE      */
/*  FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    */
/*  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN   */
/*  AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,          */
/*  ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF       */
/*  THIS SOFTWARE.                                                       */
/*                                                                       */
/*************************************************************************/
/*             Author:  Alok Parlikar (aup@cs.cmu.edu)                   */
/*               Date:  April 2010                                       */
/*************************************************************************/

package org.hear2read.indic.tts;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

//1106 import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.util.Utility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hear2read.indic.Startup.getDataPath;
import static org.hear2read.indic.Startup.mDefaultSharedPrefFile;
import static org.hear2read.indic.Flite.DBG;

/* Checks if the voice data is installed
 * for flite
 */

public class CheckVoiceData extends Activity {
    private final static String LOG_TAG = "Flite_Java_" + CheckVoiceData.class.getSimpleName();
    private final static String FLITE_DATA_PATH = Voice.getDataStorageBasePath();
    public final static String VOICE_LIST_FILE = FLITE_DATA_PATH+"cg/voices.list";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int result = TextToSpeech.Engine.CHECK_VOICE_DATA_PASS;
        final Intent returnData = new Intent();

        ArrayList<String> available = new ArrayList<String>();
        ArrayList<String> unavailable = new ArrayList<String>();

        /* First, make sure that the directory structure we need exists
         * There should be a "cg" folder inside the flite data directory
         * which will store all the clustergen voice data files.
         */


        if(!Utility.pathExists(FLITE_DATA_PATH+"cg")) {
            // Create the directory.

            //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Flite data directory missing. Trying to create it.");
            boolean success = false;

            try {
                //if (DBG) Log.v(LOG_TAG,FLITE_DATA_PATH);
                success = new File(FLITE_DATA_PATH+"cg").mkdirs();
            }
            catch (Exception e) {
                //1106 Crashlytics.log(Log.ERROR, LOG_TAG,"Could not create directory structure. "+e.getMessage());
                success = false;
            }

            if(!success) {
                //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Failed");
                // Can't do anything without appropriate directory structure.
                result = TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL;
                setResult(result, returnData);
                finish();
            }
        }

        // Get voices that have been copied onto the data directory
        ArrayList<Voice> voxList = getCopiedVoiceList();

        if (voxList.isEmpty()) {
            //1106 Crashlytics.log(Log.ERROR, LOG_TAG,"Problem reading voices list. This shouldn't happen!");
            result = TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL;
            setResult(result, returnData);
            finish();
        }

        // Confirm the voices are valid - it's redundant don't really why we're doing this twice
        // - shyam
        for(Voice vox : voxList) {
            if(vox.isAvailable()) {
                available.add(vox.getName());
                //if (DBG) Log.d(LOG_TAG, "onCreate: available Voice added: " +  vox.getName());
            } else {
                unavailable.add(vox.getName());
                //if (DBG) Log.w(LOG_TAG, "onCreate: unavailable Voice not added: " +  vox.getName());
            }
        }

        returnData.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, available);
        returnData.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, unavailable);
        setResult(result, returnData);
        finish();
    }

    /* Returns a verified list of Voice of copied voices */
    public static ArrayList<Voice> getCopiedVoices() {
        ArrayList<Voice> voices = new ArrayList<>();

        ArrayList<Voice> voxList = getCopiedVoiceList();

        for(Voice vox : voxList) {
            if(vox.isAvailable())
                voices.add(vox);
        }

        return voices;
    }

    /* Returns packages of voice apps that haven't had the files copied */
    public static Set<String> getUncopiedVoices(Set<String> installedVoices,
                                                        SharedPreferences voicePreferences) {
        Set<String> uncopiedVoicePackages = new HashSet<>();
        for (String voxPkg : installedVoices) {
            String voxName = voicePreferences.getString(voxPkg, "");
            if(!voxName.isEmpty()) {
                if (!doesVoiceExist(voxName)) {
                    uncopiedVoicePackages.add(voxPkg);
                }
            }
        }
        //Log.v(LOG_TAG, "getUncopiedVoices: uncopiedVoicePackages")
        return uncopiedVoicePackages;
    }

    /* Returns packagenames of voice apps that have been updated */
    public static Set<String> getUpdatedVoices(Set<String> installedVoicePackages,
                                               SharedPreferences versionPreferences,
                                               PackageManager packageManager) {
        Set<String> updatedVoicePackages = new HashSet<>();
        int copiedVersion, installedVersion;
        for (String voxPkg : installedVoicePackages) {
            copiedVersion = versionPreferences.getInt(voxPkg, 0);

            // some phones for some reason don't give packageinfo, we skip updating for them
            if (copiedVersion == -1) continue;

            try {
                installedVersion = packageManager.getPackageInfo(voxPkg, 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            if(installedVersion > copiedVersion) {
                updatedVoicePackages.add(voxPkg);
            }

        }
        //Log.v(LOG_TAG, "getUpdatedVoices updatedVoicePackages: " + updatedVoicePackages);
        return updatedVoicePackages;
    }

    /* check if voicefile for a voicename exists */
    private static boolean doesVoiceExist(String voxname) {
        String [] voiceParams = voxname.split("-");

        if(voiceParams.length != 3) return false;

        File root = new File(getDataPath() + "cg/");
        //if (DBG) Log.v(LOG_TAG, "root: " + root);
        File voiceFile = new File(root, "/" + voiceParams[0] + "/" +
                voiceParams[1] + "/" +
                voiceParams[2] + ".cg.flitevox");
        //Log.v(LOG_TAG, "doesVoiceExist: " + voiceFile);
        return voiceFile.exists();
    }

    /* Returns a list of Voices that have been copied */
    public static ArrayList<Voice> getCopiedVoiceList() {
        ArrayList<Voice> copiedVoices = new ArrayList<>();
        File root = new File(getDataPath() + "cg/");
        //if (DBG) Log.v(LOG_TAG, "root: " + root);
        File[] langDirs = root.listFiles();
        if (langDirs != null) {
            for (File lang : langDirs) {
                if (lang.isDirectory()) {

                    String country = "IND";
                    File voxDir = new File(lang, "/" + country + "/");
                    File[] voices = voxDir.listFiles();

                    if (voices == null) {
                        country = "USA";
                        voxDir = new File(lang, "/" + country + "/");
                        voices = voxDir.listFiles();
                    }

                    if (voices == null) continue;

                    for (File flitevox : voices) {
                        String flitevoxName = flitevox.getName();
                        //if (DBG) Log.v(LOG_TAG, "getCopiedVoiceList: Checking filename: " + flitevoxName);

                        if (flitevoxName.contains(".flitevox")) {
                            String variant = flitevoxName.split("\\.")[0];
                            //Log.v(LOG_TAG, "getCopiedVoiceList: Adding variant: " + lang.getName() + "-" + "IND" + "-" + variant);

                            copiedVoices.add(new Voice(lang.getName() + "-" + country
                                    + "-" + variant));
                        }
                    }
                    //if (!variantVoices.isEmpty()) copiedVoices.put(lang.getName(), variantVoices);
                }
            }
        }
        return copiedVoices;
    }

}
