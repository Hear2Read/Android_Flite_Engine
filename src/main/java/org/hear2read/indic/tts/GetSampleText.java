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

package org.hear2read.indic.tts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

//import com.crashlytics.android.Crashlytics;

import org.hear2read.indic.R;

import java.util.Locale;

import static org.hear2read.indic.Flite.DBG;

/*
 * Returns the sample text string for the language requested
 */
public class GetSampleText extends Activity {
	private final static String LOG_TAG = "Flite_Java_" + GetSampleText.class.getSimpleName();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Locale locale = getLocaleFromIntent(getIntent());

    final String language = (locale == null) ? "eng" : locale.getISO3Language();

    int result = TextToSpeech.LANG_AVAILABLE;
    Intent returnData = new Intent();

    switch (language) {
      case "asm":
        returnData.putExtra("sampleText", getString(R.string.asm_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.eng_sample));
        break;
      case "eng":
        returnData.putExtra("sampleText", getString(R.string.eng_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.eng_sample));
        break;
      case "guj":
        returnData.putExtra("sampleText", getString(R.string.guj_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.eng_sample));
        break;
      case "hin":
        returnData.putExtra("sampleText", getString(R.string.hin_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.hin_sample));
        break;
      case "kan":
        returnData.putExtra("sampleText", getString(R.string.kan_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.kan_sample));
        break;
      case "mal":
        returnData.putExtra("sampleText", getString(R.string.mal_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.kan_sample));
        break;
      case "mar":
        returnData.putExtra("sampleText", getString(R.string.mar_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.mar_sample));
        break;
      case "pan":
        returnData.putExtra("sampleText", getString(R.string.pan_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.pan_sample));
        break;
      case "san":
        returnData.putExtra("sampleText", getString(R.string.san_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.san_sample));
        break;
      case "tam":
        returnData.putExtra("sampleText", getString(R.string.tam_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.tam_sample));
        break;
      case "tel":
        returnData.putExtra("sampleText", getString(R.string.tel_sample));
        //if (DBG) Log.v(LOG_TAG, "Returned SampleText: " + getString(R.string.tel_sample));
        break;
      case "ori":
        returnData.putExtra("sampleText", getString(R.string.ori_sample));
        break;

      default:
        //Crashlytics.log(Log.WARN, LOG_TAG, "Unsupported Language: " + language);
        result = TextToSpeech.LANG_NOT_SUPPORTED;
        returnData.putExtra("sampleText", "");
        break;
    }

    setResult(result, returnData);

    finish();
  }

  private static Locale getLocaleFromIntent(Intent intent) {
    if (intent != null) {
      final String language = intent.getStringExtra("language");

      if (language != null) {
        return new Locale(language);
      }
    }

    return Locale.getDefault();
  }
}
