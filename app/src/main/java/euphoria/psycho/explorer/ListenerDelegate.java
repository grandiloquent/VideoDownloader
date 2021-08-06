package euphoria.psycho.explorer;

import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;
import euphoria.psycho.share.DialogShare;

public class ListenerDelegate {
    private final MainActivity mMainActivity;
    private static final String HELP_URL = "https://lucidu.cn/article/jqdkgl";

    public ListenerDelegate(MainActivity mainActivity) {
        mMainActivity = mainActivity;
        mMainActivity.findViewById(R.id.refresh_button).setOnClickListener(this::onRefresh);
        mMainActivity.findViewById(R.id.copy_button).setOnClickListener(this::onCopy);
        mMainActivity.findViewById(R.id.favorite_border).setOnClickListener(this::onFavorite);
        mMainActivity.findViewById(R.id.bookmark2_button).setOnClickListener(this::onShowBookmark);
        mMainActivity.findViewById(R.id.help_outline).setOnClickListener(this::onHelp);

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
