package euphoria.psycho.explorer;

import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.videos.AcFunShare;
import euphoria.psycho.videos.Bilibili;
import euphoria.psycho.videos.DouYin;
import euphoria.psycho.videos.Iqiyi;
import euphoria.psycho.videos.KuaiShou;
import euphoria.psycho.videos.MgTv;
import euphoria.psycho.videos.Porn91;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.TikTok;
import euphoria.psycho.videos.Twitter;
import euphoria.psycho.videos.XVideos;
import euphoria.psycho.videos.XVideosRedShare;
import euphoria.psycho.videos.YouTube;

public class ListenerDelegate {
    private final MainActivity mMainActivity;
    public static final String HELP_URL = "https://lucidu.cn/article/jqdkgl";

    public ListenerDelegate(MainActivity mainActivity) {
        mMainActivity = mainActivity;
        mMainActivity.findViewById(R.id.refresh_button).setOnClickListener(this::onRefresh);
        mMainActivity.findViewById(R.id.copy_button).setOnClickListener(this::onCopy);
        mMainActivity.findViewById(R.id.favorite_border).setOnClickListener(this::onFavorite);
        mMainActivity.findViewById(R.id.bookmark2_button).setOnClickListener(this::onShowBookmark);
        mMainActivity.findViewById(R.id.help_outline).setOnClickListener(this::onHelp);
        mainActivity.findViewById(R.id.file_download).setOnClickListener(this::onDownloadFile);
        mMainActivity.findViewById(R.id.add_link).setOnClickListener(this::onAddLink);
        mainActivity.findViewById(R.id.playlist_play).setOnClickListener(this::onPlaylist);
    }

    private void onPlaylist(View view) {
        Intent intent = new Intent(mMainActivity, VideoListActivity.class);
        mMainActivity.startActivity(intent);
    }

    private void onAddLink(View view) {
        DialogShare.createEditDialog(mMainActivity, "", uri -> {
            if (DouYin.handle(uri, mMainActivity)) {
                return;
            }
            if (KuaiShou.handle(uri, mMainActivity))
                return;
            if (YouTube.handle(uri, mMainActivity)) {
                return;
            }
            if (Twitter.handle(uri, mMainActivity)) {
                return;
            }
            if (TikTok.handle(uri, mMainActivity)) {
                return;
            }
            if (uri.equals("https://91porn.com/index.php") || uri.startsWith("https://91porn.com/v.php")) {
                new Porn91(uri, mMainActivity).fetchVideoList(uri);
                return;
            }
            if (!uri.startsWith("https://") && !uri.startsWith("http://"))
                mMainActivity.getWebView().loadUrl("https://" + uri);
            else
                mMainActivity.getWebView().loadUrl(uri);
        });
    }

    private void onDownloadFile(View view) {
        mMainActivity.getWebView().saveWebArchive(
                new File(mMainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "index.html").getAbsolutePath()
        );
        if (XVideosRedShare.parsingXVideos(mMainActivity, null)) return;
        String url = mMainActivity.getWebView().getUrl();
        if (Porn91.handle(url, mMainActivity)) {
            return;
        }
        if (YouTube.handle(url, mMainActivity)) {
            return;
        }
        if (Iqiyi.MATCH_IQIYI.matcher(url).find()) {
            new Iqiyi(url, mMainActivity).parsingVideo();
            return;
        }
        if (AcFunShare.parsingVideo(mMainActivity, null)) return;
        if (XVideos.handle(url, mMainActivity)) {
            return;
        }
        if (Bilibili.handle(url, mMainActivity)) {
            return;
        }
        if (MgTv.handle(url, mMainActivity)) {
            return;
        }
        if (PornHub.handle(url, mMainActivity)) {
            return;
        }
        if (PornOne.handle(url, mMainActivity)) {
            return;
        }
        if (Twitter.handle(url, mMainActivity)) {
            return;
        }
        if (url.equals("https://91porn.com/index.php") || url.startsWith("https://91porn.com/v.php")) {
            new Porn91(url, mMainActivity).fetchVideoList(url);
            return;
        }
        if (mMainActivity.getVideoUrl() != null) {
            try {
                mMainActivity.getWebView().loadUrl("https://hxz315.com?v=" + URLEncoder.encode(mMainActivity.getVideoUrl(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }


    private void onHelp(View view) {
        mMainActivity.getWebView().loadUrl(HELP_URL);
    }

    private ArrayAdapter<Bookmark> makeBookmarkAdapter() {
        List<Bookmark> bookmarkList = mMainActivity.getBookmarkDatabase().getBookmarkList();
        final ArrayAdapter<Bookmark> arrayAdapter = new ArrayAdapter<Bookmark>(mMainActivity, android.R.layout.simple_list_item_1) {
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

    private void onCopy(View view) {
        Share.setClipboardText(mMainActivity, mMainActivity.getWebView().getUrl());
    }

    private void onFavorite(View view) {
        String name = mMainActivity.getWebView().getTitle();
        String url = mMainActivity.getWebView().getUrl();
        DialogShare.createAlertDialogBuilder(mMainActivity, "询问", (dialog, which) -> {
            Bookmark bookmark = new Bookmark();
            bookmark.Name = name;
            bookmark.Url = url;
            mMainActivity.getBookmarkDatabase().insert(bookmark);
            dialog.dismiss();
        }, (dialog, which) -> {
            dialog.dismiss();
        })
                .setMessage(String.format("是否添\n\n\"%s\"\n\"%s\"\n\n为书签？", name, url))
                .show();
    }

    private void onRefresh(View v) {
        mMainActivity.getWebView().clearCache(true);
        mMainActivity.getWebView().reload();
    }

    private void onShowBookmark(View view) {
        Builder builderSingle = new Builder(mMainActivity).setPositiveButton(
                "修改",
                (dialog, which) -> {
                    Intent intent = new Intent(mMainActivity, BookmarkActivity.class);
                    mMainActivity.startActivity(intent);
                }
        );
        final ArrayAdapter<Bookmark> arrayAdapter = makeBookmarkAdapter();
        builderSingle.setAdapter(arrayAdapter, (dialog, which) -> mMainActivity.getWebView().loadUrl(arrayAdapter.getItem(which).Url));
        builderSingle.show();
    }
}
