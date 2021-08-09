
package euphoria.psycho.explorer;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import euphoria.psycho.share.DownloadThread;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.KeyShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class DownloadActivity extends Activity {
    private String URI = "https://ccn.killcovid2021.com//m3u8/505212/505212.m3u8?st=WMDuO07zTRAYck8PUj-pZQ&e=1628554252";
    private TextView mTitle;
    private TextView mSubTitle;

    private Handler mHandler = new Handler();
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_activity);
        mTitle = findViewById(R.id.title);
        mSubTitle = findViewById(R.id.subtitle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    //
}
/*
https://github.com/aosp-mirror/platform_packages_providers_downloadprovider/blob/master/src/com/android/providers/downloads/DownloadThread.java
https://android.googlesource.com/platform/packages/apps/Gallery2/
*/
