package euphoria.psycho.videos;

import android.content.Intent;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.share.NetShare;
import euphoria.psycho.share.StringShare;
import euphoria.psycho.tasks.VideoService;

public class Porn91 extends BaseVideoExtractor<String> {

    private static Pattern MATCH_91PORN = Pattern.compile("(?<=<a href=\")https://91porn.com/view_video.php\\?[^\"]+(?=\")");

    public Porn91(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        Pattern pattern = Pattern.compile("91porn.com/view_video.php\\?viewkey=[a-zA-Z0-9]+");
        if (pattern.matcher(uri).find()) {
            new Porn91(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }

    @Override
    protected String fetchVideoUri(String uri) {
        // We need to use fake IPs to
        // get around the 50 views per day
        // for non members limitation
        String response = getString(uri, new String[][]{
                {"Referer", "https://91porn.com"},
                {"X-Forwarded-For", NetShare.randomIp()}
        });
        if (response == null) {
//        byte[] buffer = new byte[128];
//        byte[] buf = uri.getBytes(StandardCharsets.UTF_8);
//        int result = NativeShare.get91Porn(buf, buf.length, buffer, 128);
//        if (result == 0) {
            return null;
        }
        // maybe that is the fast way to
        // extract the encoded code which
        // contains the real video uri
        String encoded = StringShare.substring(response, "document.write(strencode2(\"", "\"));");
        String htm = null;
        try {
            // translate from the javascript code 'window.unescape'
            htm = URLDecoder.decode(encoded, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (htm != null) {
            // the decoded code is some html
            // we need to locate the video uri
            return StringShare.substring(htm, "src='", "'");
        }
        return null;
        //return new String(buffer, 0, result);
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        viewVideoBetter(mMainActivity, videoUri);
    }

    public void fetchVideoList(String uri) {
        String response = getString(uri, null);
        Matcher matcher = MATCH_91PORN.matcher(response);
        List<String> videoList = new ArrayList<>();
        while (matcher.find()) {
            videoList.add(matcher.group());
        }
        Intent service = new Intent(mMainActivity, VideoService.class);
        service.putExtra(VideoService.KEY_VIDEO_LIST, videoList.toArray(new String[0]));
        mMainActivity.startService(service);
    }
}
