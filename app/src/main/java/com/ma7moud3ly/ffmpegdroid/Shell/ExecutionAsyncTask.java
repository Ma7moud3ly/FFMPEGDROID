package com.ma7moud3ly.ffmpegdroid.Shell;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class ExecutionAsyncTask extends AsyncTask<Void, String, CommandResult> {

    private final String[] cmd;
    private final ExecutionResponse mExecuteResponseHandler;
    private final ShellCommand shellCommand;
    private final long timeout;
    private long startTime;
    private Process process;
    private String output = "";

    public ExecutionAsyncTask(String[] cmd, long timeout, ExecutionResponse mExecuteResponseHandler) {
        this.cmd = cmd;
        this.timeout = timeout;
        this.mExecuteResponseHandler = mExecuteResponseHandler;
        this.shellCommand = new ShellCommand();
    }

    @Override
    protected void onPreExecute() {
        startTime = System.currentTimeMillis();
        if (mExecuteResponseHandler != null) {
            mExecuteResponseHandler.onStart();
        }
    }

    @Override
    protected CommandResult doInBackground(Void... params) {
        try {
            process = shellCommand.run(cmd);
            if (process == null) {
                return CommandResult.getDummyFailureResponse();
            }
            checkAndUpdateProcess();
            return CommandResult.getOutputFromProcess(process);
        } catch (TimeoutException e) {
            e.printStackTrace();
            return new CommandResult(false, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Util.destroyProcess(process);
        }
        return CommandResult.getDummyFailureResponse();
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values != null && values[0] != null && mExecuteResponseHandler != null) {
            mExecuteResponseHandler.onProgress(values[0]);
        }
    }

    @Override
    protected void onPostExecute(CommandResult commandResult) {
        if (mExecuteResponseHandler != null) {
            output += commandResult.output;
            if (commandResult.success) {
                mExecuteResponseHandler.onSuccess(output);
            } else {
                mExecuteResponseHandler.onFailure(output);
            }
            mExecuteResponseHandler.onFinish();
        }
    }

    private void checkAndUpdateProcess() throws TimeoutException, InterruptedException {
        while (!Util.isProcessCompleted(process)) {
            // checking if process is completed
            if (Util.isProcessCompleted(process)) {
                return;
            }

            // Handling timeout
            if (timeout != Long.MAX_VALUE && System.currentTimeMillis() > startTime + timeout) {
                throw new TimeoutException("timed out");
            }

            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    if (isCancelled()) {
                        return;
                    }

                    output += line + "\n";
                    publishProgress(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isProcessCompleted() {
        return Util.isProcessCompleted(process);
    }

    public void destroyProcess() {
        Util.destroyProcess(process);
    }

}
