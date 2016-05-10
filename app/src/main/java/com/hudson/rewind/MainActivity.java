package com.hudson.rewind;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hudson.rewind.tech.ProgressPCM;
import com.hudson.rewind.tech.WavAudioFormat;

import java.io.File;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    long first = 0;
    long latest = 0;
    SeekBar seekBar;
    TextView timeLabel;
    Button button;
    TextView MaxLabel;
    TextView CurrentLength;
//    ListView listview;
//    List<String> fileList = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String helpMessage = "WARNING: Using the task manager to close the application will temporarily interrupt the recording of audio. Interuptting the audio recording via the task manager or settings menu will not wipe the audio buffer. So if you save a file in which the recording was interrupted the audio will skip from where the recording was paused to when it was resumed.\n" +
            "Retroactive Recording is a retroactive audio recording app. The app is constantly recording audio while it is activated but only ever keeps the most recent 1 to 30 minutes of data." +
            " At any time you can tap the save audio button on the main screen to save previously recorded audio. For example, if you turn the app on in the beginning of the day, keep it on and later in the day, someone says something that you want a recording of, you can tap the save audio button and your device will store the past 1 to 30 minutes in a WAV audio file for later listening." +
            " While the app is constantly recording audio, it will only occupy up to 30 minutes worth of audio data in the memory." +
            " In the event that your device runs out of storage space the app will cease to operate.";

    private Handler mHandler = new Handler();

    private Runnable onEverySecond = new Runnable() {
        public void run() {
            try {

                refresh();
            } catch (Exception e) {

            }
            mHandler.postDelayed(onEverySecond, 1000);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final SharedPreferences sharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("showHelp", true)) {
            //startActivity(new Intent(getContext(), HelpActivity.class));

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
// Add the buttons
            builder.setTitle("Welcome");
            builder.setMessage(helpMessage);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("showHelp", false);
                    editor.commit();
                }
            });
// Set other dialog properties

// Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        //listview = (ListView) findViewById(R.id.listView);
        //adapter = new ArrayAdapter<String>(this,
                //android.R.layout.simple_list_item_1, android.R.id.text1, fileList);
        // Assign adapter to ListView
        //listview.setAdapter(adapter);


        MaxLabel = (TextView) findViewById(R.id.MaxLength);
        CurrentLength = (TextView) findViewById(R.id.CurrentLength);
        timeLabel = (TextView) findViewById(R.id.timeLabel);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        button = (Button) findViewById(R.id.Button);
        seekBar.setProgress(HelperClass.getAMOUNT(getContext()));
        timeLabel.setText(String.valueOf(HelperClass.getAMOUNT(getContext()) + " minutes"));
        mHandler.post(onEverySecond);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 1) {
                    HelperClass.setAMOUNT(getContext(), progress);
                    timeLabel.setText(String.valueOf(HelperClass.getAMOUNT(getContext()) + " minutes"));
                } else {
                    seekBar.setProgress(1);
                }
                if (progress < HelperClass.getTIME(getContext())) {
                    HelperClass.setAMOUNT(getContext(), progress);
                    timeLabel.setText(String.valueOf(HelperClass.getAMOUNT(getContext()) + " minutes"));

                } else {
                    seekBar.setProgress(HelperClass.getTIME(getContext()));
                }
                refresh();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        refresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHandler.removeCallbacks(onEverySecond);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(onEverySecond);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(onEverySecond);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeCallbacks(onEverySecond);
        mHandler.postDelayed(onEverySecond, 1000);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mHandler.removeCallbacks(onEverySecond);
        mHandler.postDelayed(onEverySecond, 1000);
    }

    public void refresh() {
        button.setText("click here to save the buffer of the past.");
        int folder_length = 0;
        try {
            for (File file : Arrays.asList(new File(getContext().getFilesDir() + "/magic/").listFiles())) {
                if (file.isFile())
                    folder_length += file.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
            folder_length = 0;

        }
        latest = folder_length;
        if(latest != first) button.setText("Click to save the current buffer.\nRecording...");
        else button.setText("Click to save the current buffer.\nNot Recording...");
        first = latest;
        double di = (double)HelperClass.getSAMPLERATE(MainActivity.this) / (double)41000;
        double div = (double)folder_length / (HelperClass.rateToByte(HelperClass.getSAMPLERATE(MainActivity.this)) / (double)60);
        int currentSeconds = (int) Math.round(div);
        if (HelperClass.getTIME(getContext()) * 60 < currentSeconds) {
            currentSeconds = HelperClass.getTIME(getContext()) * 60;
        }
        String minutes = String.valueOf(currentSeconds / 60);
        String seconds = String.valueOf(currentSeconds % 60);
        if (minutes.length() == 1) minutes = "0" + minutes;
        if (seconds.length() == 1) seconds = "0" + seconds;
        CurrentLength.setText(minutes + ":" + seconds);
        MaxLabel.setText(String.valueOf(HelperClass.getTIME(MainActivity.this)) + ":00");
        timeLabel.setText(String.valueOf(HelperClass.getAMOUNT(getContext()) + " minutes"));
        if (isServiceRunning()) {
            button.setEnabled(true);
        } else {
            button.setText("Activate the background listening in the settings.");
            button.setEnabled(false);
        }
//        fileList.clear();
//        if(new File(getFilesDir() + "/magic/").isDirectory() && new File(getFilesDir() + "/magic/").listFiles().length > 0) {
//            List<File> pcms = Arrays.asList(new File(getFilesDir() + "/magic/").listFiles());
//            Collections.sort(pcms);
//            for (File file : pcms) {
//                fileList.add(file.getName());
//            }
//            adapter.notifyDataSetChanged();
//        }
        if(seekBar.getProgress() > HelperClass.getTIME(getContext())){
            seekBar.setProgress(HelperClass.getTIME(getContext()));
            timeLabel.setText(String.valueOf(HelperClass.getTIME(getContext()) + " minutes"));
        }
    }

    public void getAudio(View view) {
        File file_raw = new File(getFilesDir() + "/buffer.raw");
        if (!file_raw.exists()) {
            return;
        }
        File file_wav = new File(HelperClass.generateStamp(getContext()));
        if (file_wav.exists()) {
            file_wav.delete();
        }

        try {
            new ProgressPCM(WavAudioFormat.mono16Bit(Math.round(HelperClass.getSAMPLERATE(getContext()))), file_raw, file_wav, HelperClass.getAMOUNT(getContext()), Math.round(HelperClass.getSAMPLERATE(getContext())), getApplicationContext(), MainActivity.this).execute();
            //new WavMaker(getContext(), HelperClass.getTIME(MainActivity.this)).get();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.hudson.rewind.ByteRecorder".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private Context getContext() {
        return MainActivity.this;
    }

    public void mainAct(View view) {
        startActivity(new Intent(getContext(), MainActivity.class));
    }

    public void settingsAct(View view) {
        startActivity(new Intent(getContext(), SettingsActivity.class));
    }

    public void recordingsAct(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // Add the buttons
        builder.setMessage("The audio playback section is still in development. Until then use your music player app.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
                startActivity(intent);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
        // Set other dialog properties

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void helpAct(View view) {
        //startActivity(new Intent(getContext(), HelpActivity.class));
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
// Add the buttons
        builder.setMessage(helpMessage);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
// Set other dialog properties

// Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void goPro(View view) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.hudson.rewindpro")));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.hudson.rewindpro")));
        }
    }
}