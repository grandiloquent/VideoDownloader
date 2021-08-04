package euphoria.psycho.explorer;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.ThreadShare;
import euphoria.psycho.share.WebViewShare;

public class MainActivity extends Activity implements ClientInterface {
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;

    @Override
    public void onVideoUrl(String uri) {
        Share.setClipboardText(this, uri);
        mVideoUrl = uri;
    }

    //
    public boolean parsingXVideos() {
        String uri = mWebView.getUrl();
        if (uri.contains(".xvideos.")) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.show();
            XVideosShare.performTask(uri, value -> MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (value != null) {
                        Share.setClipboardText(MainActivity.this, value);
                        Toast.makeText(MainActivity.this, "视频地址已成功复制到剪切板.", Toast.LENGTH_LONG).show();
                        try {
                            mWebView.loadUrl("http://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        progressDialog.dismiss();
                    } else {
                        progressDialog.dismiss();
                    }
                }
            }));

            return true;
        }
        return false;
    }

    public boolean parsing91Porn() {
        String uri = mWebView.getUrl();
        if (uri.contains("91porn.com/")) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.show();
            Porn91Share.performTask(uri, value -> MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (value != null) {
                        String script = FileShare.readAssetString(MainActivity.this, "encode.js");
                        mWebView.evaluateJavascript(script + value, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {

                                Pattern pattern = Pattern.compile("(?<=src=').*?(?=')");
                                Matcher matcher = pattern.matcher(value);


                                Log.e("TAG/", "Debug: onReceiveValue, \n" + value);

                                if (matcher.find()) {


                                    Log.e("TAG/", "Debug: onReceiveValue, \n" + matcher.group());

                                    value = matcher.group();
                                    Share.setClipboardText(MainActivity.this, value);
                                    Toast.makeText(MainActivity.this, "视频地址已成功复制到剪切板.", Toast.LENGTH_LONG).show();
                                    try {
                                        mWebView.loadUrl("http://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }


                            }
                        });

                        progressDialog.dismiss();
                    } else {
                        progressDialog.dismiss();
                    }
                }
            }));

            return true;
        }
        return false;
    }

    private void addBookmark() {
        String name = mWebView.getTitle();
        String url = mWebView.getUrl();
        Bookmark bookmark = new Bookmark();
        bookmark.Name = name;
        bookmark.Url = url;
        mBookmarkDatabase.insert(bookmark);
    }

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
        File cacheDirectory = new File(new File(getCacheDir(), "Explorer"), "Cache");
        if (!cacheDirectory.isDirectory()) {
            cacheDirectory.mkdirs();
        }


        mWebView = findViewById(R.id.web);
        Helper.setWebView(mWebView, cacheDirectory.getAbsolutePath());

        mWebView.setWebViewClient(new CustomWebViewClient(this));
        mWebView.setWebChromeClient(new CustomWebChromeClient(this));

        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl("https://m.youtube.com");
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(mWebView, true);

        getWebViewVersion();


    }

    private void getWebViewVersion() {
        PackageInfo webViewPackageInfo;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            webViewPackageInfo = WebView.getCurrentWebViewPackage();
            Log.e("TAG/", "Debug: initialize, " + webViewPackageInfo.versionName);
        }
    }

    private void openUrlDialog(View v) {
        EditText editText = new EditText(v.getContext());
        AlertDialog alertDialog = new AlertDialog.Builder(v.getContext())
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    mWebView.loadUrl(editText.getText().toString());
                })
                .create();
        alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    private void refreshPage() {
        mWebView.clearCache(true);
        mWebView.reload();
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


        findViewById(R.id.add_link).setOnClickListener(this::openUrlDialog);
        findViewById(R.id.favorite_border).setOnClickListener(v -> {
            addBookmark();
        });
        findViewById(R.id.bookmark2_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this).setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) -> {
                            Intent intent = new Intent(MainActivity.this, BookmarkActivity.class);
                            startActivity(intent);
                        }
                );
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


        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            refreshPage();
        });


        findViewById(R.id.copy_button).setOnClickListener(v -> getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newPlainText(null, mWebView.getUrl())));
        findViewById(R.id.file_download).setOnClickListener(v -> {
            if (parsingXVideos()) return;
            if (parsing91Porn()) return;
            if (mWebView.getUrl().contains("youtube.com/watch")) {
                Intent intent = new Intent(this, SampleDownloadActivity.class);
                intent.setAction(Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
                startActivity(intent);
                return;
            }
            if (mVideoUrl != null) {
                try {
                    mWebView.loadUrl("https://hxz315.com?v=" + URLEncoder.encode(mVideoUrl, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }


        });
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
            boolean isLog = false;
            if (isLog) {
                Toast.makeText(MainActivity.this, consoleMessage.message(), Toast.LENGTH_LONG).show();
            }
            return super.onConsoleMessage(consoleMessage);
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

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mActivity.setTitle(title);

        }

        @Override
        public void onRequestFocus(WebView view) {
            super.onRequestFocus(view);

        }
    }
}
