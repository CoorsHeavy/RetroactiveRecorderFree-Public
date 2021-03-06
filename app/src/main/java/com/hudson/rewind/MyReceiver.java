package com.hudson.rewind;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.util.Calendar;

public class MyReceiver extends BroadcastReceiver {
    public MyReceiver() {
    }
    private PendingIntent pendingIntent;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        SharedPreferences settings = context.getSharedPreferences("AppOn", 0);
        if(settings.getBoolean("AppOn", false)){
            Log.d("Hudson Hughes", "Service Started");

            Intent myIntent = new Intent(context, BootReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(context, 0, myIntent, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC, SystemClock.elapsedRealtime() +
                    20 * 1000, pendingIntent);
        }
    }
}
