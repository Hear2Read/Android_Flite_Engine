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
/*               Date:  June 2012                                        */
/*************************************************************************/

package org.hear2read.indic.tts;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.NotificationCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

//1106 import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.R;
import org.hear2read.indic.Startup;
//import org.hear2read.indic.util.SettingsActivity;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static org.hear2read.indic.Startup.KEY_OTHER_ENG_ENGINE;
import static org.hear2read.indic.tts.CheckVoiceData.getCopiedVoiceList;
import static org.hear2read.indic.tts.CheckVoiceData.getCopiedVoices;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENGLISH_ENGINE;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENGLISH_VOLUME;
import static org.hear2read.indic.util.SettingsActivity.KEY_ENG_RATE;
//import static org.hear2read.indic.util.SettingsActivity.KEY_OTHER_ENG_ENGINE;
//import static org.hear2read.indic.util.SettingsActivity.KEY_OTHER_ENG_SWITCH;
//import static org.hear2read.indic.util.SettingsActivity.KEY_VOLUME_NORMALIZE_SWITCH;

/**
 * Implements the Flite Engine as a TextToSpeechService
 *
 */

/*@TargetApi(17)*/
@SuppressLint("NewApi")
public class FliteTtsService extends TextToSpeechService implements TextToSpeech.OnInitListener,
        SharedPreferences.OnSharedPreferenceChangeListener  {
	public static final String FLITE_INITIALIZED = "org.hear2read.indic.FLITE_INITIALIZED";
	private final static String LOG_TAG = "Flite_Java_" + FliteTtsService.class.getSimpleName();
    private final static int MEDIA_NOT_RESPONSIVE = 1;
    private final static int MEDIA_OKAY = 2;
    private final static int MEDIA_APP_NAME = 3;
	private NativeFliteTTS mEngine;
    //private static final int QUEUE_DESTROY = 2;

	private static final String DEFAULT_LANGUAGE = "";
	private static final String DEFAULT_COUNTRY = "IND";
    private static final String DEFAULT_VARIANT = "";
    //for mediaplayer, unused:

    private static final String ENG_UTT_ID = "eng";
    private static final String ENG_AUDIO_INIT = "eng_audio_init";
/*
    private static final String TEXT_NOT_RESPONSIVE =
            "Hear2Read is not responding. It will restart and continue in a few seconds.";
    private static final String TEXT_OKAY = "Okay, Button";
    private static final String TEXT_APP_NAME = "Indic Hear2Read";
*/

	private String mCountry = DEFAULT_COUNTRY;
	private String mLanguage = DEFAULT_LANGUAGE;
	private String mVariant = DEFAULT_VARIANT;

	private ArrayList<Voice> mAvailableVoices;
	private SynthesisCallback mCallback;

    private SharedPreferences sharedPref;
    private Boolean otherEngPref = true;
    //private Boolean volNormPref = true;
    //private int mCurrentVolume = 100;
    private String defaultEnglishEngine = "";
    private String defaultEnglishVoice = "";
    private boolean otherEngInit = false;
    private int mEngVolume = 60;
    private int mEngRate = 100;
    private int mEngTtsSampleRate = 22050;
    private int mEngTtsAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int mEngTtsChannelcount = 1;
    private TextToSpeech mEngTts;
	//private boolean DBG = true;

    MediaPlayer mPlayer;

    private final Object mPauseLock = new Object();
    private boolean mPaused;

    /* Kannada low phone hack
    private String[] hackOrig = {"ಠ", "ಢ", "ಝ"};
    private String[] hackReplace = {"ಟ", "ಡ", "ಜ"}; */

    /* Handler to set delayed runnable to ensure tts engine is responding */
    private Handler killerHandler = new Handler();

    /* If this runnable executes, it means the engine is not responding. We restart the app in this
    case */
    private Runnable killerRunnable = new Runnable() {
        @Override
        public void run() {
            //1106 Crashlytics.log(Log.WARN, LOG_TAG, "Unresponsive, restarting app.");

            NotificationCompat.Builder notifyRestart =
                    new NotificationCompat.Builder(FliteTtsService.super.getApplicationContext());
            notifyRestart.setSmallIcon(R.drawable.ic_launcher_app_logo)
                    .setContentTitle("Unresponsive, restarted Hear2Read")
                    .setContentText("");

            int mNotificationId = 001;
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (mNotifyMgr != null)
                mNotifyMgr.notify(mNotificationId, notifyRestart.build());

            Runtime.getRuntime().exit(0);

        }
    };

    @Override
	public void onCreate() {
		initializeFliteEngine();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        //otherEngPref = sharedPref.getBoolean(KEY_OTHER_ENG_SWITCH, true);
        //volNormPref = sharedPref.getBoolean(KEY_VOLUME_NORMALIZE_SWITCH, true);
        String engVoice = sharedPref.getString(KEY_ENGLISH_ENGINE, "");
        if (engVoice.isEmpty())
            engVoice = sharedPref.getString(KEY_OTHER_ENG_ENGINE, "");

        defaultEnglishEngine = engVoice.split(",")[0];

        if(!defaultEnglishEngine.isEmpty()) {
            if (engVoice.split(",").length > 1)
                defaultEnglishVoice = engVoice.split(",")[1];
        }


        mEngRate = sharedPref.getInt(KEY_ENG_RATE, 100);
        mEngVolume = sharedPref.getInt(KEY_ENGLISH_VOLUME, 60);

		if (/*otherEngPref && */!defaultEnglishEngine.isEmpty()) initializeEnglish();

        //mPauseLock = new Object();
        mPaused = false;

		//if (mPlayer == null)
        //    mPlayer = new MediaPlayer();

		// This calls onIsLanguageAvailable() and must run after Initialization
		super.onCreate();
	}

    private void initializeEnglish() {
        Log.d(LOG_TAG, "initializeEnglish: " + defaultEnglishEngine);
        mEngTtsSampleRate = 22050;
        String [] defaultEnglishParams = defaultEnglishEngine.split(",");
        if (defaultEnglishParams.length > 1) {
            defaultEnglishEngine = defaultEnglishParams [0];
            defaultEnglishVoice = defaultEnglishParams [1];
        }
        mEngTts = new TextToSpeech(getApplicationContext(), this, defaultEnglishEngine);
    }

    private void initializeFliteEngine() {
		if (mEngine != null) {
			mEngine.stop();
			mEngine = null;
		}
		mEngine = new NativeFliteTTS(this, mSynthCallback);

		//Map<String, ArrayList<Voice>> languageList = getCopiedVoiceList();

        ArrayList<Voice> allVoices = getCopiedVoices();

        mAvailableVoices = new ArrayList<Voice>();
        boolean result = false;
        for(Voice vox:allVoices) {
            if (vox.isAvailable()) {
                mAvailableVoices.add(vox);
                //Log.d(LOG_TAG, "onCreate: Added voice: " + vox.getName());
/*
                if (!result) {
                    onLoadLanguage(vox.getLocale().getISO3Language(), DEFAULT_COUNTRY,
                            DEFAULT_VARIANT);
                    result = mEngine.setLanguage(vox.getLocale().getISO3Language(),
                            vox.getLocale().getISO3Country(),
                            vox.getLocale().getVariant());
                    if (result) mLanguage = vox.getLocale().getISO3Language();
                }
*/
            }
        }

        final Intent intent = new Intent(FLITE_INITIALIZED);
        sendBroadcast(intent);
	}


    @Override
    public void onInit(int status) {
        //Log.v(LOG_TAG, "onInit: " + defaultEnglishEngine + defaultEnglishVoice);

        if (status == TextToSpeech.ERROR) {
           //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "onInit status error.");
            Log.v(LOG_TAG, "if");
        }
        else {
            //Log.v(LOG_TAG, "onInit success");
            //int setLangEng = mEngTts.setLanguage(new Locale("eng"));
            Locale engLocale;
            if (!defaultEnglishVoice.isEmpty()) {
                String [] engLocaleArgs = defaultEnglishVoice.split("-");
                if (engLocaleArgs.length >= 3)
                    engLocale = new Locale("en", engLocaleArgs[1], engLocaleArgs[2]);
                else if (engLocaleArgs.length == 2)
                    engLocale = new Locale("en", engLocaleArgs[1]);
                else
                    engLocale = new Locale("en");
            } else {
                //setDefaultEnglishVoice();
                //engLocale = new Locale("eng");
                engLocale = new Locale("en");
            }
            //Log.v(LOG_TAG, "onInit, engLocale: " + engLocale);
            if (engLocale == null)
                engLocale = Locale.ENGLISH;
            try {
                engLocale.getLanguage();
            }
            catch (NullPointerException e) {
                return;
            }
            int setLangEng = mEngTts.setLanguage(engLocale);
            //Log.v(LOG_TAG, "onInit, setLangEng: " + setLangEng);
            if (setLangEng >= TextToSpeech.LANG_AVAILABLE) {
                otherEngInit = true;
                //if (defaultEnglishVoice.isEmpty())
                //    setDefaultEnglishVoice();
                mEngTts.setOnUtteranceProgressListener(mEngCallback);
                engAudioParamInit();
            }
        }
        resumeEngSynth();
    }

    // Initialize English TTS audio parameters
    private void engAudioParamInit() {
        Bundle paramBundle = new Bundle();
        paramBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ENG_AUDIO_INIT);
        paramBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0f);
        engSpeak("Initialize English", TextToSpeech.QUEUE_FLUSH, paramBundle, ENG_AUDIO_INIT);
    }

    private void startCallback(int sampleRateInHz, int audioFormat, int channelCount) {
        if(mCallback != null) {
            //Log.v(LOG_TAG, "startCallback");
            mCallback.start(sampleRateInHz, audioFormat, channelCount);
        }
    }

    private void finishCallback() {
        if(mCallback != null){
            mCallback.done();
            //File engwav = new File(Startup.getDataPath() + "eng.wav");
            //engwav.delete();
        }
        resumeEngSynth();
    }


    @Override
	protected String[] onGetLanguage() {
		//Log.v(LOG_TAG, "onGetLanguage returning: " + mLanguage + mCountry + mVariant);
		return new String[] {
				mLanguage, mCountry, mVariant
		};
	}

	@Override
	protected int onIsLanguageAvailable(String language, String country, String variant) {
		//Log.v(LOG_TAG, "onIsLanguageAvailable: " + language + country + variant);
		return mEngine.isLanguageAvailable(language, country, variant);

	}

	@Override
	protected int onLoadLanguage(String language, String country, String variant) {
		//Log.v(LOG_TAG, "onLoadLanguage: " + language + country + variant);
		return mEngine.isLanguageAvailable(language, country, variant);
	}

    @Override
    protected Set<String> onGetFeaturesForLanguage(String lang, String country, String variant) {
        return new HashSet<>();
    }

    @Override
    public String onGetDefaultVoiceNameFor(String language, String country, String variant) {
        //Log.v(LOG_TAG, "onGetDefaultVoiceNameFor: " + language + country + variant);
        int result = mEngine.isLanguageAvailable(language, country, variant);
        if (result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            return language + "-" + country + "-" + variant;
        }

        if(result >=TextToSpeech.LANG_AVAILABLE) {
            //Log.v(LOG_TAG, "onGetDefaultVoiceNameFor: finding alternatives :" + language
            // + country + variant + ". Current: " +mLanguage+mCountry+mVariant);
            Locale defaultLocale = parseEngineLocalePrefFromList(
                    Settings.Secure.getString(getContentResolver(), "tts_default_locale"),
                    getPackageName());

            /*
            *  Check if there's a user preferred variant to the language
            *  - we do this because Talkback is currently doesn't parse default Locale well
            * */
            if ((defaultLocale!=null) && Objects.equals(defaultLocale.getISO3Language(), language))
            {
                result = mEngine.isLanguageAvailable(defaultLocale.getISO3Language(),
                        defaultLocale.getISO3Country(),
                        defaultLocale.getVariant());
                if (result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    return defaultLocale.getISO3Language() + "-"
                            + defaultLocale.getISO3Country() + "-"
                            + defaultLocale.getVariant();
                }

            }

            /*
            *  Else return a random available variant
            * */
            for (Voice vox : getCopiedVoiceList()){
                if (Objects.equals(vox.getName().split("-")[0], language))
                    return vox.getName();
            }
        }
        //Log.v(LOG_TAG, "onGetDefaultVoiceNameFor: " + language + country +
        //        variant + "is null");
        return null;
    }

    /**
     * Taken from the Talkback project, but improved on the returned locale
     *
     * Parses a comma separated list of engine locale preferences. The list is of the form {@code
     * "engine_name_1:locale_1,engine_name_2:locale2"} and so on and so forth. Returns null if the
     * list is empty, malformed or if there is no engine specific preference in the list.
     */
    public static Locale parseEngineLocalePrefFromList(String prefValue, String engineName) {
        if (TextUtils.isEmpty(prefValue)) {
            return null;
        }

        //Log.v(LOG_TAG, "parseEngineLocalePrefFromList on: " + prefValue);

        final String[] prefValues = prefValue.split(",");
        for (String value : prefValues) {
            final int delimiter = value.indexOf(':');
            if (delimiter > 0) {
                if (engineName.equals(value.substring(0, delimiter))) {
                    // We check for more than just language, unlike Talkback
                    String[] localeSplit = value.substring(delimiter + 1).split("_");
                    if (localeSplit.length > 2)
                        return new Locale(localeSplit[0], localeSplit[1], localeSplit[2]);
                    if (localeSplit.length == 2)
                        return new Locale(localeSplit[0], localeSplit[1]);
                    if (localeSplit.length == 1)
                        return new Locale(localeSplit[0]);
                }
            }
        }

        return null;
    }

    @Override
    public List<android.speech.tts.Voice> onGetVoices() {
        List<android.speech.tts.Voice> voices = new ArrayList<>();
        for (Voice vox : mAvailableVoices) {
            int quality = android.speech.tts.Voice.QUALITY_HIGH;
            int latency = android.speech.tts.Voice.LATENCY_NORMAL;
            //Locale locale = new Locale(vox.getLocale().getISO3Language(),
            //                        vox.getLocale().getISO3Country(), vox.getLocale().getVariant());
            Locale locale = vox.getLocale();
            Set<String> features = onGetFeaturesForLanguage(locale.getLanguage(),
                                                    locale.getCountry(), locale.getVariant());
            voices.add(new android.speech.tts.Voice(vox.getName(),
                    vox.getLocale(),
                    quality,
                    latency,
                    false, features));
        }
        return voices;
    }

    @Override
	public int onIsValidVoiceName(String name) {
        //Log.v(LOG_TAG, "onIsValidVoiceName");
        mAvailableVoices = getCopiedVoices();
		for (Voice vox : mAvailableVoices) {
			if (Objects.equals(name, vox.getName()))
				return TextToSpeech.SUCCESS;
		}
        //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "onIsValidVoiceName error: " + name);
		return TextToSpeech.ERROR;
	}

    @Override
    public int onLoadVoice(String name) {
        mAvailableVoices = getCopiedVoices();
        for (Voice vox : mAvailableVoices) {
            //Log.v(LOG_TAG, "Checking for: " + name + " vs: " + vox.getName());

            if (Objects.equals(name, vox.getName()))
                return TextToSpeech.SUCCESS;
        }
        //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "onLoadVoice error: " + name);
        return TextToSpeech.ERROR;
    }

    @Override
	protected void onStop() {
		//Log.v(LOG_TAG, "onStop");

        //sharedPref.unregisterOnSharedPreferenceChangeListener(this);

        if (mEngTts != null) {
            mEngTts.stop();
        }

        if (mEngine == null) return;
		mEngine.stop();
	}

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    /* After Lollipop, the request text is a CharSequence, this is to convert it to String */
    @SuppressWarnings("deprecation")
    private String getRequestString(SynthesisRequest request) {
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return request.getCharSequenceText().toString();
        //} else {
        //    return request.getText();
        //}
    }

	@Override
	protected synchronized void onSynthesizeText(
			SynthesisRequest request, SynthesisCallback callback) {
		//Log.v(LOG_TAG, "onSynthesize");

		String language = request.getLanguage();
		String country = request.getCountry();
		String variant = request.getVariant();
		String text = getRequestString(request);
		Integer speechrate = request.getSpeechRate();
        Bundle params = request.getParams();
        params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");


        boolean result = true;
		/*if(variant == null || variant.length() == 0)
			variant = "male";*/

        //Log.v(LOG_TAG, "onSynth: otherEngPref, otherEngInit, defaultEnglishEngine: "
        //        + otherEngPref + ", " + otherEngInit + ", " + defaultEnglishEngine);
        if(/*otherEngPref &&*/ !otherEngInit && !defaultEnglishEngine.isEmpty()) {
            pauseEngSynth();
            synchronized (mPauseLock) {
                initializeEnglish();
                while (mPaused) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        //1106 Crashlytics.log(Log.ERROR, LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        if (/*otherEngPref &&*/ otherEngInit && isTextEnglish(text))
        {
            //Log.v(LOG_TAG, "Speaking eng: " + text);
            //if (defaultEnglishVoice.isEmpty())
                //setDefaultEnglishVoice();
            //mEngTts.stop();

            //Bundle with params
            Bundle paramBundle = new Bundle();
            paramBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, ENG_UTT_ID);
            paramBundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, mEngVolume/100f);

            //Params for SDK < 21 (deprecated)
/*
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "eng");
            map.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(mEngVolume/100f));
*/

            mEngTts.setSpeechRate(mEngRate/100f);

            // pause thread until English synthesis finishes
            pauseEngSynth();

            synchronized (mPauseLock) {
                mCallback = callback;
                //if (mEngTtsSampleRate != 0)
                startCallback(mEngTtsSampleRate, mEngTtsAudioFormat, mEngTtsChannelcount);
                    //startCallback(22050, AudioFormat.ENCODING_PCM_16BIT, 1);
                engSpeak(text, TextToSpeech.QUEUE_FLUSH, paramBundle, ENG_UTT_ID);
/*
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mEngTts.speak(text, TextToSpeech.QUEUE_FLUSH, paramBundle, "eng");
                } else {
                    mEngTts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
                }
*/
                while (mPaused) {
                    try {
                        mPauseLock.wait();
                    } catch (InterruptedException e) {
                        //1106 Crashlytics.log(Log.ERROR, LOG_TAG, e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

/*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mEngTts.synthesizeToFile(text, paramBundle,
                        new File(Startup.getDataPath() + "eng.wav"),
                        "eng");
            } else {
                mEngTts.synthesizeToFile(text, map, Startup.getDataPath() + "eng.wav");
            }
*/
        }
        else {
            if (!((Objects.equals(mLanguage, language)) &&
                    (Objects.equals(mCountry, country)) &&
                    (Objects.equals(mVariant, variant)))) {
/*
		    if (!(language.equals("eng") || language.equals("en"))) {
*/
                //Log.v(LOG_TAG, "Checked: " +language+country+variant +" vs: " +mLanguage+mCountry+mVariant);
                result = mEngine.setLanguage(language, country, variant);
                mLanguage = language;
                mCountry = country;
                mVariant = variant;
/*
            }

            elseif (mLanguage.isEmpty()) {
		        result = false;
                Set<String> langs = getCopiedVoiceList().keySet();
                int size = langs.size();
                if (size > 0) {
                    for (String lang : langs) {
                        Log.v(LOG_TAG, "onSynthesize: checking for random lang:" + lang);
                        Voice defaultVoice = getCopiedVoiceList().get(lang).get(0);
                        if (defaultVoice != null)
                            result = mEngine.setLanguage(lang, DEFAULT_COUNTRY, variant);
                    }
                }
            }*/
            } else {
                result = true;
            }

            if (!result) {
                //1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Could not set language for synthesis");
                return;
            }

            text = text.replace(" ।", ". ").replace("।", ".")
                    .replace(" |", ". ").replace("|", ".");
            //Log.v(LOG_TAG, "onSynthesize: " + text);

            mEngine.setSpeechRate(speechrate);

            mCallback = callback;
            //Integer rate = new Integer(mEngine.getSampleRate());
            //Log.v(LOG_TAG, rate.toString());
            mCallback.start(mEngine.getSampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1);
            mEngine.synthesize(text);
        }
	}

    //Handle deprecated speak method
    private void engSpeak(String text, int queueMode, Bundle paramBundle, String uttID) {
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mEngTts.speak(text, queueMode, paramBundle, uttID);
        } else {
            HashMap<String, String> paramMap = mapToBundle(paramBundle);
            mEngTts.speak(text, TextToSpeech.QUEUE_FLUSH, paramMap);
        }
*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mEngTts.synthesizeToFile(text, paramBundle,
                    new File(Startup.getDataPath() + "eng.wav"),
                    "eng");
        } else /*(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)*/ {
            mEngTts.speak(text, queueMode, paramBundle, uttID);
        }// else {
/*
            HashMap<String, String> map = new HashMap<>();
            map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "eng");
            map.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf(mEngVolume/100f));
*/
         //   mEngTts.speak(text, queueMode, mapToBundle(paramBundle));
        //}
    }

    //Converts Bundle to HashMap for SDK < 21
    private HashMap<String,String> mapToBundle(Bundle paramBundle) {
        HashMap<String, String> paramMap = new HashMap<>();
        Set<String> keySet = paramBundle.keySet();
        for (String key : keySet) {
            //Log.v(LOG_TAG, "Adding to map: " + key + String.valueOf(paramBundle.get(key)));
            paramMap.put(key, String.valueOf(paramBundle.get(key)));
        }
        return paramMap;
    }

    public void pauseEngSynth() {
        synchronized (mPauseLock) {
            //Log.v(LOG_TAG, "pauselock");
            mPaused = true;
        }
    }

    /**
     * Call this on resume.
     */
    public void resumeEngSynth() {
        synchronized (mPauseLock) {
            //Log.v(LOG_TAG, "Resuming pauselock");
            mPaused = false;
            mPauseLock.notifyAll();
        }
    }

    private void setDefaultEnglishVoice() {
        Locale defaultEnglishVoiceLocale;

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            if (mEngTts.getVoice() != null)
                defaultEnglishVoiceLocale = mEngTts.getVoice().getLocale();
            else
                defaultEnglishVoiceLocale = mEngTts.getLanguage();
        //else
        //    defaultEnglishVoiceLocale = mEngTts.getLanguage();

        //Log.v(LOG_TAG, "setDefaultEnglishVoice, locale: " + defaultEnglishVoiceLocale);

        if (!defaultEnglishVoiceLocale.getISO3Language().isEmpty())
            if (!defaultEnglishVoiceLocale.getISO3Country().isEmpty())
                if (!defaultEnglishVoiceLocale.getVariant().isEmpty())
                    defaultEnglishVoice = defaultEnglishVoiceLocale.getISO3Language() + "-"
                            + defaultEnglishVoiceLocale.getISO3Country() + "-"
                            + defaultEnglishVoiceLocale.getVariant();
                else
                    defaultEnglishVoice = defaultEnglishVoiceLocale.getISO3Language() + "-"
                            + defaultEnglishVoiceLocale.getISO3Country();
            else
                defaultEnglishVoice = defaultEnglishVoiceLocale.getISO3Language();

        if (!defaultEnglishVoice.isEmpty())
            sharedPref.edit().putString(KEY_OTHER_ENG_ENGINE,
                    defaultEnglishEngine + "," + defaultEnglishVoice).apply();
    }

    private boolean isASCIIAlpha(char c) {
        return (((c >= 'a') && (c <='z')) || ((c >= 'A') && (c <='Z')));
    }

    /*
     *  Check for text to be redirected to English TTS
     *  Shyam Krishna
     *  2018/06/08
     *  */
    private boolean isUnicodeIndic(char c) {
        return (((c >= 0x0900) && (c <= 0x0D7F)));
    }

    private boolean isTextEnglish(String text) {
        //Log.v(LOG_TAG, "isTextEnglish: " + text);

        if ((text == null) || (text.isEmpty()))
            return false;

        int length = text.length();
        int lim = (length < 50 ? length : 50);

        for(int i = 0; i < lim; i++) {
            //Log.v(LOG_TAG, "isTextEnglish: test " + text.charAt(i));
            if (isUnicodeIndic(text.charAt(i)))
                return false;
        }

        return true;
    }


	/*
	*  To be used when engine is unresponsive
	*  Shyam Krishna
	*  2018/02/28
	*  (unused)
	*  */
    private void sayWithMediaPlayer(int mediaFile) {

        String langDir;
        String soundFile;

        switch (mLanguage) {
            case "tam":
            case "san":
            case "eng":
                //TODO: case "tel":
                langDir = "eng" + File.separator + mLanguage;
                break;
            default:
                langDir = "";
        }

        //Log.v(LOG_TAG,"sayWithMediaPlayer: langDir: " + langDir);

        if (Objects.equals(langDir, ""))
            return;

        switch (mediaFile) {
            case MEDIA_NOT_RESPONSIVE:
                soundFile = langDir + "_not_responding.mp3";
                break;
            case MEDIA_OKAY:
                soundFile = langDir + "_okay.mp3";
                break;
            case MEDIA_APP_NAME:
                soundFile = langDir + "_app_name.mp3";
                break;
            default:
                soundFile = "";
                break;
        }

        //Log.v(LOG_TAG,"sayWithMediaPlayer: soundFile: " + soundFile);

        try {
            AssetFileDescriptor afd = getAssets().openFd(soundFile);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            //1106  Crashlytics.log(Log.ERROR, LOG_TAG, "IOException on sound file: " + soundFile);
            Log.v(LOG_TAG, "catch");
        }
    }

    private final UtteranceProgressListener mEngCallback = new UtteranceProgressListener() {
        @Override
        public void onStop(String utteranceId, boolean interrupted) {
            //Log.v(LOG_TAG, "onStop:" + utteranceId);
            //mCallback.done();
            finishCallback();
        }

        @Override
        public void onStart(String utteranceId) {
            //Log.v(LOG_TAG, "onStart:" + utteranceId);

        }

        @Override
        public void onDone(String utteranceId) {
            //Log.v(LOG_TAG, "onDone:" + utteranceId);
            //mCallback.done();
            finishCallback();
        }

        @Override
        public void onError(String utteranceId) {
            //mCallback.done();
            //mCallback.error();
            finishCallback();
            //Log.v(LOG_TAG, "onError:" + utteranceId);

        }

        @Override
        public void onError(String utteranceId, int errorCode) {
            //mCallback.error(errorCode);
            finishCallback();
            //Log.v(LOG_TAG, "onError " + errorCode + ": " + utteranceId);
        }

        @Override
        public void onBeginSynthesis(String utteranceId, int sampleRateInHz, int audioFormat, int channelCount) {
            //Log.v(LOG_TAG, "onBeginSynthesis:" + utteranceId + sampleRateInHz + audioFormat
            //        + channelCount);
            if (Objects.equals(utteranceId, ENG_AUDIO_INIT)) {
                //Log.v(LOG_TAG, "Initializing eng audio params");
                mEngTtsSampleRate = sampleRateInHz;
                mEngTtsAudioFormat = audioFormat;
                mEngTtsChannelcount = channelCount;
            }
            //mCallback.start(sampleRateInHz, audioFormat, channelCount);
            //startCallback(sampleRateInHz, audioFormat, channelCount);
        }

        @Override
        public void onAudioAvailable(String utteranceId, byte[] audio) {
            //if (DBG)
            //Log.v(LOG_TAG, "onAudioAvailable: " + utteranceId);
            audioAvailableCallback(audio);
        }
    };

    private final NativeFliteTTS.SynthReadyCallback mSynthCallback = new NativeFliteTTS.SynthReadyCallback() {
        @Override
        public void onSynthDataReady(byte[] audioData) {
            //if(mEngTts != null) mEngTts.stop();
            //Log.v(LOG_TAG, "onSynthDataReady");
            if ((audioData == null) || (audioData.length == 0)) {
                onSynthDataComplete();
                return;
            }

            final int maxBytesToCopy = mCallback.getMaxBufferSize();

            int offset = 0;

            while (offset < audioData.length) {
                final int bytesToWrite = Math.min(maxBytesToCopy, (audioData.length - offset));
                mCallback.audioAvailable(audioData, offset, bytesToWrite);
                //audioAvailableCallback(audioData, offset, bytesToWrite);
                offset += bytesToWrite;
            }
            //Log.v(LOG_TAG, "onSynthDataReady: adding handler with runnable");
            killerHandler.removeCallbacksAndMessages(null);
            killerHandler.postDelayed(killerRunnable, 5000);
        }

        @Override
        public void onSynthDataComplete() {

            //Log.v(LOG_TAG, "SynthReadyCallback: onSynthDataComplete");
            //Log.v(LOG_TAG, "onSynthDataComplete: removing handler with runnable");
            killerHandler.removeCallbacksAndMessages(null);

            mCallback.done();
        }
	};

    private void audioAvailableCallback(byte[] audioData) {

        if (mCallback == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        if ((audioData == null) || (audioData.length == 0)) {
            //if (DBG)
            //Log.v(LOG_TAG, "onAudioAvailable done");
            //mCallback.done();
            finishCallback();
            //return;
        }

        final int maxBytesToCopy = mCallback.getMaxBufferSize();

        int offset = 0;

        if (audioData != null) {
            while (offset < audioData.length) {
                final int bytesToWrite = Math.min(maxBytesToCopy, (audioData.length - offset));
                //mCallback.audioAvailable(audio, offset, bytesToWrite);
                mCallback.audioAvailable(audioData, offset, bytesToWrite);
                offset += bytesToWrite;
            }
        }
    }

    //Listens for changes in the English TTS preferences
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        /*Log.v(LOG_TAG, "onSharedPreferenceChanged: " + key);

        if (key.equals(KEY_OTHER_ENG_SWITCH)) {
            otherEngPref = sharedPreferences.getBoolean(KEY_OTHER_ENG_SWITCH, true);
            if (otherEngPref && !otherEngInit && !defaultEnglishEngine.isEmpty())
                initializeEnglish();
        }
        else */
        switch (key) {
            case KEY_OTHER_ENG_ENGINE:
                String defEngVoice = sharedPreferences.getString(KEY_OTHER_ENG_ENGINE, "");
                defaultEnglishEngine = defEngVoice.split(",")[0];
                if (!defaultEnglishEngine.isEmpty() && !otherEngInit) {
                    if (defEngVoice.split(",").length > 1)
                        defaultEnglishVoice = defEngVoice.split(",")[1];
                    initializeEnglish();
                }
                break;
            case KEY_ENGLISH_ENGINE:
                String engVoice = sharedPreferences.getString(KEY_ENGLISH_ENGINE, "");
                defaultEnglishEngine = engVoice.split(",")[0];
                if (!defaultEnglishEngine.isEmpty()) {
                    if (engVoice.split(",").length > 1)
                        defaultEnglishVoice = engVoice.split(",")[1];
                    initializeEnglish();
                }
                break;
            case KEY_ENGLISH_VOLUME:
                mEngVolume = sharedPreferences.getInt(KEY_ENGLISH_VOLUME, 100);
                break;
            case KEY_ENG_RATE:
                mEngRate = sharedPreferences.getInt(KEY_ENG_RATE, 60);
                break;
        }
    }

	/*
	  Listens for language update broadcasts and initializes the flite engine.
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			initializeFliteEngine();
		}
	};
	 */
}
