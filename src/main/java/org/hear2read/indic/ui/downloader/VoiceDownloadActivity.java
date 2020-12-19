package org.hear2read.indic.ui.downloader;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.hear2read.indic.R;
import org.hear2read.indic.ui.EmailComposeActivity;
import org.hear2read.indic.ui.FliteInfoActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.hear2read.indic.Flite.DBG;
import static org.hear2read.indic.Startup.mVoicesSharedPrefFile;
import static org.hear2read.indic.ui.EmailComposeActivity.ACTIVITY_DETAILS_TAG;

/**
 * Activity containing Playstore links to available voice apps not installed on the phone
 * Created by shyam on 6/1/18.
 */

public class VoiceDownloadActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Flite_Java_" + VoiceDownloadActivity.class.getSimpleName();
    //ImageButton mDemoButton;
    private List<DownloadListItem> downloadsList = new ArrayList<>();
    private PopupWindow mPopupWindow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_voice_downloader);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        //mDemoButton = (ImageButton) findViewById(R.id.demo_button);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        RecyclerView mDownloadsListView = (RecyclerView) findViewById(R.id.list_voice_download);
        TextView mWarningText = (TextView) findViewById(R.id.text_warning_no_downloads);


        DownloadAdapter mDownloadsAdapter = new DownloadAdapter(this, downloadsList);
        //populateDownloadList();


        SharedPreferences mVoicePreferences = getSharedPreferences(mVoicesSharedPrefFile, 0);

        if (mVoicePreferences.getString("org.hear2read.Gujarati", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.Gujarati",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Gujarati",
                    "guj", "female"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }
        /* remove as 12122020 as per email Gujarati Male Voice not available
        if (mVoicePreferences.getString("org.hear2read.gujarati.male", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.gujarati.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.gujarati.male",
                    "guj", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }*/

        if (mVoicePreferences.getString("org.hear2read.kannada.female", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.kannada.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.kannada.female",
                    "kan", "female"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.Kannada", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.kannada.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Kannada",
                    "kan", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.Malayalam", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.punjabi.female",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Malayalam",
                    "mal", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }


        if (mVoicePreferences.getString("org.hear2read.Marathi", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.punjabi.female",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Marathi",
                    "mar", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.Punjabi", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.punjabi.female",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Punjabi",
                    "pan", "female"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.sanskrit.male", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.sanskrit.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.sanskrit.male",
                    "san", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.Tamil", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.tamil.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Tamil",
                    "tam", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.Telugu", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.telugu.female",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Telugu",
                    "tel", "female"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (mVoicePreferences.getString("org.hear2read.telugu.male", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.telugu.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.telugu.male",
                    "tel", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }
        //Added Oct 2020
        if (mVoicePreferences.getString("org.hear2read.Assamese", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.asm.female",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Assamese",
                    "asm", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }
        //Added Oct 2020
        if (mVoicePreferences.getString("org.hear2read.Odia", "").isEmpty()) {
            //if(DBG) Log.v("LOG", mVoicePreferences.getString("org.hear2read.ori.male",
            //        ""));
            downloadsList.add(new DownloadListItem("org.hear2read.Odia",
                    "ori", "male"));
            mDownloadsListView.setVisibility(View.VISIBLE);
            mWarningText.setVisibility(View.INVISIBLE);
        }

        if (downloadsList.isEmpty()) {
            mDownloadsListView.setVisibility(View.INVISIBLE);
            mWarningText.setVisibility(View.VISIBLE);
        } else {
            Collections.sort(downloadsList, new Comparator<DownloadListItem>() {
                @Override
                public int compare(DownloadListItem o1, DownloadListItem o2) {
                    return (o1.getName() + o1.getVariant()).compareToIgnoreCase(o2.getName() + o2.getVariant());
                }
            });
            mDownloadsAdapter.notifyDataSetChanged();
        }

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mDownloadsListView.setLayoutManager(mLayoutManager);
        mDownloadsListView.setItemAnimator(new DefaultItemAnimator());
        mDownloadsListView.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        mDownloadsListView.setAdapter(mDownloadsAdapter);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_download, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        /* Send feedback by email */
        if (i == R.id.action_contact) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Please use this to send specific questions or detailed problem" +
                    " description.\n" + "Please do not attach any images or .pdf files.");
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    Intent intent = new Intent(VoiceDownloadActivity.this, EmailComposeActivity.class);
                    intent.putExtra(ACTIVITY_DETAILS_TAG, "Download Activity");
                    VoiceDownloadActivity.this.startActivity(intent);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            return true;
        }
        /* Start About screen activity */
        else if (i == R.id.action_about) {
            Intent intent = new Intent(this, FliteInfoActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(mPopupWindow!=null){
            //dismiss the popup
            mPopupWindow.dismiss();
            //make popup null again
            mPopupWindow=null;
        }
        else super.onBackPressed();
    }

}
