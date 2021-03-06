package euphoria.psycho.explorer;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cocosw.bottomsheet.BottomSheet;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.bilibili.Bilibili;
import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.share.DialogShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.tencent.Tencent;
import euphoria.psycho.videos.AcFun;
import euphoria.psycho.videos.CCTV;
import euphoria.psycho.videos.DouYin;
import euphoria.psycho.videos.Iqiyi;
import euphoria.psycho.videos.KuaiShou;
import euphoria.psycho.videos.MgTv;
import euphoria.psycho.videos.PornHub;
import euphoria.psycho.videos.PornOne;
import euphoria.psycho.videos.TikTok;
import euphoria.psycho.videos.Twitter;
import euphoria.psycho.videos.XVideos;
import euphoria.psycho.videos.XiGua;
import euphoria.psycho.videos.YouTube;

public class ListenerDelegate {
    private final MainActivity mMainActivity;
    public static final String HELP_URL = "https://lucidu.cn/article/jqdkgl";

    public ListenerDelegate(MainActivity mainActivity) {
        mMainActivity = mainActivity;
        mainActivity.findViewById(R.id.file_download).setOnClickListener(this::onDownloadFile);
        mMainActivity.findViewById(R.id.add_link).setOnClickListener(this::onAddLink);
        mainActivity.findViewById(R.id.more_vert).setOnClickListener(this::onMore);
    }

    private void onMore(View view) {
        new BottomSheet.Builder(mMainActivity)
                .title("菜单")
                .grid()
                .sheet(R.menu.list)
                .listener((dialog, which) -> {
                    if (R.id.favorite_border == which) {
                        onFavorite();
                    } else if (R.id.bookmark2_button == which) {
                        onShowBookmark();
                    } else if (R.id.refresh_button == which) {
                        onRefresh();
                    } else if (R.id.copy_button == which) {
                        onCopy();
                    } else if (R.id.playlist_play == which) {
                        onPlaylist();
                    } else if (R.id.help_outline == which) {
                        onHelp();
                    } else {
                        start(mMainActivity);
                    }
                }).show();
    }

    public static void start(Context context) {
        Intent starter = new Intent(context, SettingsActivity.class);
        context.startActivity(starter);
    }

    private void onPlaylist() {
        Intent intent = new Intent(mMainActivity, VideoListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
            if (Tencent.handle(uri, mMainActivity)) {
                return;
            }
            if (XiGua.handle(uri, mMainActivity)) {
                return;
            }
//            if (uri.equals("https://91porn.com/index.php") || uri.startsWith("https://91porn.com/v.php")) {
//                new Porn91(uri, mMainActivity).fetchVideoList(uri);
//                return;
//            }
            if (!uri.startsWith("https://") && !uri.startsWith("http://"))
                mMainActivity.getWebView().loadUrl("https://" + uri);
            else
                mMainActivity.getWebView().loadUrl(uri);
        });
    }

    private void onDownloadFile(View view) {
        String url = mMainActivity.getWebView().getUrl();
        if (StringShare.matchOne(new String[]{
                "/vodplay/[\\d-]+\\.html",
                "(?<=<a href=\")https://91porn.com/view_video.php\\?[^\"]+(?=\")",
                "xvideos\\.com/video\\d+"
        }, url)) {
            Intent starter = new Intent(mMainActivity, WebActivity.class);
            starter.putExtra("extra.URI", url);
            mMainActivity.startActivity(starter);
            return;
        }
        if (YouTube.handle(url, mMainActivity)) {
            return;
        }
        if (Iqiyi.MATCH_IQIYI.matcher(url).find()) {
            new Iqiyi(url, mMainActivity).parsingVideo();
            return;
        }
        if (AcFun.handle(url, mMainActivity)) return;
        if (Bilibili.handle(url, mMainActivity)) {
            return;
        }
        if (MgTv.handle(url, mMainActivity)) {
            return;
        }
        if (CCTV.handle(url, mMainActivity)) {
            return;
        }
        if (PornHub.handle(url, mMainActivity)) {
            return;
        }
        if (PornOne.handle(url, mMainActivity)) {
            return;
        }
        if (Tencent.handle(url, mMainActivity)) {
            return;
        }
        if (Twitter.handle(url, mMainActivity)) {
            return;
        }
        if (XiGua.handle(url, mMainActivity)) {
            return;
        }
//        if (url.equals("https://91porn.com/index.php") || url.startsWith("https://91porn.com/v.php")) {
//            new Porn91(url, mMainActivity).fetchVideoList(url);
//            return;
//        }
        if (mMainActivity.getVideoUrl() != null) {
            mMainActivity.getWebView().loadUrl(mMainActivity.getVideoUrl());
        }
    }

    private void onHelp() {
        mMainActivity.getWebView().loadUrl(HELP_URL);
        mMainActivity.getWebView().saveWebArchive(
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "1.mhtml").getAbsolutePath());

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

    private void onCopy() {
        Share.setClipboardText(mMainActivity, mMainActivity.getWebView().getUrl());
    }

    private void onFavorite() {
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
        }).setMessage(String.format("是否添\n\n\"%s\"\n\"%s\"\n\n为书签？", name, url))
                .show();
    }

    private void onRefresh() {
        mMainActivity.getWebView().clearCache(true);
        mMainActivity.getWebView().reload();
        String url = mMainActivity.getWebView().getUrl();
        Pattern pattern = Pattern.compile("xvideos\\.com/video\\d+");
        if (pattern.matcher(url).find()) {
            new Thread(() -> XVideos.fetchVideos(url)).start();
        }


    }

    private void onShowBookmark() {
        Builder builderSingle = new Builder(mMainActivity).setPositiveButton(
                R.string.edit,
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