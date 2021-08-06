package euphoria.psycho.explorer;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.webkit.URLUtil;
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
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.FileShare;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.PermissionShare;
import euphoria.psycho.share.PreferenceShare;
import euphoria.psycho.share.WebViewShare;

public class MainActivity extends Activity implements ClientInterface {
    public static final String HELP_URL = "https://lucidu.cn/article/jqdkgl";
    public static final String LAST_ACCESSED = "lastAccessed";
    private static final int REQUEST_PERMISSION = 66;
    private WebView mWebView;
    private BookmarkDatabase mBookmarkDatabase;
    private String mVideoUrl;

    public static String matchTikTokVideoId(String input) {
        if (input == null) return null;
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public boolean parsing91Porn() {
        String uri = mWebView.getUrl();
        if (uri.contains("91porn.com/")) {
            ProgressDialog progressDialog = createProgressDialog();
            Porn91Share.performTask(uri, value -> MainActivity.this.runOnUiThread(() -> {
                if (value != null) {
                    String script = FileShare.readAssetString(MainActivity.this, "encode.js");
                    mWebView.evaluateJavascript(script + value, value1 -> {
                        if (value1 != null) {
                            get91PornVideo(value1);
                        }
                    });
                }
                progressDialog.dismiss();
            }));
            return true;
        }
        return false;
    }

    public boolean parsingXVideos() {
        String uri = mWebView.getUrl();
        if (uri.contains(".xvideos.")) {
            ProgressDialog progressDialog = createProgressDialog();
            XVideosShare.performTask(uri, value -> MainActivity.this.runOnUiThread(() -> {
                if (value != null) {
                    getVideo(value);
                } else {
                    Toast.makeText(this, "无法解析视频", Toast.LENGTH_LONG).show();
                }
                progressDialog.dismiss();
            }));
            return true;
        }
        return false;
    }

    private void addBookmark() {
        String name = mWebView.getTitle();
        String url = mWebView.getUrl();
        DialogShare.createAlertDialogBuilder(this, "询问", (dialog, which) -> {
            Bookmark bookmark = new Bookmark();
            bookmark.Name = name;
            bookmark.Url = url;
            mBookmarkDatabase.insert(bookmark);
            dialog.dismiss();
        }, (dialog, which) -> {
            dialog.dismiss();
        })
                .setMessage(String.format("是否添\n\n\"%s\"\n\"%s\"\n\n为书签？", name, url))
                .show();

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

    private void copyUrl(String value) {
        Share.setClipboardText(MainActivity.this, value);
        Toast.makeText(MainActivity.this, "视频地址已成功复制到剪切板.", Toast.LENGTH_LONG).show();
    }

    private File createCacheDirectory() {
        File cacheDirectory = new File(new File(getCacheDir(), "Explorer"), "Cache");
        Logger.d(String.format("createCacheDirectory: 览器储存目录 = %s", cacheDirectory.getAbsolutePath()));
        if (!cacheDirectory.isDirectory()) {
            boolean result = cacheDirectory.mkdirs();
            if (!result) {
                Logger.d(String.format("createCacheDirectory: 创建目录 %s 失败", cacheDirectory.getAbsolutePath()));
            }
        }
        return cacheDirectory;
    }

    private ProgressDialog createProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("解析...");
        progressDialog.show();
        return progressDialog;
    }

    private void get91PornVideo(String value) {
        Pattern pattern = Pattern.compile("(?<=src=').*?(?=')");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group();
            getVideo(value);
        } //
    }

    private void getVideo(String value) {
        copyUrl(value);
        try {
            String uri = "https://hxz315.com/?v=" + URLEncoder.encode(value, "UTF-8");
            DialogShare.createAlertDialogBuilder(this, "询问", (dialog, which) -> {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(uri));
                startActivity(Intent.createChooser(intent, "打开视频链接"));
            }, (dialog, which) -> {
                mWebView.loadUrl(uri);
                dialog.dismiss();
            })
                    .setMessage("是否使用浏览器打开视频链接")
                    .show();
        } catch (UnsupportedEncodingException ignored) {
        }
    }

    //
    private void initialize() {
        setContentView(R.layout.activity_main);
        PreferenceShare.initialize(this);
        findViewById(R.id.add_link).setOnClickListener(this::openUrlDialog);
        findViewById(R.id.favorite_border).setOnClickListener(v -> {
            addBookmark();
        });
        setBookmark();
        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            refreshPage();
        });
        findViewById(R.id.copy_button).setOnClickListener(v -> getSystemService(ClipboardManager.class)
                .setPrimaryClip(ClipData.newPlainText(null, mWebView.getUrl())));
        setDownloadVideo();
        mWebView = findViewById(R.id.web);
        mBookmarkDatabase = new BookmarkDatabase(this);
        setWebView();
        loadStartPage();
        setHelpListener();
    }

    private void loadStartPage() {
        if (getIntent().getData() != null) {
            mWebView.loadUrl(getIntent().getData().toString());
        } else {
            mWebView.loadUrl(PreferenceShare.getPreferences()
                    .getString(LAST_ACCESSED, HELP_URL));
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

    private void openDownloadDialog(String videoId, String videoUrl) {
        new AlertDialog.Builder(this)
                .setTitle("询问")
                .setMessage("是否下载视频？")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    WebViewShare.downloadFile(MainActivity.this, videoId + ".mp4", videoUrl, NetShare.DEFAULT_USER_AGENT);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void openUrlDialog(View v) {
        EditText editText = new EditText(v.getContext());
        AlertDialog alertDialog = new Builder(v.getContext())
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (editText.getText().toString().contains("douyin.com")) {
                        ProgressDialog progressDialog = createProgressDialog();
                        String id = matchTikTokVideoId(editText.getText().toString());
                        if (id == null) return;
                        DouYinShare.performTask(id, value -> {
                            MainActivity.this.runOnUiThread(() -> {
                                if (value != null) {
                                    mWebView.loadUrl(value);
                                    openDownloadDialog(id, value);
                                }
                                progressDialog.dismiss();

                            });
                        });
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

    private void setBookmark() {
        findViewById(R.id.bookmark2_button).setOnClickListener(v -> {
            Builder builderSingle = new Builder(MainActivity.this).setPositiveButton(
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
    }

    private void setDownloadVideo() {
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
    }

    private void setHelpListener() {
        findViewById(R.id.help_outline).setOnClickListener(v -> mWebView.loadUrl(HELP_URL));
    }

    private void setWebView() {
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, WebViewShare.getFileType(MainActivity.this, url));
            WebViewShare.downloadFile(MainActivity.this, fileName, url, userAgent);
        });
        WebViewShare.setWebView(mWebView, createCacheDirectory().getAbsolutePath());
        mWebView.setWebViewClient(new CustomWebViewClient(this));
        mWebView.setWebChromeClient(new CustomWebChromeClient(this));
        mWebView.setDownloadListener(Helper.getDownloadListener(this));
        WebViewShare.supportCookie(mWebView);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (checkPermissions()) return;
        initialize();
    }

    @Override
    protected void onPause() {
        if (mWebView != null)
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
        for (int i = 0; i < permissions.length; i++) {
            boolean result = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            if (!result) {
                Toast.makeText(this, "缺少必要权限，程序无法运行", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        initialize();
    }

    @Override
    public void onVideoUrl(String uri) {
        Share.setClipboardText(this, uri);
        mVideoUrl = uri;
    }


}
