package com.ma7moud3ly.ffmpegdroid;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MyViewModel extends ViewModel {
    private MutableLiveData<String> input = new MutableLiveData<>();

    public LiveData getInput() {
        return input;
    }

    public void setInput(String val) {
        input.setValue(val);
    }
}
