package com.hudson.rewind;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ThemedSpinnerAdapter;
import android.support.v7.widget.Toolbar;
import android.util.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryCancelEvent;
import com.turhanoz.android.reactivedirectorychooser.event.OnDirectoryChosenEvent;
import com.turhanoz.android.reactivedirectorychooser.ui.DirectoryChooserFragment;
import com.turhanoz.android.reactivedirectorychooser.ui.OnDirectoryChooserFragmentInteraction;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends AppCompatActivity implements
        OnDirectoryChooserFragmentInteraction {

    Button SampleButton;
    TextView BufferSizeLabel;
    Switch toggle;
    Button BufferSizeButton;
    Button DeleteButton;
    Button DirectoryButton;
    TextView pathView;
    TextView sampleView;
    @Override
    public void onEvent(OnDirectoryChosenEvent event) {
        File directoryChosenByUser = event.getFile();
        HelperClass.setRecordingDirectory(getContext(), event.getFile().getPath());
        pathView.setText(HelperClass.getRecordingDirectory(getContext()));
    }

    @Override
    public void onEvent(OnDirectoryCancelEvent event) {
    }
    public static boolean getMicrophoneAvailable(Context ctx){
        AudioRecord audio = null;
        boolean ready = true;
        try{
            int baseSampleRate = 44100;
            int channel = AudioFormat.CHANNEL_IN_MONO;
            int format = AudioFormat.ENCODING_PCM_16BIT;
            int buffSize = AudioRecord.getMinBufferSize(baseSampleRate, channel, format);
            audio = new AudioRecord(MediaRecorder.AudioSource.MIC, baseSampleRate, channel, format, buffSize );
            audio.startRecording();
            short buffer[] = new short[buffSize];
            int audioStatus = audio.read(buffer, 0, buffSize);

            if(audioStatus == AudioRecord.ERROR_INVALID_OPERATION || audioStatus == AudioRecord.STATE_UNINITIALIZED /* For Android 6.0 */)
                ready = false;
        }
        catch(Exception e){
            ready = false;
        }
        finally {
            try{
                audio.release();
            }
            catch(Exception e){}
        }

        return ready;
    }
    AudioRecord recorder;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_settings);
        BufferSizeLabel = (TextView) findViewById(R.id.BufferSize);
        toggle = (Switch) findViewById(R.id.switch1);
        SampleButton = (Button) findViewById(R.id.SampleButton);
        BufferSizeButton = (Button) findViewById(R.id.BufferSizeButton);
        DeleteButton = (Button) findViewById(R.id.DeleteButton);
        DirectoryButton = (Button) findViewById(R.id.DirectoryButton);
        pathView = (TextView) findViewById(R.id.PathView);
        pathView.setText(HelperClass.getRecordingDirectory(getContext()));
        sampleView = (TextView) findViewById(R.id.SampleView);
        sampleView.setText("Sampling Rate:" + String.valueOf(HelperClass.getSAMPLERATE(getContext())));
        refreshh();
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

// 2. Chain together various setter methods to set the dialog characteristics
                builder.setTitle("Alert")
                        .setMessage("Another app is using the microphone. Kill it before starting this one.").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

// 3. Get the AlertDialog from create()
                AlertDialog dialog = builder.create();
                Intent intent = new Intent(getApplicationContext(), ByteRecorder.class);
                SharedPreferences settings = getSharedPreferences("AppOn", 0);
                if (isChecked) {
                    if(getMicrophoneAvailable(getContext())) {
                        startService(intent);
                        settings.edit().putBoolean("AppOn", true).commit();
                        Log.d("Hudson Hughes", String.valueOf(settings.getBoolean("AppOn", false)));
                    }else{
                        dialog.show();
                    }
                } else {
                    stopService(intent);
                    settings.edit().putBoolean("AppOn", false).commit();
                }
                refreshh();
            }
        });
        DeleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                try {
                                    for (File file : new File(getApplicationContext().getFilesDir() + "/magic/").listFiles()) file.delete();
                                } catch (Exception e) {

                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Are you sure?")
                        .setMessage("Pressing yes will wipe the recorded audio buffer. If you press yes now and tap the grab audio button on the main screen you will only get audio collected after you pressed this yes button.")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });
        BufferSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    Toast.makeText(getApplicationContext(), "Cannot change while background recording is active.", Toast.LENGTH_SHORT).show();
                } else {
                    final Dialog dialog = new Dialog(getContext());
                    dialog.setContentView(R.layout.seekbarlayout);
                    dialog.setTitle("Set a buffer size");
                    dialog.setCancelable(true);
                    final SeekBar seekBar = (SeekBar) dialog.findViewById(R.id.seekBar);
                    final TextView BufferSieLabel = (TextView) dialog.findViewById(R.id.BufferSizeLabel);
                    final TextView OKButton = (TextView) dialog.findViewById(R.id.OKButton);
                    OKButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            HelperClass.setTIME(getApplicationContext(), seekBar.getProgress());
                            BufferSizeLabel.setText(HelperClass.getTIME(getApplicationContext()) + " minutes");
                            //BufferSizeLabel.setText(HelperClass.getTIME(getApplicationContext()) + " minutes");
                            dialog.cancel();
                        }
                    });
                    BufferSizeLabel.setText(String.valueOf(HelperClass.getTIME(getApplicationContext()) + " minutes"));
                    BufferSieLabel.setText(String.valueOf(HelperClass.getTIME(getApplicationContext()) + " minutes"));
                    seekBar.setProgress(HelperClass.getTIME(getApplicationContext()));
                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (progress < 1) {
                                seekBar.setProgress(1);
                            }else if (progress > 5) {
                                seekBar.setProgress(5);
                            } else {
                                //HelperClass.setTIME(getApplicationContext(), progress);
                            }
                            //HelperClass.setAMOUNT(getApplicationContext(), HelperClass.getTIME(getApplicationContext()));
                            BufferSieLabel.setText(seekBar.getProgress() + " minutes");
                            BufferSizeLabel.setText(HelperClass.getTIME(getApplicationContext()) + " minutes");
                            refreshh();
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    dialog.show();
                }
            }
        });

        DirectoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File currentRootDirectory = Environment.getExternalStorageDirectory();
                DirectoryChooserFragment directoryChooserFragment = DirectoryChooserFragment.newInstance(currentRootDirectory);
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                directoryChooserFragment.show(transaction, "RDC");
            }
        });

        SampleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning()) {
                    Toast.makeText(getApplicationContext(), "Cannot change while background recording is active.", Toast.LENGTH_SHORT).show();
                } else {
                    final Dialog dialog = new Dialog(getContext());
                    dialog.setContentView(R.layout.samplingrates);
                    dialog.setTitle("Set a sampling rate");
                    dialog.setCancelable(true);
                    final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radioGroup);
                    RadioButton r11025 = (RadioButton) dialog.findViewById(R.id.r11025);
                    RadioButton r16000 = (RadioButton) dialog.findViewById(R.id.r16000);
                    RadioButton r22050 = (RadioButton) dialog.findViewById(R.id.r22050);
                    RadioButton r8000 = (RadioButton) dialog.findViewById(R.id.r8000);
                    RadioButton r44100 = (RadioButton) dialog.findViewById(R.id.r44100);
                    Button ok_button = (Button) dialog.findViewById(R.id.ok_button);
                    Button cancel_button = (Button) dialog.findViewById(R.id.cancel_action);
                    switch (Math.round(HelperClass.getSAMPLERATE(getApplicationContext()))) {
                        case 11025:
                            r11025.setChecked(true);
                            android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                            break;
                        case 16000:
                            r16000.setChecked(true);
                            android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                            break;
                        case 22050:
                            r22050.setChecked(true);
                            android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                            break;
                        case 8000:
                            r8000.setChecked(true);
                            android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                            break;
                        case 44100:
                            r44100.setChecked(true);
                            android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                            break;
                    }
                    cancel_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.cancel();
                        }
                    });
                    ok_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!isServiceRunning())
                                switch (radioGroup.getCheckedRadioButtonId()) {
                                    case R.id.r11025:
                                        android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                                        int bufferSize = AudioRecord.getMinBufferSize(11025, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                        if (bufferSize > 0) {
                                            if(HelperClass.getSAMPLERATE(getApplicationContext()) != 11025) for (File file : new File(getApplicationContext().getFilesDir() + "/magic/").listFiles()) file.delete();
                                            HelperClass.setSAMPLERATE(getApplicationContext(), 11025);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Your phone doesn't support this frequency.", Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    case R.id.r16000:
                                        android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                                        int bufferSize1 = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                        if (bufferSize1 > 0) {
                                            if(HelperClass.getSAMPLERATE(getApplicationContext()) != 16000) for (File file : new File(getApplicationContext().getFilesDir() + "/magic/").listFiles()) file.delete();
                                            HelperClass.setSAMPLERATE(getApplicationContext(), 16000);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Your phone doesn't support this frequency.", Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    case R.id.r22050:
                                        android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                                        int bufferSize2 = AudioRecord.getMinBufferSize(22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                        if (bufferSize2 > 0) {
                                            if(HelperClass.getSAMPLERATE(getApplicationContext()) != 22050) for (File file : new File(getApplicationContext().getFilesDir() + "/magic/").listFiles()) file.delete();
                                            HelperClass.setSAMPLERATE(getApplicationContext(), 22050);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Your phone doesn't support this frequency.", Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    case R.id.r8000:
                                        android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                                        int bufferSize3 = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                        if (bufferSize3 > 0) {
                                            if(HelperClass.getSAMPLERATE(getApplicationContext()) != 8000) for (File file : new File(getApplicationContext().getFilesDir() + "/magic/").listFiles()) file.delete();
                                            HelperClass.setSAMPLERATE(getApplicationContext(), 8000);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Your phone doesn't support this frequency.", Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    case R.id.r44100:
                                        android.util.Log.d("Hudson Hughes", String.valueOf(radioGroup.getCheckedRadioButtonId()));
                                        int bufferSize4 = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                                        if (bufferSize4 > 0) {
                                            HelperClass.setSAMPLERATE(getApplicationContext(), 44100);
                                        } else {
                                            Toast.makeText(getApplicationContext(), "Your phone doesn't support this frequency.", Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                }
                            else {
                                Toast.makeText(getApplicationContext(), "Cannot set while background recording is active.", Toast.LENGTH_LONG).show();
                            }
                            sampleView.setText("Sampling Rate:" + String.valueOf(HelperClass.getSAMPLERATE(getContext())));
                            dialog.cancel();
                            refreshh();
                        }
                    });

                    dialog.show();
                }
            }
        });
    }
    public void refreshh(){
        toggle.setChecked(isServiceRunning());
        BufferSizeLabel.setText(HelperClass.getTIME(getApplicationContext()) + " minutes");
        sampleView.setText("Sampling Rate:" + String.valueOf(HelperClass.getSAMPLERATE(getContext())));
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.hudson.rewind.ByteRecorder".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private Context getContext() {
        return SettingsActivity.this;
    }
    public void mainAct(View view){
        startActivity(new Intent(getContext(), MainActivity.class));
    }
    public void settingsAct(View view){
        startActivity(new Intent(getContext(), SettingsActivity.class));
    }
    public void recordingsAct(View view) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getContext());
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
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void helpAct(View view){
        String helpMessage = "WARNING: Using the task manager to close the application will temporarily interrupt the recording of audio. Interuptting the audio recording via the task manager or settings menu will not wipe the audio buffer. So if you save a file in which the recording was interrupted the audio will skip from where the recording was paused to when it was resumed.\n" +
                "Retroactive Recording is a retroactive audio recording app. The app is constantly recording audio while it is activated but only ever keeps the most recent 1 to 30 minutes of data." +
                " At any time you can tap the save audio button on the main screen to save previously recorded audio. For example, if you turn the app on in the beginning of the day, keep it on and later in the day, someone says something that you want a recording of, you can tap the save audio button and your device will store the past 1 to 30 minutes in a WAV audio file for later listening." +
                " While the app is constantly recording audio, it will only occupy up to 30 minutes worth of audio data in the memory." +
                " In the event that your device runs out of storage space the app will cease to operate.";
        //startActivity(new Intent(getContext(), HelpActivity.class));
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getContext());
// Add the buttons
        builder.setMessage(helpMessage);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });
// Set other dialog properties

// Create the AlertDialog
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void goPro(View view){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.hudson.rewindpro")));
        } catch (android.content.ActivityNotFoundException anfe) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.hudson.rewindpro")));
        }
    }
}
