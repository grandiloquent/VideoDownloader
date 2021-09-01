package euphoria.psycho.explorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import euphoria.psycho.explorer.BookmarkDatabase.Bookmark;

public class BookmarkActivity extends Activity {
    private BookmarkAdapter mBookmarkAdapter;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bookmark_layout);
        mListView = (ListView) findViewById(R.id.list);
        BookmarkDatabase bookmarkDatabase = new BookmarkDatabase(this);
        // Use an existing ListAdapter that will map an array of strings to TextViews
        mBookmarkAdapter = new BookmarkAdapter(this,
                android.R.layout.simple_list_item_1, bookmarkDatabase.getBookmarkList().toArray(new Bookmark[0]));
        mListView.setAdapter(mBookmarkAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setTextFilterEnabled(true);
        mListView.setOnItemClickListener((parent, v, position, id) -> {
            EditText editText = new EditText(v.getContext());
            Bookmark bookmark = mBookmarkAdapter.getItem(position);
            editText.setText(bookmark.Name);
            AlertDialog alertDialog = new AlertDialog.Builder(v.getContext())
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (editText.getText() == null) {
                            bookmarkDatabase.deleteBookmark(bookmark);
                        } else {
                            String newBookmark = editText.getText().toString().trim();
                            if (newBookmark.length() == 0) {
                                bookmarkDatabase.deleteBookmark(bookmark);
                            } else {
                                bookmark.Name = newBookmark;
                                bookmarkDatabase.update(bookmark);
                            }
                        }
                        mBookmarkAdapter.setBookmarks(bookmarkDatabase.getBookmarkList().toArray(new Bookmark[0]));
                        dialog.dismiss();
                    })
                    .create();
            alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertDialog.show();
        });
        mBookmarkAdapter.notifyDataSetChanged();
    }

    private static class ViewHolder {
        ImageView icon;
        TextView text;
    }

    private static class BookmarkAdapter extends ArrayAdapter<Bookmark> {
        private final LayoutInflater mLayoutInflater;
        private Bookmark[] mBookmarks;

        public BookmarkAdapter(Context context, int resource, Bookmark[] bookmarks) {
            super(context, resource, bookmarks);
            mLayoutInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mBookmarks = bookmarks;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.bookmark_row, parent, false);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.row_text);
                holder.icon = (ImageView) convertView.findViewById(R.id.row_icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.text.setText(mBookmarks[position].Name);
            return convertView;
        }

        @Override
        public int getCount() {
            return mBookmarks.length;
        }

        public void setBookmarks(Bookmark[] bookmarks) {
            mBookmarks = bookmarks;
            notifyDataSetChanged();
        }
    }
}