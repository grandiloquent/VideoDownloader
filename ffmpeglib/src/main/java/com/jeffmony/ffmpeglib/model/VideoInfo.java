package com.jeffmony.ffmpeglib.model;

public class VideoInfo {

    private String mName;
    private long mDuration;
    private int mWidth;
    private int mHeight;
    private String mVideoFormat;
    private String mAudioFormat;
    private String mContainerFormat;

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    //这个duration是微秒,需要除以1000 * 1000才能得到秒
    public void setDuration(long duration) {
        mDuration = duration;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setWidth(int width) {
        mWidth = width;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setHeight(int height) {
        mHeight = height;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setVideoFormat(String videoFormat) {
        mVideoFormat = videoFormat;
    }

    public String getVideoFormat() {
        return mVideoFormat;
    }

    public void setAudioFormat(String audioFormat) {
        mAudioFormat = audioFormat;
    }

    public String getAudioFormat() {
        return mAudioFormat;
    }

    public void setContainerFormat(String containerFormat) {
        mContainerFormat = containerFormat;
    }

    public String toString() {
        return "VideoInfo[" +
                "\nWidth=" + mWidth +
                "\nHeight=" + mHeight +
                "\nDuration=" + mDuration +
                "\nVideoDecoder=" + mVideoFormat +
                "\nAudioDecoder=" + mAudioFormat +
                "\nMediaContainer=" + mContainerFormat +
                "\n]";
    }

}
