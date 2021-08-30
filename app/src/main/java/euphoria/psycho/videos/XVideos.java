package euphoria.psycho.videos;

import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.StringShare;

import static euphoria.psycho.videos.VideosHelper.getString;
import static euphoria.psycho.videos.VideosHelper.launchDialog;

public class XVideos extends BaseVideoExtractor<List<Pair<String, String>>> {


    public XVideos(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("xvideos\\.com/video\\d+");
        if (pattern.matcher(uri).find()) {
            new XVideos(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }



    private void parseHls(String hlsUri, List<Pair<String, String>> videoList) {
        String hls = getString(hlsUri, null);
        if (hls == null) return;
        String[] pieces = hls.split("\n");
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#EXT-X-STREAM-INF")) {
                String name = StringShare.substring(pieces[i], "NAME=\"", "\"");
                String url = StringShare.substringBeforeLast(hlsUri, "/") + "/" + pieces[i + 1];
                videoList.add(Pair.create(name, url));
                i++;
            }
        }

    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        List<Pair<String, String>> videoList = new ArrayList<>();
        String htmlCode = getString(uri, null);
        if (htmlCode == null) return null;
        String low = StringShare.substring(htmlCode, "html5player.setVideoUrlLow('", "'");
        if (low != null) {
            videoList.add(Pair.create("标清", low));
        }
        String high = StringShare.substring(htmlCode, "html5player.setVideoUrlHigh('", "'");
        if (high != null) {
            videoList.add(Pair.create("高清", high));
        }
        String hls = StringShare.substring(htmlCode, "html5player.setVideoHLS('", "'");
        if (hls != null) {
            parseHls(hls, videoList);
        }
        return videoList;
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(List<Pair<String, String>> videoUriList) {
        try {
            launchDialog(mMainActivity, videoUriList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

