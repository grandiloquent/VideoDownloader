package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.explorer.XVideosShare.Callback;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.share.WebViewShare;

public class MainActivity extends Activity implements ClientInterface {
    public static final String LAST_ACCESSED = "lastAccessed";
    public static final String HTTPS_LUCIDU_CN_ARTICLE_JQDKGL = "https://lucidu.cn/article/jqdkgl";
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;


    //
    public boolean parsing91Porn() {
        String uri = mWebView.getUrl();
        if (uri.contains("91porn.com/")) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.show();
            Porn91Share.performTask(uri, value -> MainActivity.this.runOnUiThread(() -> {
                if (value != null) {
                    String script = FileShare.readAssetString(MainActivity.this, "encode.js");
                    mWebView.evaluateJavascript(script + value, value1 -> {
                        if (value1 != null) {
                            get91PornVideo(value1);
                        }
                    });
                    progressDialog.dismiss();
                } else {
                    progressDialog.dismiss();
                }
            }));
            return true;
        }
        return false;
    }

    public boolean parsingXVideos() {
        String uri = mWebView.getUrl();
        if (uri.contains(".xvideos.")) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.show();
            XVideosShare.performTask(uri, value -> MainActivity.this.runOnUiThread(() -> {
                if (value != null) {
                    getVideo(value);
                    progressDialog.dismiss();
                } else {
                    progressDialog.dismiss();
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

    private boolean checkPermissions() {
        List<String> needPermissions = new ArrayList<>();
        if (!PermissionShare.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)) {
            needPermissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PermissionShare.checkSelfPermission(this, permission.INTERNET)) {
            needPermissions.add(permission.INTERNET);
        }
        if (needPermissions.size() > 0) {
            if (VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(needPermissions.toArray(new String[0]), REQUEST_PERMISSION);
                return true;
            }
        }
        return false;
    }

    private void get91PornVideo(String value) {
        Pattern pattern = Pattern.compile("(?<=src=').*?(?=')");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group();
            getVideo(value);
        }
    }

    private void getVideo(String value) {
        Share.setClipboardText(MainActivity.this, value);
        Toast.makeText(MainActivity.this, "视频地址已成功复制到剪切板.", Toast.LENGTH_LONG).show();
        try {
            mWebView.loadUrl("http://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void initialize() {
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        findViewById(R.id.add_link).setOnClickListener(this::openUrlDialog);
        findViewById(R.id.favorite_border).setOnClickListener(v -> {
            addBookmark();
        });
        findViewById(R.id.bookmark2_button).setOnClickListener(v -> {
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(MainActivity.this).setPositiveButton(
                    "修改",
                    (dialog, which) -> {
                        Intent intent = new Intent(MainActivity.this, BookmarkActivity.class);
                        startActivity(intent);
                    }
            );
            final ArrayAdapter<Bookmark> arrayAdapter = makeBookmarkAdapter();
            builderSingle.setAdapter(arrayAdapter, (dialog, which) -> mWebView.loadUrl(arrayAdapter.getItem(which).Url));
            builderSingle.show();
        });
        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            refreshPage();
        });
        findViewById(R.id.copy_button).setOnClickListener(v -> getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newPlainText(null, mWebView.getUrl())));
        findViewById(R.id.file_download).setOnClickListener(v -> {
            if (parsingXVideos()) return;
            if (parsing91Porn()) return;
            if (parseYouTube()) return;
            if (mVideoUrl != null) {
                try {
                    mWebView.loadUrl("https://hxz315.com?v=" + URLEncoder.encode(mVideoUrl, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        mWebView = findViewById(R.id.web);
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, WebViewShare.getFileType(MainActivity.this, url));
            WebViewShare.downloadFile(MainActivity.this, fileName, url, userAgent);
        });
        mBookmarkDatabase = new BookmarkDatabase(this);
        File cacheDirectory = new File(new File(getCacheDir(), "Explorer"), "Cache");
        if (!cacheDirectory.isDirectory()) {
            cacheDirectory.mkdirs();
        }
        Helper.setWebView(mWebView, cacheDirectory.getAbsolutePath());
        mWebView.setWebViewClient(new CustomWebViewClient(this));
        mWebView.setWebChromeClient(new CustomWebChromeClient(this));
        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        loadStartPage();
        WebViewShare.supportCookie(mWebView);
        setHelpListener();
    }

    private void loadStartPage() {
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl(PreferenceShare.getPreferences()
                    .getString(LAST_ACCESSED, "https://m.youtube.com"));
        }
    }

    private ArrayAdapter<Bookmark> makeBookmarkAdapter() {
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
        return arrayAdapter;
    }

    private void openDownloadDialog(String url) {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    WebViewShare.downloadFile(MainActivity.this, StringShare.substringAfterLast(StringShare.substringBeforeLast(url,"?"),"/"), url, NetShare.DEFAULT_USER_AGENT);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create();
        alertDialog.show();
    }

    private void openUrlDialog(View v) {
        EditText editText = new EditText(v.getContext());
        AlertDialog alertDialog = new Builder(v.getContext())
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (editText.getText().toString().contains("douyin.com")) {
                        ProgressDialog progressDialog = new ProgressDialog(this);
                        progressDialog.show();
                        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
                        Matcher matcher = pattern.matcher(editText.getText().toString());
                        if (matcher.find()) {
                            DouYinShare.performTask(matcher.group(), value -> {
                                MainActivity.this.runOnUiThread(() -> {
                                    if (value != null) {
                                        Share.setClipboardText(MainActivity.this, value);
                                        Toast.makeText(MainActivity.this, "视频地址已成功复制到剪切板.", Toast.LENGTH_LONG).show();
                                        mWebView.loadUrl(value);
                                        progressDialog.dismiss();
                                        openDownloadDialog(value);
                                    } else {
                                        progressDialog.dismiss();
                                    }
                                });
                            });
                        }

                    } else {
                        mWebView.loadUrl(editText.getText().toString());
                    }
                })
                .create();
        alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
    }

    private boolean parseYouTube() {
        if (mWebView.getUrl().contains("youtube.com/watch")) {
            Share.startYouTubeActivity(this, mWebView);
            return true;
        }
        return false;
    }

    private void refreshPage() {
        mWebView.clearCache(true);
        mWebView.reload();
    }

    private void setHelpListener() {
        findViewById(R.id.help_outline).setOnClickListener(v -> mWebView.loadUrl(HTTPS_LUCIDU_CN_ARTICLE_JQDKGL));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkPermissions()) return;
        initialize();
    }

    @Override
    protected void onPause() {
        PreferenceShare.putString(LAST_ACCESSED, mWebView.getUrl());
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

    @Override
    public void onVideoUrl(String uri) {
        Share.setClipboardText(this, uri);
        mVideoUrl = uri;
        Toast.makeText(this, "嗅探到视频地址：" + uri, Toast.LENGTH_LONG).show();
    }
 // 

}
