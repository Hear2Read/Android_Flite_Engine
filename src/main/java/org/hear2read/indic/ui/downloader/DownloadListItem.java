package org.hear2read.indic.ui.downloader;

import android.util.Log;

import java.util.Locale;

/**
 * Item in the DownloadAdapter
 * Created by shyam on 23/12/17.
 */

public class DownloadListItem {
    private static final String LOG_TAG = "Flite_Java_" + DownloadListItem.class.getSimpleName();
    String mPackageName;
    private String mName;
    private String mISO3;

    private String mVariant;


    public DownloadListItem(String packageName, String iso3, String variant) {
        this.mPackageName = packageName;
        this.mISO3 = iso3;
        this.mVariant = variant;
        this.mName = (new Locale(iso3)).getDisplayLanguage() + ", " + variant;
        //if(this.mName.equals("Oriya"))
           // this.mName= "Odia";
        //Log.v(LOG_TAG, "Created Download Item: " + mPackageName + ", " + mISO3);
    }

    String getISO3() {
        return mISO3;
    }

    public void setISO3(String mISO3) {
        this.mISO3 = mISO3;
    }

    String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        this.mPackageName = packageName;
    }

    public String getVariant() {
        return mVariant;
    }

    public void setVariant(String variant) {
        this.mVariant = variant;
    }

    public String getName() {

        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }
}
