package com.ma7moud3ly.ffmpegdroid;

import android.text.Html;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellCommands {
    public static Process process;
    public static boolean is_err_run;
    public static boolean is_inp_run;
    private ShellResponse response;

    public ShellCommands(ShellResponse response) {
        this.response = response;
    }

    public void run(final String commands[]) {
        try {
            if (is_err_run || is_inp_run) {
                response.onError("current process is running !");
                return;
            }
            process = Runtime.getRuntime().exec(commands);
            response.onStart();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    inStream(process.getInputStream());
                    errStream(process.getErrorStream());
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            is_inp_run = false;
            is_err_run = false;
            response.onFinish();
        }
    }

    void inStream(final InputStream inputStream) {
        try {
            is_inp_run = true;
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (is_inp_run && (line = r.readLine()) != null) {
                response.onSuccess(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is_inp_run = false;
                if (is_inp_run == false && is_err_run == false) response.onFinish();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void errStream(final InputStream inputStream) {
        try {
            is_err_run = true;
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (is_err_run && (line = r.readLine()) != null) {
                if (response != null) response.onError(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is_err_run = false;
                if (is_inp_run == false && is_err_run == false) response.onFinish();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

