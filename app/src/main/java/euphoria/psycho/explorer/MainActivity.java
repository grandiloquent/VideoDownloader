package euphoria.psycho.explorer;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.zip.GZIPInputStream;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.ThreadShare;
import euphoria.psycho.share.WebViewShare;
import euphoria.psycho.share.XVideosShare;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private File mCacheDirectory;
    private boolean mIsLog = false;
    private BookmarkDatabase mBookmarkDatabase;


    private void initialize() {
        setContentView(R.layout.activity_main);

        mBookmarkDatabase = new BookmarkDatabase(this);

        ThreadShare.postOnBackgroundThread(new Runnable() {
            @Override
            public void run() {
                // String value = request("https://www.xvideos.red/video-download/" + "63171345" + "/");

//


            }
        });
        mCacheDirectory = new File(new File(getCacheDir(), "Explorer"), "Cache");
        if (!mCacheDirectory.isDirectory()) {
            mCacheDirectory.mkdirs();
        }


        mWebView = findViewById(R.id.web);
//        mUrlEditText.setOnKeyListener(new OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                if (keyCode == KeyEvent.KEYCODE_ENTER) {
//                    String url = mUrlEditText.getText().toString();
//                    mWebView.loadUrl(url);
//                    return true;
//                }
//                return false;
//            }
//        });

        Helper.setWebView(mWebView, mCacheDirectory.getAbsolutePath());

        mWebView.setWebViewClient(new CustomWebViewClient(this));
        mWebView.setWebChromeClient(new CustomWebChromeClient(this));

        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl("https://www.xvideos.red/video63171345/_20_8_sp_5_pts-408_2_?sxcaf=4353LFJE75");
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        PackageInfo webViewPackageInfo;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            webViewPackageInfo = WebView.getCurrentWebViewPackage();
            Log.e("TAG/", "Debug: initialize, " + webViewPackageInfo.versionName);
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<String> needPermissions = new ArrayList<>();

        if (!PermissionShare.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);

        }
        if (needPermissions.size() > 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
                return;
            }
        }
        initialize();


        findViewById(R.id.bookmark2_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this);
                List<Bookmark> bookmarkList = mBookmarkDatabase.getBookmarkList();
                final ArrayAdapter<Bookmark> arrayAdapter = new ArrayAdapter<Bookmark>(MainActivity.this, android.R.layout.simple_list_item_1) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        text.setTextColor(Color.BLACK);
                        text.setText(bookmarkList.get(position).Name);
                        return view;
                    }
                };

                arrayAdapter.addAll(bookmarkList);

                builderSingle.setAdapter(arrayAdapter, (dialog, which) -> mWebView.loadUrl(bookmarkList.get(which).Url));
                builderSingle.show();
            }
        });


        findViewById(R.id.refresh_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.clearCache(true);
                mWebView.reload();
            }
        });

        findViewById(R.id.copy_button).setOnClickListener(v -> getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newPlainText(null, mWebView.getUrl())));
        findViewById(R.id.edit_button).setOnClickListener(v -> {
            if (mWebView.getUrl().contains("xvideos.red/")) {
                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.show();

                ThreadShare.postOnBackgroundThread(() -> {
                    String url = XVideosShare.getUrl(mWebView.getUrl(), null);
                    ThreadShare.postOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (url != null) {
                            mWebView.loadUrl(url);
                        }
                    });
                });
            }
        });
        findViewById(R.id.event_note_button).setOnClickListener(v -> mIsLog = !mIsLog);
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, WebViewShare.getFileType(MainActivity.this, url));
            WebViewShare.downloadFile(MainActivity.this, fileName, url, userAgent);
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        initialize();
    }


    // https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/webkit/WebViewClient.java


    private class CustomWebChromeClient extends WebChromeClient {
        private Activity mActivity;

        public CustomWebChromeClient(Activity activity) {
            mActivity = activity;
        }


        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {


            //Log.e("TAG/", "Debug: onConsoleMessage, \n" + consoleMessage.message());
            if (mIsLog) {
                Toast.makeText(MainActivity.this, consoleMessage.message(), Toast.LENGTH_LONG).show();
            }
            return super.onConsoleMessage(consoleMessage);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mActivity.setTitle(title);

        }

        @Override
        public void onRequestFocus(WebView view) {
            super.onRequestFocus(view);

        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            for (String permission : request.getResources()) {
                switch (permission) {
                    case "android.webkit.resource.AUDIO_CAPTURE": {
                        break;
                    }
                    case Manifest.permission.RECORD_AUDIO: {
                        break;
                    }
                    case Manifest.permission.CAMERA: {
                        break;
                    }
                }
            }
            request.grant(request.getResources());
        }
    }
}
