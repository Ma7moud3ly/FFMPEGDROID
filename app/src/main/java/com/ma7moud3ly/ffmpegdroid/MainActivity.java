package com.ma7moud3ly.ffmpegdroid;

import android.Manifest;
import android.app.Activity;
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

import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.ma7moud3ly.ffmpegdroid.Shell.ExecutionAsyncTask;
import com.ma7moud3ly.ffmpegdroid.Shell.ExecutionResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private TextView result;
    private EditText input;
    private String path;
    private ExecutionAsyncTask mExecuteAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();
        input = findViewById(R.id.input);
        result = findViewById(R.id.result);
        result.setMovementMethod(new ScrollingMovementMethod());
        result.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                result.setText("");
                return false;
            }
        });
        path = getApplicationContext().getApplicationInfo().dataDir + "/" + "ffmpeg";


        String arch = System.getProperty("os.arch");
        if (!arch.equals("aarch64")) {
            Toast.makeText(this, "Your device architecture not supported", Toast.LENGTH_LONG).show();
            finish();
        }
        if (first_time()) copyFFMPEG();
    }

    private boolean first_time() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("first_time", true)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first_time", false);
            editor.commit();
            return true;
        }
        return false;
    }

    private boolean copyFFMPEG() {
        //Toast.makeText(this, "copy", Toast.LENGTH_LONG).show();
        try {
            InputStream in = getAssets().open("ffmpeg_aarch64");
            FileOutputStream out = new FileOutputStream(path);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
            new File(path).setExecutable(true);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    private String[] getArgs() {
        String cmd = input.getText().toString().trim();
        cmd = path + " " + cmd;
        ArrayList<String> args = new ArrayList<>(Arrays.asList(cmd.split(" ")));
        while (args.remove("")) ;
        result.append(args.toString());
        return args.toArray(new String[args.size()]);
    }

    public void clear(View v) {
        input.setText("-i ");
    }

    public void eval(View V) {
        hideKeyboard(this);
        String[] command = getArgs();
        mExecuteAsyncTask = new ExecutionAsyncTask(command, 1000 * 60 * 10, mExecutionResponse);
        mExecuteAsyncTask.execute();
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

    public void stop(View V) {
        if (mExecuteAsyncTask != null && mExecuteAsyncTask.isProcessCompleted() == false)
            mExecuteAsyncTask.destroyProcess();
        result.append("\n" + "stop running process!");
    }

    ExecutionResponse mExecutionResponse = new ExecutionResponse() {
        @Override
        public void onFailure(String s) {
            result.append(s);
        }

        @Override
        public void onSuccess(String s) {
            result.append(s);
        }

        @Override
        public void onProgress(String s) {
            result.append(s);
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onFinish() {
        }
    };


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