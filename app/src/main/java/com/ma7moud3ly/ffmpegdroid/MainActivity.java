package com.ma7moud3ly.ffmpegdroid;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private TextView result;
    private EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();
        input = findViewById(R.id.input);
        result = findViewById(R.id.result);
        result.setMovementMethod(new ScrollingMovementMethod());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("not_first_time", false)) {
            loadFFMPEG(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("not_first_time", true);
            editor.commit();
        }

    }

    private void loadFFMPEG(Context context) {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            Toast.makeText(getApplicationContext(), "your device supported", Toast.LENGTH_SHORT).show();
        }
    }

    private void execFFMPEG(final String[] cmd, Context context) {
        FFmpeg ffmpeg = FFmpeg.getInstance(context);
        try {
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {
                    result.setText("");
                }

                @Override
                public void onProgress(String message) {
                    result.append(message);
                }

                @Override
                public void onFailure(String message) {
                    result.append(message);
                }

                @Override
                public void onSuccess(String message) {
                    result.append(message);
                }

                @Override
                public void onFinish() {
                    //Toast.makeText(getApplicationContext(), "execution finished", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            e.printStackTrace();
        }
    }

    public void clear(View v) {
        input.setText("-y -i ");
    }

    public void eval(View v) {
        hideKeyboard(this);
        String cmd = input.getText().toString();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(cmd.split(" ")));
        while (args.remove("")) ;
        execFFMPEG(args.toArray(new String[args.size()]), this);
    }

    public void insert(View v) {
        Intent intent = new Intent(this, FilePickerActivity.class);
        intent.putExtra(FilePickerActivity.CONFIGS, new Configurations.Builder()
                .setShowFiles(true)
                .enableImageCapture(true)
                .setMaxSelection(1)
                .setSingleChoiceMode(true)
                .setSkipZeroSizeFiles(true)
                .build());
        startActivityForResult(intent, 10003);
    }

    public void storage(View v) {
        String path = Environment.getExternalStorageDirectory().getPath();
        input.getText().insert(input.getSelectionEnd(), path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10003 && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<MediaFile> files = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);
            input.getText().insert(input.getSelectionEnd(), files.get(0).getPath());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            Toast.makeText(getApplicationContext(), "storage permission is enabled :)", Toast.LENGTH_LONG).show();
        else {
            Toast.makeText(getApplicationContext(), "You should enable storage permissions", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.
                        WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            return true;
        }
    }

    private void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}