package com.jeffmony.ffmpeglib.listener;

public interface OnVideoCompositeListener {

    /**
     * 合成成功回调
     */
    void onComplete();

    /**
     * 合成错误回调
     * @param errCode 具体的错误码可以细分错误类型
     */
    void onError(int errCode);

}
