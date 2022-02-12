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
/*               Date:  July 2012                                        */
/*************************************************************************/

package org.hear2read.indic.tts;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import android.support.annotation.NonNull;
import android.util.Log;

//import com.crashlytics.android.Crashlytics;

import static org.hear2read.indic.Startup.getDataPath;
import static org.hear2read.indic.Flite.DBG;

public class Voice {
	private final static String LOG_TAG = "Flite_Java_" + Voice.class.getSimpleName();
	private final static String FLITE_DATA_PATH = getDataPath();

	// private final static String FLITE_DATA_PATH = "file:///android_asset/";

	// private final static Uri path = Uri.parse("file:///src/assets/");

	// private final static String FLITE_DATA_PATH = path.toString();


	private final static String VOICE_BASE_URL = "http://festvox.org/flite/voices/cg/voxdata-hinilonly/";
	// private final static String VOICE_BASE_URL = "http://tts.speech.cs.cmu.edu/temp_info_for_flite_app/";

	private String mVoiceName;
	private String mVoiceMD5;
	private String mVoiceLanguage;
	private String mVoiceCountry;
	private String mVoiceVariant;
	private boolean mIsValidVoice;
	private String mVoicePath;
	private boolean mIsVoiceAvailable;

	/**
	 * @return absolute path to the hear2read-data directory
	 */
	public static String getDataStorageBasePath() {

		//if (DBG) Log.v(LOG_TAG, "FLITE_DATA_PATH is: " + FLITE_DATA_PATH);
		return FLITE_DATA_PATH;
	}

	/**
	 * @return base URL to download voices and other flite data
	 */
	public static String getDownloadURLBasePath() {
		return VOICE_BASE_URL;
	}

	/**
	 * @param voiceInfoLine is the line that is found in "voices.list" file
	 * as downloaded on the server and cached. This line has text in the format:
	 * language-country-variant<TAB>MD5SUM
	 */
	public Voice(String voiceInfoLine) {
		boolean parseSuccessful = false;

		String[] voiceInfo = voiceInfoLine.split("\t");
		if (voiceInfo.length != 1) {
			//1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Voice line could not be read: " + voiceInfoLine);
			Log.v(LOG_TAG, "if");
		}
		else {
			mVoiceName = voiceInfo[0];
			//mVoiceMD5 = voiceInfo[1];

			String[] voiceParams = mVoiceName.split("-");
			if(voiceParams.length != 3) {
				//1106 Crashlytics.log(Log.ERROR, LOG_TAG,"Incorrect voicename:" + mVoiceName);
				Log.v(LOG_TAG, "if");
			}
			else {
				String voiceLanguageISO2 = voiceParams[0];
				String voiceCountryISO2 = voiceParams[1];
				mVoiceVariant = voiceParams[2];

				mVoiceLanguage = langISO2toISO3(voiceLanguageISO2);
				mVoiceCountry = countryISO2toISO3(voiceLanguageISO2, voiceCountryISO2);
				//if (DBG) Log.v(LOG_TAG, "The voice language and country codes are stored as: "
				//		+ mVoiceLanguage + ", " + mVoiceCountry);

				parseSuccessful = true;
			}
		}

		if (parseSuccessful) {
			mIsValidVoice = true;
			mVoicePath = getDataStorageBasePath() + "cg/" + mVoiceLanguage +
					"/" + mVoiceCountry + "/" + mVoiceVariant + ".cg.flitevox";
			checkVoiceAvailability();
		}
		else {
			mIsValidVoice = false;
		}

	}

	private String langISO2toISO3(String language) {
		Locale loc = new Locale(language);
		return loc.getISO3Language();
	}

	private String countryISO2toISO3(String language, String country) {
		Locale loc = new Locale(language, country);
		return loc.getISO3Country();
	}

	private void checkVoiceAvailability() {
		//if (DBG) Log.v(LOG_TAG, "Checking for Voice Available: " + mVoiceName);

		mIsVoiceAvailable = false;

		// The file should exist, as well as the MD5 sum should match.
		// Only then do we mark a voice as available.
		//
		// We can attempt getting an MD5sum, and an IOException will
		// tell us if the file didn't exist at all.

		FileInputStream fis;
		try {
			fis = new FileInputStream(mVoicePath);
			mIsVoiceAvailable = true;
		}
		catch (FileNotFoundException e) {
			//1106 Crashlytics.log(Log.ERROR, LOG_TAG, "Voice File not found: " + mVoicePath);
			Log.v(LOG_TAG, "catch");
		}/*

		byte[] dataBytes = new byte[1024];
		int nread = 0;
		try {
			while ((nread = fis.read(dataBytes)) != -1) {
			}
		} catch (IOException e) {
			if (DBG) Log.e(LOG_TAG, "Could not read voice file: " + mVoicePath);
			return;
		}
		finally {
			try {
				fis.close();
			} catch (IOException e) {
				// Ignoring this exception.
			}
		}
			mIsVoiceAvailable = true;*/
	}

	public boolean isValid() {
		return mIsValidVoice;
	}

	public boolean isAvailable() {
		return mIsVoiceAvailable;
	}

	public String getName() {
		return mVoiceName;
	}

	public String getDisplayName() {
		Locale loc = new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);
		return loc.getDisplayLanguage() +
				"(" + loc.getDisplayCountry() + "," + loc.getVariant() + ")";
	}

	public String getVariant() {
		return mVoiceVariant;
	}

	public String getDisplayLanguage() {
		Locale loc = new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);

		return loc.getDisplayLanguage() +
				" (" + loc.getDisplayCountry() + ")";
	}

	public String getPath() {
		return mVoicePath;
	}

	public Locale getLocale() {
		//Log.v(LOG_TAG, "Returning voice locale: " + mLocale);
		return new Locale(mVoiceLanguage, mVoiceCountry, mVoiceVariant);
		//return new Locale(mVoiceLanguage, mVoiceCountry, "");
	}
}
