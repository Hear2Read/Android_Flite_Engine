/*************************************************************************/
/*                                                                       */
/*                  Language Technologies Institute                      */
/*                     Carnegie Mellon University                        */
/*                         Copyright (c) 2012                            */
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
/*               Date:  July 2012                                        */
/*************************************************************************/
package org.hear2read.indic.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
//import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
//import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;

//import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.BuildConfig;
import org.hear2read.indic.R;
import org.hear2read.indic.tts.Voice;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static org.hear2read.indic.Startup.getLog;
import static org.hear2read.indic.Startup.mPackagesSharedPrefFile;
import static org.hear2read.indic.ui.EmailComposeActivity.ACTIVITY_DETAILS_TAG;
import static org.hear2read.indic.util.Utility.sendEmail;

/* Greatly modified original home activity of the flite app. Now serves as the demo page for
* each voice */
public class TTSDemoActivity extends AppCompatActivity implements OnClickListener, OnInitListener {
    private final static String LOG_TAG = "Flite_Java_" + TTSDemoActivity.class.getSimpleName();
    private final static String TTS_INIT = "init";
    private static final String USER_TEXT = "user_text";
    private static final String USER_TEXTS = "user_text_array";
    private static final String HIGHLIGHT_INDEX = "hilite_index_array";

    private EditText mUserText;
    private String mDefaultText;
    private SpannableStringBuilder mLanguageSpannable = new SpannableStringBuilder();
    private String mLanguageText;
    private ArrayList<String> mTexts = new ArrayList<>();
    private ArrayList<Integer> mHighlightSpans = new ArrayList<>();
    private ImageButton mPlayPauseButton;
    private ImageButton mClearResetButton;
    private ImageButton mPasteButton;
    private Context mContext;
    private PopupWindow mPopupWindow = null;
    private ProgressDialog mInitProgressDialog;

    private ArrayList<Voice> mVoices;
    private Voice mVoice;
    private TextToSpeech mTts;
    private int mSelectedVoice;

    private int mUtterancePosition = 0; //current highlighted sentence/sentence being played
    private int mCharIndex = 0;

    private boolean isPlaying = false; //denotes whether speech is supposed to be playing or not
                                       //used to determine visual state of pause/play button
    private boolean isCleared;

    private boolean enableButtons = false;

    private String mVoiceName;
    private String mPackagename;
    private String mISO3;

    /* Handler and runnable to deal with Talkback interrupting speech */
    private Handler talkbackHandler = new Handler();

    /* This is only run after speech from our app has been stopped */
    private Runnable talkbackRunnable = new Runnable() {
        @Override
        public void run() {
            /* Repost delayed if Talkback still engaged and speech was interrupted */
            if (mTts.isSpeaking() && isPlaying)
                talkbackHandler.postDelayed(talkbackRunnable, 1500);
            else {
                /* Else, resume speech from last sentence, and remove any delayed runnables */
                if (isPlaying) {
                    //Log.v(LOG_TAG, "talkbackRunnable: resuming speech at: "
                    //        + mUtterancePosition);
                    sayText(mUtterancePosition, TextToSpeech.QUEUE_FLUSH);
                }

                /* Remove any delayed runnable */
                talkbackHandler.removeCallbacksAndMessages(null);
            }
        }
    };

    /* Handler and runnable to wait for engine init */
    private Handler initHandler = new Handler();

    /* Run after mInitProgressDialog shown */
    private Runnable initRunnable = new Runnable() {
        @Override
        public void run() {
            if (mInitProgressDialog!=null) {
                /* Repost delayed if Talkback still engaged and dialog showing */
                if (mTts.isSpeaking() && mInitProgressDialog.isShowing())
                    initHandler.postDelayed(initRunnable, 250);
                else {
                    /* Else, dismiss dialog */
                    if (mInitProgressDialog.isShowing()) {
                        //Log.v(LOG_TAG, "initRunnable: dismissing dialog");
                        mInitProgressDialog.dismiss();
                    }

                    /* Remove any delayed runnable */
                    initHandler.removeCallbacksAndMessages(null);
                }
            }
            else initHandler.removeCallbacksAndMessages(null);

        }
    };

    @TargetApi(17)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mContext = this;
        //mVoiceMap = new HashMap<>();

        // Get voice to be used for the demo page
        boolean voiceNameNull = true;
        mVoiceName = intent.getStringExtra("VoiceName");

        if (mVoiceName != null) {
            mVoiceName = mVoiceName.replace("[", "")
                    .replace("]", "");
            voiceNameNull = false;
        }
        else mVoiceName = "";

        SharedPreferences packagePreferences;
        packagePreferences = this.getSharedPreferences(mPackagesSharedPrefFile, 0);
        mPackagename = packagePreferences.getString(mVoiceName, "");



        //ArrayList<Voice> allVoices = getCopiedVoices();
        //Log.v(LOG_TAG, "onCreate: got VoiceName from Intent: " + mVoiceName);
        mVoice = new Voice(mVoiceName);
/*
        if (voice.isValid()) {
            mISO3 = voice.getLocale().getISO3Language();
            //Log.v(LOG_TAG, "onCreate: adding to VoiceMap: " + mISO3 + ", " +
            //        voice.getDisplayName());
            mVoiceMap.put(mISO3, voice);
        }
*/

        if (!mVoice.isValid() || voiceNameNull/*mVoiceMap.isEmpty()*/) {
            // We can't demo anything if there are no voices installed.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Hear2Read voices not installed. Please add voices in order to run the demo");
            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            mISO3 = mVoice.getLocale().getISO3Language();
            // Initialize the TTS
            //Log.v(LOG_TAG, "onCreate: Attempting to create new TextToSpeech instance org.hear2read.indic");
            mTts = new TextToSpeech(getApplicationContext(), this, "org.hear2read.indic");
            mSelectedVoice = -1;

        }
    }


    @Override
    public void onDestroy() {
        hideInitProgressDialog();
        super.onDestroy();
        if (mTts != null)
            mTts.shutdown();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTts != null) mTts.stop();
        isPlaying = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        togglePausePlay();
    }

    /* The UI portion is nested in the TTS initialization function */
    private void buildUI() {

		/* Initialize engine */
        //Log.v(LOG_TAG, "Engine init");

        /* Show progress dialog */
        showInitProgressDialog();

        setContentView(R.layout.activity_tts_demo);
        enableButtons = true;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setNavigationIcon(R.drawable.hear2read_logo);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //mDefaultText = (getString(R.string.hin_demo));
        mDefaultText = getDemoText(mISO3);

        mLanguageText = mDefaultText;
        splitText();

        mLanguageSpannable.append(mLanguageText);

        ImageButton stopButton;
        ImageButton previousButton;
        ImageButton nextButton;
        TextView copyrightText;



        mUserText = (EditText) findViewById(R.id.text_demo);
        mPlayPauseButton = (ImageButton) findViewById(R.id.button_play_pause);
        stopButton = (ImageButton) findViewById(R.id.button_stop);
        mClearResetButton = (ImageButton) findViewById(R.id.button_clear_reset);
        mPasteButton = (ImageButton) findViewById(R.id.button_paste);
        previousButton = (ImageButton) findViewById(R.id.button_previous);
        nextButton = (ImageButton) findViewById(R.id.button_next);
        copyrightText = (TextView) findViewById(R.id.test_copyright_language);

        copyrightText.setText(getCopyrightText(mISO3));

        mUserText.setSelection(mUserText.getText().length());
        mUserText.setText(mLanguageSpannable);
        mUserText.setOnClickListener(null);
        mUserText.setOnKeyListener(null);






        //12172020 sushanta
        // as per shyam email https://stackoverflow.com/questions/10627137/how-can-i-know-when-an-edittext-loses-focus/10627231#10627231
        // added : setOnFocusChangeListener

//        mUserText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//             public void onFocusChange(View v, boolean hasFocus) {
//                if (!hasFocus) {
//
//                    EditText textBox = (EditText)v;
//                    String text = textBox.getText().toString();
//
//                    // code to execute when EditText loses focus
//                    //String text = v.toString();
//
//                    isCleared = text.isEmpty();
//                    toggleClearReset();
//
//                    if (!(Objects.equals(mLanguageText, text))) {
//                        //Log.v(LOG_TAG, "Original Text in textbox: " + mLanguageText);
//                        //Log.v(LOG_TAG, "Change in textbox to: " + text + ", " + text.isEmpty());
//
//                        if (text.length() >= 1024) {
//                            AlertDialog.Builder builder =
//                                    new AlertDialog.Builder(TTSDemoActivity.this);
//                            View alertTextLayout = getLayoutInflater().inflate(R.layout.alert_demo,
//                                    null);
//                        /*TextView demoAlertTextView = alertTextLayout.findViewById(
//                                R.id.alert_text_demo);*/
//                            builder.setView(alertTextLayout);
//                            builder.setTitle("WARNING");
//                            //builder.setMessage(R.string.demo_popup);
//                            builder.setPositiveButton(android.R.string.ok, null);
//                            builder.show();
//                        }
//
//                        mLanguageText = text;
//                        splitText();
//                        stopText(); //add comment sushanta
//                    }
//
//                }
//            }
//        });







        /* Listens to changes in the EditText box to update variables - shyam */

        mUserText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(LOG_TAG, "beforetextchanged:\n" + "\n" + start +
                        ", " + count + ", " + after);


            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String text = s.toString();

                isCleared = text.isEmpty();
                toggleClearReset();

                if (!(Objects.equals(mLanguageText, text))) {
                    //Log.v(LOG_TAG, "Original Text in textbox: " + mLanguageText);
                    //Log.v(LOG_TAG, "Change in textbox to: " + text + ", " + text.isEmpty());

                    if (text.length() >= 1024) {
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(TTSDemoActivity.this);
                        View alertTextLayout = getLayoutInflater().inflate(R.layout.alert_demo,
                                null);
                        /*TextView demoAlertTextView = alertTextLayout.findViewById(
                                R.id.alert_text_demo);*/
                        builder.setView(alertTextLayout);
                        builder.setTitle("WARNING");
                        //builder.setMessage(R.string.demo_popup);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                    }

                    mLanguageText = text;
                    splitText();
                    //stopText(); //add comment sushanta
                }

            }
        });


       mPlayPauseButton.setOnClickListener(this);



        stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopText();
            }
        });

        mClearResetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetText();
            }
        });

        mPasteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pasteText();
            }
        });

        previousButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sayPrevious(mUtterancePosition);
            }
        });

        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sayNext(mUtterancePosition);
            }
        });

        highlightText(mUtterancePosition);
    }

    /* Show initialization dialog - shyam */
    private void showInitProgressDialog() {
        if (mInitProgressDialog == null) {
            mInitProgressDialog = new ProgressDialog(TTSDemoActivity.this);
            mInitProgressDialog.setCancelable(true);
            mInitProgressDialog.setCanceledOnTouchOutside(false);
            mInitProgressDialog.setMessage("TTS starting up");
            mInitProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mInitProgressDialog.setOnCancelListener(
                    new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });
        }
        mInitProgressDialog.show();
        initHandler.postDelayed(initRunnable, 500);
    }

    /* Hide dialog - shyam */
    private void hideInitProgressDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mInitProgressDialog != null && mInitProgressDialog.isShowing())
                    mInitProgressDialog.dismiss();
            }
        });
    }

    /* Get demo text to fill Edittext box - shyam */
    private String getDemoText(String iso3) {
        switch (iso3) {
            case "asm" :
                return getString(R.string.asm_demo);
            case "guj" :
                return getString(R.string.guj_demo);
            case "hin" :
                return getString(R.string.hin_demo);
            case "kan" :
                return getString(R.string.kan_demo);
            case "mal" :
                return getString(R.string.mal_demo);
            case "mar" :
                return getString(R.string.mar_demo);
            case "pan" :
                return getString(R.string.pan_demo);
            case "san" :
                return getString(R.string.san_demo);
            case "tam" :
                return getString(R.string.tam_demo);
            case "tel" :
                return getString(R.string.tel_demo);
            case "ori":
                return getString(R.string.ori_demo);
            default:
                return "";
        }
    }

    /* Get copyright texts to be displayed in the bottom - shyam */
    private String getCopyrightText(String iso3) {
        switch (iso3) {
            case "san" :
                return getString(R.string.san_copyright);
            case "tam" :
                return getString(R.string.tam_copyright);
            case "tel" :
            case "kan" :
            case "mal" :
            case "mar" :
            case "pan" :
            default:
                return "";
        }
    }

    /* onClick of the Play button - shyam */
    @Override
    public void onClick(View v) {

        if ((mTexts.isEmpty())||(!Objects.equals(mLanguageText, mLanguageSpannable.toString()))) {
            mLanguageSpannable.clear();
            mLanguageSpannable.append(mLanguageText);
            splitText();
        }

        if (isCleared || mLanguageText.isEmpty()) {
            isPlaying = false;
            togglePausePlay();
        }
        else if (isPlaying) {
            isPlaying = false;
            mTts.stop();
            togglePausePlay();
        }
        else {
            isPlaying = true;
            togglePausePlay();
            sayText(mUtterancePosition, TextToSpeech.QUEUE_FLUSH);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_demo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
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
                    PackageInfo pinfo = null;
                    try {
                        // Get phone details to be sent along with feedback
                        pinfo = getPackageManager().getPackageInfo(mPackagename, 0);
                        int verCode = pinfo.versionCode;
                        String verName = pinfo.versionName;
                        String activityDetails = mVoiceName + ", v" + verCode;

                        Intent intent = new Intent(mContext, EmailComposeActivity.class);
                        intent.putExtra(ACTIVITY_DETAILS_TAG, activityDetails);
                        mContext.startActivity(intent);

                    } catch (PackageManager.NameNotFoundException e) {
                        // In some phones, the packagemanager returns no details, this is a fallback
                        e.printStackTrace();
                        Intent intent = new Intent(mContext, EmailComposeActivity.class);
                        intent.putExtra(ACTIVITY_DETAILS_TAG, mPackagename + "(" + mISO3 + ")");
                        mContext.startActivity(intent);
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
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
    /* Initialize text related objects */
    private boolean splitText() {

        //Crashlytics.setString(USER_TEXT, mLanguageText);

        if (mLanguageText.isEmpty())
            return false;

        mTexts.clear();
        mHighlightSpans.clear();
        // A simple split according to some common EOS punctuations
        //02012021 String [] textSplit = mLanguageText.split("(?<=[?!.\\r\\n।|])", 0);
        String [] textSplit = mLanguageText.split("([?!\\r\\n।|]|((?<!\\d)\\.(?!\\d))|((?<=\\d)\\.(?=\\D)))", 0);

        int i = 0;
        mHighlightSpans.add(0, 0);
        for (String sentence : textSplit) {
            // skip empty sentences, or sentences with just sentence end chars (. ! ?)
            if (sentence.trim().isEmpty() || (sentence.trim().length() == 1))
                continue;
            //Log.v(LOG_TAG, "splitText adding: " + sentence);

            if (i > 0) {
                //Log.v(LOG_TAG, "splitText adding highlightspan: " +
                //        i +") " + (mLanguageText.indexOf(sentence, mHighlightSpans.get(i - 1)) + 1));
                mHighlightSpans.add(i,
                        mLanguageText.indexOf(sentence, mHighlightSpans.get(i - 1) + 1));
            }
            mTexts.add(sentence);
            i++;
        }
        if (i>0)
            mHighlightSpans.add(i, mLanguageText.length() + 1);
        Log.d(LOG_TAG, "mHighlightSpans: " + mHighlightSpans);
        Log.d(LOG_TAG, "mLanguageText: " + mLanguageText);
        Log.d(LOG_TAG, "mLanguageText length: " + mLanguageText.length());
        //Log.d(LOG_TAG, "mTexts:" + Arrays.toString(mTexts));
        //Crashlytics.setString(USER_TEXTS, Arrays.toString(mTexts));

        //commented follwing 1106
        //Crashlytics.setString(USER_TEXTS, String.valueOf(mTexts));
        //Crashlytics.setString(HIGHLIGHT_INDEX, String.valueOf(mHighlightSpans));
        Log.d(LOG_TAG, "mTexts: " + mTexts);
        return true;
    }

    private void pasteText() {
        stopText();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String pasteData = "";

        // If it does contain data, decide if you can handle the data.
        if (!(clipboard.hasPrimaryClip())) {

        } else {
            ClipData clipData = clipboard.getPrimaryClip();
            ClipDescription clipDescription = clipData.getDescription();
            if (!(clipDescription.hasMimeType(android.content.ClipDescription.
                            MIMETYPE_TEXT_PLAIN) ||
                    clipDescription.hasMimeType(ClipDescription.
                            MIMETYPE_TEXT_HTML) ||
                    clipDescription.hasMimeType(ClipDescription.
                            MIMETYPE_TEXT_URILIST) ||
                    clipDescription.hasMimeType(ClipDescription.
                            MIMETYPE_TEXT_INTENT)
            )) {

                // since the clipboard has data but it is not plain text

            } else {

                //since the clipboard contains plain text.
                ClipData.Item item = clipData.getItemAt(0);

                // Gets the clipboard as text.
                pasteData = item.getText().toString();
                Log.v(LOG_TAG, "Got text: " + pasteData);
            }
        }

        mLanguageText = pasteData;
        splitText();
        mLanguageSpannable.clearSpans();
        mLanguageSpannable.clear();
        mLanguageSpannable.append(mLanguageText);

        mUserText.setText(mLanguageSpannable);
    }


    /* Reset/Clear text on button click */
    private void resetText() {

        /* Stop playback */
        stopText();

        if (isCleared) {
            /* Reset Spannable text */
            //Log.v(LOG_TAG, "resetText isCleared");
            mLanguageText = mDefaultText;
            splitText();
            mLanguageSpannable.clearSpans();
            mLanguageSpannable.clear();
            mLanguageSpannable.append(mLanguageText);

            mUserText.setText(mLanguageSpannable);

        }
        else {
            //Log.v(LOG_TAG, "resetText isCleared not");
            mUserText.setText("");
            //mTexts = null;
            mTexts.clear();

        }
    }

    /* onClick for the stop button */
    private void stopText() {

        mTts.stop();

        /* Reset indices */
        mUtterancePosition = 0;
        mCharIndex = 0;

        //mTexts = null;
        isPlaying = false;
        togglePausePlay();

        if (mLanguageText.isEmpty()) return;

        mLanguageSpannable.clear();
        mLanguageSpannable.append(mLanguageText);

        //mLanguageSpannable.clearSpans();

        highlightText(mUtterancePosition);
    }

    /* Function sending text to TTS */
    private void sayText(int utteranceID, int queueMode) {
        /* Ensure textbox isn't empty */
		if (mUserText.getText().toString().isEmpty()) {
            togglePausePlay();
            return;
        }

        /* Check for out-of-bounds */
        if (((utteranceID > (mTexts.size() - 1)))// && (utteranceID == mUtterancePosition))
                ||
                (utteranceID < 0))
        {
            stopText();
            return;
        }

        Log.d(LOG_TAG, "saytext: " + utteranceID + ", \"" + mTexts.get(utteranceID) +"\"");

        /* Set Voice - redundant right now */
        //int currentVoiceID = 0;
        //mSelectedVoice = 0;
        //Voice v = mVoices.get(mSelectedVoice);
        //Voice v = mVoiceMap.get(mISO3);
        //mTts.setLanguage(v.getLocale());

		/*int currentRate = mRateSpinner.getSelectedItemPosition();
		mTts.setSpeechRate((float)(currentRate + 1)/3);*/

		/* Set utterance ID parameter for SDK < 21, not supported anymore*/
        //HashMap<String, String> map = new HashMap<>();
        //map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(utteranceID));
        //Log.v(LOG_TAG, "Adding: " + utteranceID + ") " + mTexts[utteranceID]);

        /* Use bundle params for SDK >= 21 (not supported anymore) */
        String utteranceIDString = String.valueOf(utteranceID);
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceIDString);

        /* Speak text */
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mTts.speak(mTexts.get(utteranceID), queueMode, params, utteranceIDString);
        //} else {
        //    mTts.speak(mTexts.get(utteranceID), queueMode, map);
        //}

    }

    /* Speaks next sentence on button click */
    private void sayNext(int utteranceID) {

        /* Stop current audio */
        mTts.stop();

        if (mTexts.isEmpty()) return;

        /* Disable for last sentence */
        if  (utteranceID >= (mTexts.size() - 1)) {
            isPlaying = false;
            togglePausePlay();
            return;
        }

/*
        if (mTexts.get(utteranceID + 1).trim().length() <= 0) {
            sayNext(utteranceID + 1);
            return;
        }
*/

        if (! isPlaying) {
            /* Toggle play/pause button*/
            isPlaying = true;
            //togglePausePlay();
        }

        sayText(utteranceID + 1, TextToSpeech.QUEUE_FLUSH);
    }

    /* Speaks previous sentence on button click */
    private void sayPrevious(int utteranceID) {

        /* Stop current audio */
        mTts.stop();

        if (mTexts.isEmpty())
            return;

        /* Disable for first sentence */
        if  (utteranceID <= 0){
            isPlaying = false;
            togglePausePlay();
            return;
        }

/*
        if (mTexts.get(utteranceID - 1).trim().length() <= 0) {
            //Log.v(LOG_TAG, "Skipping: " + (utteranceID - 1) + ", " + mTexts[utteranceID - 1]);
            sayPrevious(utteranceID - 1);
            return;
        }
*/

        if (! isPlaying) {
            /* Toggle play/pause button*/
            isPlaying = true;
            togglePausePlay();
        }

        sayText(utteranceID - 1, TextToSpeech.QUEUE_FLUSH);
    }

    /* Highlights text currently being spoken */
    private void highlightText(int utteranceID) {

        mLanguageSpannable.clearSpans();

        //Log.v(LOG_TAG,"Highlighting in text: " + mLanguageSpannable.toString());

        /* Todo: Find next span - to allow final punctuation in mTexts */
        //int nextTextIndex = mLanguageText.indexOf(mTexts.get(utteranceID));
        //int nextTextIndex = mCharIndex;

        //Log.v(LOG_TAG, "nextTextIndex: " + nextTextIndex + ", mCharIndex: " + mCharIndex);

        if (utteranceID < (mHighlightSpans.size() - 1))
        mLanguageSpannable.setSpan(
               // 01282021 :removed deprecated  >   getResources().getColor(R.color.foregroundblue)
                // new ForegroundColorSpan(getResources().getColor(R.color.foregroundblue)),

                new ForegroundColorSpan(ContextCompat.getColor(getApplicationContext(),R.color.foregroundblue)),
                mHighlightSpans.get(utteranceID),
                mHighlightSpans.get(utteranceID + 1) - 1,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        // ContextCompat.getColor(context, R.color.your_color);
        /* UI update can't be called from synthesis thread */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mUserText.setText(mLanguageSpannable);
            }
        });
    }

    /* Toggle play/pause button displayed */
    private void togglePausePlay() {
        /* Avoid nullpointer by waiting till setContentView completes */
        if (enableButtons && (mPlayPauseButton != null))
            /* UI update not possible from synthesis thread */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isPlaying) {
                        mPlayPauseButton.setImageResource(R.drawable.ic_action_pause);
                        //mPlayPauseButton.setContentDescription(getString(R.string.pause_button));
                    }
                    else {
                        mPlayPauseButton.setImageResource(R.drawable.ic_action_play);
                        //mPlayPauseButton.setContentDescription(getString(R.string.play_button));
                    }
                }
            });
    }

    /* Toggle play/pause button displayed */
    private void toggleClearReset(){
        /* Avoid nullpointer by waiting till setContentView completes */
        if (enableButtons && (mClearResetButton != null))
            /* UI update not possible from synthesis thread */
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isCleared) {
                        mClearResetButton.setImageResource(R.drawable.ic_action_reset);
                        //mClearResetButton.setContentDescription(getString(R.string.reset_button));
                    }
                    else {
                        mClearResetButton.setImageResource(R.drawable.ic_action_clear);
                        //mClearResetButton.setContentDescription(getString(R.string.reset_button));
                    }
                }
            });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.ERROR) {
            //Crashlytics.log(Log.ERROR, LOG_TAG, "onInit status error.");
            //success = false;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("TTS failed to initialize. You can e-mail the log to help debugging.");

            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();   //to finish Activity on which dialog is displayed
                }
            });

            builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });

            builder.setNeutralButton("E-mail Log", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String emailContent = "Device model: " + Build.MODEL + "\n";
                    emailContent = emailContent + "Flite version: "
                            + getString(R.string.flite_version) + "\n\n";
                    String log = getLog(getApplicationContext());
                    emailContent = emailContent + log;
                    Activity activity = (Activity) mContext;
                    sendEmail(activity,"feedback@Hear2Read.org", getString(R.string.email_subject)
                            + " (v" + BuildConfig.VERSION_NAME + ")", emailContent);
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            /* UtteranceProgressListener to notify sentence being spoken etc. */
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onBeginSynthesis(String utteranceId, int sampleRateInHz, int audioFormat, int channelCount) {
                    //Log.v(LOG_TAG, "onBeginSynthesis: " + utteranceId);
                }

                @Override
                public void onStart(String utteranceId) {
                    //if (DBG)
                    //Log.v(LOG_TAG, "onStart: " + utteranceId);

                    if(Objects.equals(utteranceId, TTS_INIT))
                        hideInitProgressDialog();

                    else {
                        try {
                            mUtterancePosition = Integer.parseInt(utteranceId);

                            /* Highlight current sentence */
                            highlightText(mUtterancePosition);

                            /* set play/pause button*/
                            if (!isPlaying) {
                                isPlaying = true;
                                togglePausePlay();
                            }

                            /* speak text */
                            if (mUtterancePosition < (mTexts.size() - 1)) {
                                sayText(mUtterancePosition + 1, TextToSpeech.QUEUE_ADD);
                            }
                        } catch (NumberFormatException e) {
                            Log.v(LOG_TAG, "catch");
                           // Crashlytics.log(Log.WARN, LOG_TAG, "onStart: invalid " +
                                   // "utteranceID: " + utteranceId);
                        }
                    }
                }

                @Override
                public void onDone(String utteranceId) {
                    //if (DBG)
                    //Log.v(LOG_TAG, "onDone: " + utteranceId);

                    try {

                        int intUtteranceID = Integer.parseInt(utteranceId);

                    /* Check if next sentence id doesn't go beyond number of sentences */
                        if ((mTexts.isEmpty()) || (intUtteranceID >= (mTexts.size() - 1)))
                            isPlaying = false;

                        if (!isPlaying) {
                            togglePausePlay();
                            return;
                        }

                    /* Update indices to next sentence */
                        mCharIndex += mTexts.get(intUtteranceID).length();
                        mUtterancePosition = intUtteranceID + 1;
                    } catch (NumberFormatException e) {
                        if (! Objects.equals(utteranceId, TTS_INIT))
                            Log.v(LOG_TAG,"if");
                            //Crashlytics.log(Log.ERROR, LOG_TAG,
                                 //   "onDone: invalid utteranceID: " + utteranceId );
                    }

                    /* Remove any runnable to resume speech on successful audio playback
                     completion */
                    talkbackHandler.removeCallbacksAndMessages(null);
                }

                @Override
                public void onStop(String utteranceId, boolean interrupted) {
                    //Log.v(LOG_TAG, "onStop: " + utteranceId);

                    if (isPlaying && interrupted) {
                        //Log.v(LOG_TAG, "onStop: not stopped by demo");

                        /* On being interrupted, post a delayed runnable that calls itself every
                         1.5s, until the TTS is no longer speaking, and then resumes playback from
                          the last sentence played */
                        talkbackHandler.removeCallbacksAndMessages(null);
                        talkbackHandler.postDelayed(talkbackRunnable, 1500);
                    }

                }

                @Override
                public void onError(String utteranceId) {
                    try {
                        int utterance = Integer.parseInt(utteranceId);
                        if ((!mTexts.isEmpty()) && (mTexts.size() >= utterance))
                            Log.v(LOG_TAG,"if");
                            //Crashlytics.log(Log.ERROR, LOG_TAG, "onError: " + utteranceId + ": " +
                              //      mTexts.get(utterance));
                        else //Crashlytics.log(Log.ERROR, LOG_TAG, "onError: " + utteranceId);
                            Log.v(LOG_TAG,"else");
                    } catch (NumberFormatException e) {
                        //Crashlytics.log(Log.WARN, LOG_TAG, "onError: utteranceID not a" +
                              //  " number: " + utteranceId);
                        Log.v(LOG_TAG,"catch");
                    }

                }

                @Override
                public void onError(String utteranceId, int i) {
                    try {
                        int utterance = Integer.parseInt(utteranceId);
                        if ((!mTexts.isEmpty()) && (mTexts.size() >= utterance))
                            Log.v(LOG_TAG,"if");
                        //Crashlytics.log(Log.ERROR, LOG_TAG, "onError " + i + " :"
                         //       + utteranceId + ": " +
                         //       mTexts.get(utterance));
                        else //Crashlytics.log(Log.ERROR, LOG_TAG, "onError: " + utteranceId);
                            Log.v(LOG_TAG,"else");
                    } catch (NumberFormatException e) {
                        //Crashlytics.log(Log.WARN, LOG_TAG, "onError: utteranceID not a" +
                         //       " number: " + utteranceId);
                        Log.v(LOG_TAG,"catch");
                    }
                }
            });

        /* Set Voice - redundant right now */
        mSelectedVoice = 0;
        //Voice v = mVoices.get(mSelectedVoice);
        //Voice v = mVoiceMap.get(mISO3);
        mTts.setLanguage(mVoice.getLocale());

        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TTS_INIT);

        /* Speak text */
        mTts.speak("", TextToSpeech.QUEUE_FLUSH, map);

        buildUI();
        }
    }

    /* Get engine init alert for talkback users
    * (unused)
    * Shyam Krishna
    * 2018/03/06 */
    private String getInitAudio(String voice) {

        String langDir;
        String soundFile;

        switch (voice) {
            case "tam":
            case "san":
                langDir = voice + File.separator + voice;
                break;
            default: langDir = "";
        }

        //Log.v(LOG_TAG,"getInitAudio: langDir: " + langDir);

        if (Objects.equals(langDir, ""))
            return "";

        langDir = langDir + "_tts_init.mp3";
        return langDir;
    }

}