package com.hudson.rewind;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Environment;

import com.hudson.rewind.tech.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Hudson Hughes on 1/3/2016.
 */
public class HelperClass {
    public static int getSAMPLERATE(Context context){
        int SAMPLING_RATE = 8000;
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        for (int rate : new int[]{44100, 22050, 16000, 11025, 8000}) {  // add the rates you wish to check against
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                // buffer size is valid, Sample rate supported
                SAMPLING_RATE = rate;
                break;
            }
        }
        SAMPLING_RATE = preferences.getInt("SAMPLING_RATE", SAMPLING_RATE);
        return SAMPLING_RATE;
    }
    public static int setSAMPLERATE(Context context, int SAMPLERATE){
        int SAMPLING_RATE = 8000;
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("SAMPLING_RATE", SAMPLERATE);
        editor.commit();
        SAMPLING_RATE = preferences.getInt("SAMPLING_RATE", SAMPLING_RATE);
        return SAMPLING_RATE;
    }

    public static int getTIME(Context context){
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        int TIME = preferences.getInt("TIME", 5);
        return TIME;
    }
    public static int setTIME(Context context, int TIME){
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        int preTIME = preferences.getInt("TIME", TIME);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("TIME", TIME);
        editor.commit();
        TIME = preferences.getInt("TIME", TIME);
        Log.d("Hudson Hughes", "New Time: " + String.valueOf(TIME));
        if(TIME < preTIME){

        }
        return TIME;
    }

    public static int getAMOUNT(Context context){
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        int TIME = preferences.getInt("AMOUNT", 5);
        return TIME;
    }
    public static int setAMOUNT(Context context, int AMOUNT){
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("AMOUNT", AMOUNT);
        if(AMOUNT > 0)
        editor.commit();
        AMOUNT = preferences.getInt("AMOUNT", AMOUNT);
        Log.d("Hudson Hughes", "New Time: " + String.valueOf(AMOUNT));
        return AMOUNT;
    }

    public static String generateStamp(Context context) {
        String mFileName = getRecordingDirectory(context);
        if (!new File(mFileName).exists()) {
            new File(mFileName).mkdir();
        }
        mFileName += getCurrentTimeStamp() + ".wav";
        return mFileName;
    }

    public static String setRecordingDirectory(Context context, String path) {
        String mFileName = path;
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("PATH", path);
        editor.commit();
        return mFileName;
    }

    public static String getRecordingDirectory(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("settings", context.MODE_PRIVATE);
        String mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RetroactiveRecorder/";
        mFileName = preferences.getString("PATH", mFileName ) + "/";
        if (!new File(mFileName).exists()) {
            new File(mFileName).mkdir();
        }
        return mFileName;
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd-MM-yy_HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
    public static int rateToByte(int rate){
        switch (rate) {
            case 44100:  return 5292000;
            case 22050:  return 5292000 / 2;
            case 16000:  return 1920000;
            case 11025:  return 5292000 / 4;
            case 8000:  return 960000;
            default: return 5292000;
        }
    }
}
