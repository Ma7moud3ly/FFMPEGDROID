package com.ma7moud3ly.ffmpegdroid;

public class ShellResponse implements ResponseHandler {

    @Override
    public void onSuccess(String result) {

    }

    @Override
    public void onError(String result) {

    }

    @Override
    public void onFinish() {

    }

    @Override
    public void onStart() {

    }

}

interface ResponseHandler {

    public void onSuccess(String result);

    public void onError(String result);

    public void onFinish();
    public void onStart();

}


