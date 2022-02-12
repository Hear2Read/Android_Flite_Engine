package org.hear2read.indic.ui.manager;

import android.util.Log;

import org.hear2read.indic.tts.Voice;

import java.util.ArrayList;
import java.util.Locale;

/**
 * List item for the UI list of installed voices
 * Created by shyam on 16/12/17.
 */

class LanguageListItem {
    private static final String LOG_TAG = "Flite_Java_" + LanguageListItem.class.getSimpleName();

    private String mIcon;
    private String mName;
    private Voice mVoice;
    private String mISO3;


    public LanguageListItem(String icon, String iso3, Voice vox ) {
        this.mIcon = icon;
        this.mISO3 = iso3;
        this.mName = (new Locale(iso3)).getDisplayLanguage();
        //if(this.mName.equals("Oriya"))
           // this.mName= "Odia";
        this.mVoice = vox;

        //Log.v(LOG_TAG, "Created Languge: " + mIcon + ", " + mName + ", " + mISO3);
    }

    String getISO3() {
            return mISO3;
    }

    public void setISO3(String mISO3) {
        this.mISO3 = mISO3;
    }

    String getIcon() {
        return mIcon;
    }

    public void setIcon(String mIcon) {
        this.mIcon = mIcon;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public Voice getVoice() {
        return mVoice;
    }

    public void setVoice(Voice voice) {
        this.mVoice = voice;
    }

    String getDisplayNames() {
/*
        ArrayList<String> displayNames = new ArrayList<>();
        for (Voice vox : mVoice)
            displayNames.add(vox.getDisplayName());
        //Log.v(LOG_TAG, "getDisplayNames: " + displayNames.toString());
        return displayNames.toString();
*/
        return mVoice.getDisplayLanguage();
    }

    String getAndroidName() {
/*
        ArrayList<String> androidNames = new ArrayList<>();
        for (Voice vox : mVoice)
            androidNames.add(vox.getName());
        //Log.v(LOG_TAG, "getAndoidNames: " + androidNames.toString());
        return androidNames.toString();
*/
        return mVoice.getName();
    }

    String getVariant() {
        return mVoice.getVariant();
    }
}
