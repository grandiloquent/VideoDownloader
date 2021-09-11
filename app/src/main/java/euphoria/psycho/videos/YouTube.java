package euphoria.psycho.videos;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Pair;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Share;
import euphoria.psycho.share.Logger;

public class YouTube extends BaseExtractor<List<Pair<String, YtFile>>> {
    protected YouTube(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected List<Pair<String, YtFile>> fetchVideoUri(String uri) {
        final FutureTask<Object> ft = new FutureTask<>(() -> {
        }, new Object());
        List<Pair<String, YtFile>> files = new ArrayList<>();
        new YouTubeExtractor(mMainActivity) {
            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                if (ytFiles == null) {
                    ft.run();
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);
                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        files.add(Pair.create(vMeta.getTitle(), ytFile));
                    }
                }
                ft.run();
            }

        }.extract(uri);
        try {
            ft.get();
        } catch (Exception e) {
            Logger.d(String.format("fetchVideoUri: %s", e.getMessage()));

        }
        return files;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(List<Pair<String, YtFile>> videoList) {
        String[] names = new String[videoList.size()];
        for (int i = 0; i < names.length; i++) {
            YtFile ytFile = videoList.get(i).second;
            String btnText = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
                    ytFile.getFormat().getAudioBitrate() + " kbit/s" :
                    ytFile.getFormat().getHeight() + "p";
            btnText += (ytFile.getFormat().isDashContainer()) ? " dash" : "";
            names[i] = btnText;
        }
        new AlertDialog.Builder(mMainActivity)
                .setItems(names, (dialog, which) -> {
                    viewVideo(videoList.get(which).first, videoList.get(which).second);
                })
                .show();
    }

    public void viewVideo(String videoTitle, YtFile ytFile) {
        String filename;
        if (videoTitle.length() > 55) {
            filename = videoTitle.substring(0, 55) + "." + ytFile.getFormat().getExt();
        } else {
            filename = videoTitle + "." + ytFile.getFormat().getExt();
        }
        filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
        downloadFromUrl(ytFile.getUrl(), videoTitle, filename);
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Share.setClipboardText(mMainActivity,youtubeDlUrl);
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager manager = (DownloadManager) mMainActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("下载", n);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("youtube\\.com/watch\\?v=");
        if (pattern.matcher(uri).find()) {
            new YouTube(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
