package euphoria.psycho.videos;

import android.util.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.Logger;
import euphoria.psycho.share.StringShare;

public class PornOne extends BaseVideoExtractor<List<Pair<String, String>>> {
    private static Pattern MATCH_PORNONE = Pattern.compile("pornone\\.com/.+/.+/\\d+");

    protected PornOne(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected List<Pair<String, String>> fetchVideoUri(String uri) {
        String response = getString(uri, null);
        if (response == null) {
            return null;
        }
        List<String> videos = StringShare.substringCodes(response, ".mp4", 15);
        if (videos == null) {
            return null;
        }
        List<Pair<String, String>> videoUriList = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            videoUriList.add(Pair.create(
                    StringShare.substring(videos.get(i), "_", "x"),
                    videos.get(i)
            ));
        }
        return videoUriList;
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

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_PORNONE.matcher(uri).find()) {
            new PornOne(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
