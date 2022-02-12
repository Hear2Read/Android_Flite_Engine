/*package org.hear2read.indic.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static org.hear2read.indic.Startup.mDefaultSharedPrefFile;
import static org.hear2read.indic.Startup.mVoicesSharedPrefFile;*/

/**
 * Listens for newly installed, uninstalled or updated voice apps, then copies/deletes files
 * accordingly
 * Most of the functionality is now deprecated past Android 26 since we don't get these intents
 * Created by shyam on 14/12/17.
 * removed 20190412
 */
/*

public class VoiceUpdateReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        String newPackage = intent.getDataString();

        if ((newPackage == null) || (intentAction == null)) return;

        newPackage = newPackage.split(":")[1];
        String searchString = "hear2read";
        //Log.v("Bradcast","got bradcast: " + intentAction);
        switch (intentAction) {
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REPLACED:
            case Intent.ACTION_PACKAGE_CHANGED:
                //Log.v("Bradcast:", newPackage);
                if (newPackage != null && newPackage.contains(searchString)) {
                    SharedPreferences sharedPreferences[] =
                            {context.getSharedPreferences(mDefaultSharedPrefFile, 0),
                                    context.getSharedPreferences(mVoicesSharedPrefFile, 0)};

                    VoiceAddTask voiceAddTask = new VoiceAddTask(context.getApplicationContext(),
                            new VoiceAddTask.VoiceAddAsyncResponse() {
                        @Override
                        public void voiceAddFinished(String voxName) {

                        }
                    });
                    voiceAddTask.execute(newPackage);
                }
                break;
            case Intent.ACTION_PACKAGE_REMOVED:
                if (newPackage != null && newPackage.contains(searchString)) {
                    SharedPreferences sharedPreferences[] =
                            {context.getSharedPreferences(mDefaultSharedPrefFile, 0),
                                    context.getSharedPreferences(mVoicesSharedPrefFile, 0)};

                    VoiceRemoveTask voiceremoveTask = new VoiceRemoveTask(
                            context.getApplicationContext(),
                            new VoiceRemoveTask.VoiceRemoveAsyncResponse() {
                        @Override
                        public void voiceRemoveFinished(String voxName) {

                        }
                    });
                    voiceremoveTask.execute(newPackage);
                }
                break;
        }
    }

}
*/
