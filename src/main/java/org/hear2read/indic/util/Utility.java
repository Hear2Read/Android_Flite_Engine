/*************************************************************************/
/*                                                                       */
/*                  Language Technologies Institute                      */
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

package org.hear2read.indic.util;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.SparseArray;

import org.hear2read.indic.R;
import org.hear2read.indic.tts.Voice;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hear2read.indic.Startup.VOICE_NAMES_PREF;
import static org.hear2read.indic.Startup.VOICE_PACKAGES_PREF;

public class Utility {
    private static final String LOG_TAG = "Flite_Java_" + Utility.class.getSimpleName();

    public static final String BROADCAST_END = "org.hear2read.indic.VOICE_UPDATE_FINISHED";

    public static boolean pathExists(String pathname){
        File tempFile = new File(pathname);
        return tempFile.exists();
    }

    public static  ArrayList<String>readLines(String filename) throws IOException {
        ArrayList<String> strLines = new ArrayList<String>();
        FileInputStream fstream = new FileInputStream(filename);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in),1024);
        String strLine;
        while ((strLine = br.readLine()) != null)   {
            strLines.add(strLine);
        }
        in.close();
        return strLines;
    }

    public static void sendEmail(Activity parent, String emailTo, String emailSubject, String emailContent)
    {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",emailTo, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, emailSubject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, emailContent);
        parent.startActivity(Intent.createChooser(emailIntent, parent.getString(R.string.email_client)));
    }

    // Dim background for popup (unused after moving from popup email to email activity)
/*
    public static void applyDim(@NonNull ViewGroup parent, float dimAmount){
        Drawable dim = new ColorDrawable(Color.BLACK);
        dim.setBounds(0, 0, parent.getWidth(), parent.getHeight());
        dim.setAlpha((int) (255 * dimAmount));

        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.add(dim);
    }
*/

    // Clear background for popup (unused after moving from popup email to email activity)
/*
    public static void clearDim(@NonNull ViewGroup parent) {
        ViewGroupOverlay overlay = parent.getOverlay();
        overlay.clear();
    }
*/


    /* Returns both installed voices that haven't been updated into the engine, as well as
     uninstalled voices that haven't been removed from the engine */
    public static SparseArray<Set<String>> syncedVoicesFromPreferences
    (List<PackageInfo> packages, Set<String> installedVoices) {
        //Log.v(LOG_TAG, "syncedVoicesFromPreferences, installedVoices: " + installedVoices);
        String searchString = "hear2read";
        Set<String> missingVoices = new HashSet<>();
        List<String> hear2readPackages = new ArrayList<>();
        Set<String> uninstalledVoices = new HashSet<>();
        for(PackageInfo pkg : packages) {
            String packageName = pkg.packageName;
            //allPackages.add(packageName);
            if (packageName != null && packageName.contains(searchString) &&
                    (!Objects.equals(packageName, "org.hear2read.indic"))) {
                if (!installedVoices.contains(packageName)) {
                    //Log.v(LOG_TAG, "syncedVoicesFromPreferences, adding missingVoice: " + packageName);
                    missingVoices.add(packageName);
                }
                hear2readPackages.add(packageName);
            }
        }
        //Log.v(LOG_TAG, "hear2readPackages: " + hear2readPackages);
        for(String pkg : installedVoices) {
            if (!hear2readPackages.contains(pkg)) {
                //Log.v(LOG_TAG, "hear2readPackages does not contain: " + pkg);
                uninstalledVoices.add(pkg);
            }
        }
        SparseArray<Set<String>> packageVoiceInfo = new SparseArray<>();
        packageVoiceInfo.put(0, missingVoices);
        packageVoiceInfo.put(1, uninstalledVoices);
        //Log.v(LOG_TAG, "syncedVoicesFromPreferences, missingVoices: " + missingVoices.toString());
        //Log.v(LOG_TAG, "syncedVoicesFromPreferences, uninstalledVoices: " + uninstalledVoices.toString());
        return packageVoiceInfo;
    }

    // gets list of orphan flitevox files (voice app uninstalled)
    public static Set<String> getPersistingUninstalledVoices(Set<String> installedVoices,
                                                        ArrayList<Voice> copiedVoices) {
        //Log.v(LOG_TAG, "getPersistingUninstalledVoices: " + installedVoices + ", "
        //        + copiedVoices);
        Set<String> persistingVoices = new HashSet<>();
        for (Voice vox : copiedVoices) {
            //remove copied voices not in preferences
            //Log.v(LOG_TAG, "getPersistingUninstalledVoices checking: " + vox.getName());
            if (!installedVoices.contains(vox.getName())) {
                persistingVoices.add(vox.getName());
            }
        }
        //Log.v(LOG_TAG, "getPersistingUninstalledVoices result: " + persistingVoices);
        return persistingVoices;
    }

    // remove voice preferences of uninstalled voices
    public static void removeVoicePrefs(Set<String> pkgs,
                                        SharedPreferences defaultPreferences,
                                        SharedPreferences packagePreferences,
                                        SharedPreferences versionPreferences,
                                        SharedPreferences voicePreferences) {

        for (String pkgName : pkgs) {
            //Log.v(LOG_TAG, "removeVoicePrefs: " + pkgName);
            String voiceName = voicePreferences.getString(pkgName, "");
            SharedPreferences.Editor sharedprefeditor = voicePreferences.edit();
            sharedprefeditor.remove(pkgName);
            sharedprefeditor.commit();

            sharedprefeditor = packagePreferences.edit();
            sharedprefeditor.remove(voiceName);
            sharedprefeditor.commit();

            sharedprefeditor = versionPreferences.edit();
            sharedprefeditor.remove(pkgName);
            sharedprefeditor.commit();

            Set<String> packages = defaultPreferences.getStringSet(VOICE_PACKAGES_PREF,
                    new HashSet<String>());
            packages.remove(pkgName);
            sharedprefeditor = defaultPreferences.edit();
            sharedprefeditor.putStringSet(VOICE_PACKAGES_PREF, packages);
            sharedprefeditor.commit();

            Set<String> voices = defaultPreferences.getStringSet(VOICE_NAMES_PREF,
                    new HashSet<String>());
            voices.remove(voiceName);
            sharedprefeditor = defaultPreferences.edit();
            sharedprefeditor.putStringSet(VOICE_NAMES_PREF, voices);
            sharedprefeditor.commit();
        }

    }


}


