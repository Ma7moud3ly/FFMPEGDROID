package com.ma7moud3ly.ffmpegdroid;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaiselrahman.filepicker.activity.FilePickerActivity;
import com.jaiselrahman.filepicker.config.Configurations;
import com.jaiselrahman.filepicker.model.MediaFile;
import com.ma7moud3ly.ustore.USon;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {
    private TextView result;
    private EditText input;
    private String path;
    public static int history_index;
    private ArrayList<String> history = new ArrayList<>();
    private ShellCommands shellCommands;
    private USon uson;
    private String sdcard = "";
    private ProgressBar progress;
    private MyViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();

        model = new ViewModelProvider(this).get(MyViewModel.class);
        Observer<String> input_observer = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String val) {
                input.setText(val);
            }
        };
        model.getInput().observe(this, input_observer);

        input = findViewById(R.id.input);
        progress = findViewById(R.id.progress);
        final ImageView btn = findViewById(R.id.clear_result_btn);
        input.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (keyboardShown(input.getRootView())) {
                btn.setVisibility(View.GONE);
            } else {
                btn.setVisibility(View.VISIBLE);
            }
        });
        if (ShellCommands.is_err_run || ShellCommands.is_inp_run)
            progress.setVisibility(View.VISIBLE);
        else
            progress.setVisibility(View.GONE);

        hideKeyboard(this);
        result = findViewById(R.id.result);
        result.setMovementMethod(new ScrollingMovementMethod());

        path = getApplicationContext().getApplicationInfo().dataDir + "/" + "ffmpeg";
        sdcard = Environment.getExternalStorageDirectory().getPath();

        String arch = System.getProperty("os.arch");
        boolean is_arm = arch.equals("aarch64") || arch.contains("arm");
        if (!is_arm) {
            Toast.makeText(this, "Your device architecture not supported", Toast.LENGTH_LONG).show();
            finish();
        }
        if (first_time()) copyFFMPEG();
        shellCommands = new ShellCommands(response);
        uson = new USon(this, "ffmpeg.json");
        List list = uson.getList();
        if (list != null && !list.isEmpty())
            history.addAll(list);

    }

    public void eval(View V) {
        hideKeyboard(this);
        String cmd = input.getText().toString();
        if (cmd.trim().equals("")) {
            Toast.makeText(getApplicationContext(), "command is empty !!", Toast.LENGTH_SHORT).show();
            return;
        }
        model.setInput(cmd);
        final String[] commands = getArgs();
        if (history.contains(cmd))
            history.remove(cmd);
        history.add(cmd);

        history_index = history.size() - 1;
        shellCommands.run(commands);
    }

    private String[] getArgs() {
        String cmd = modify_command();
        ArrayList<String> args = new ArrayList<>(Arrays.asList(cmd.split(" ")));
        while (args.remove("")) ;
        //result.append(args.toString());
        return args.toArray(new String[args.size()]);
    }

    private String modify_command() {
        String cmd = input.getText().toString().trim();
        cmd = cmd.replace("ffmpeg", "");
        cmd = cmd.replace("/sdcard", sdcard);
        //cmd = "-loglevel error " + cmd;
        cmd = "-hide_banner -y " + cmd;
        cmd = path + " " + cmd;
        return cmd;
    }

    private ShellResponse response = new ShellResponse() {
        @Override
        public void onSuccess(final String r) {
            result.post(new Runnable() {
                @Override
                public void run() {
                    result.append(r);
                    result.append("\n");
                }
            });
        }

        @Override
        public void onError(final String r) {
            result.post(new Runnable() {
                @Override
                public void run() {
                    result.append(Html.fromHtml("<font color='red'>" + r + "</font>"));
                    result.append("\n");
                }
            });
        }

        @Override
        public void onFinish() {
            progress.post(new Runnable() {
                @Override
                public void run() {
                    progress.setVisibility(View.GONE);
                    result.append("\n");
                }
            });

        }

        @Override
        public void onStart() {
            progress.post(new Runnable() {
                @Override
                public void run() {
                    progress.setVisibility(View.VISIBLE);
                }
            });
        }
    };

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
        input.getText().insert(input.getSelectionEnd(), "/sdcard");
    }

    public void stop(View V) {
        ShellCommands.is_err_run = false;
        ShellCommands.is_inp_run = false;
        if (ShellCommands.process != null)
            ShellCommands.process.destroy();
        progress.setVisibility(View.GONE);
        result.append("\n" + "stop running process!");
    }

    public void undo(View v) {
        if (history_index == history.size())
            history_index--;
        if (history_index >= 0 && history_index < history.size()) {
            input.setText(history.get(history_index));
            if (history_index > 0) history_index--;
        }
    }

    public void redo(View v) {
        if (history_index == 0)
            history_index++;
        if (history_index <= history.size() - 1 && history_index >= 0) {
            input.setText(history.get(history_index));
            history_index++;
        }
    }

    public void clear(View v) {
        input.getText().clear();
        input.getText().insert(input.getSelectionEnd(), "-i ");
    }

    public void clear_result(View v) {
        result.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10003 && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<MediaFile> files = data.getParcelableArrayListExtra(FilePickerActivity.MEDIA_FILES);
            String path = files.get(0).getPath();
            path = path.replace(sdcard, "/sdcard");
            input.getText().insert(input.getSelectionEnd(), path);
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

    private boolean keyboardShown(View rootView) {
        final int softKeyboardHeight = 100;
        Rect r = new Rect();
        rootView.getWindowVisibleDisplayFrame(r);
        DisplayMetrics dm = rootView.getResources().getDisplayMetrics();
        int heightDiff = rootView.getBottom() - r.bottom;
        return heightDiff > softKeyboardHeight * dm.density;
    }

    @Override
    protected void onPause() {
        uson.putList(history);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        uson.putList(history);
        super.onDestroy();
    }
}