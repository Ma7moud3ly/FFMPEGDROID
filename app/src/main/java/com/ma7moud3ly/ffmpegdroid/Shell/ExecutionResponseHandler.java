package com.ma7moud3ly.ffmpegdroid.Shell;

interface ExecutionResponseHandler {

    /**
     * on Success
     *
     * @param message complete output of the  command
     */
    public void onSuccess(String message);

    /**
     * on Progress
     *
     * @param message current output of  command
     */
    public void onProgress(String message);

    /**
     * on Failure
     *
     * @param message complete output of the  command
     */
    public void onFailure(String message);

    /**
     * on Start
     */
    public void onStart();

    /**
     * on Finish
     */
    public void onFinish();

}
