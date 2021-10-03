package euphoria.psycho.videos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.Helper;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;

public class DouYin extends BaseExtractor<String> {

    private String mVideoId;

    public DouYin(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }


    @Override
    protected String fetchVideoUri(String shareId) {
        return Native.fetchDouYin("/" + shareId);
    }

    @Override
    protected void processVideo(String videoUri) {
        mMainActivity.getWebView().loadUrl(videoUri);
        Helper.openDownloadDialog(mMainActivity, mVideoId, videoUri);
    }

    @Override
    protected String processUri(String inputUri) {
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        Matcher matcher = pattern.matcher(inputUri);
        if (matcher.find()) return matcher.group();
        return null;
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("(?<=douyin.com/).+(?=/)");
        if (pattern.matcher(uri).find()) {
            new DouYin(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}
