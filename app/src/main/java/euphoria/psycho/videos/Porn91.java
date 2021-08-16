package euphoria.psycho.videos;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.NativeShare;

public class Porn91 extends BaseVideoExtractor<String> {

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
        byte[] buffer = new byte[128];
        byte[] buf = uri.getBytes(StandardCharsets.UTF_8);
        int result = NativeShare.get91Porn(buf, buf.length, buffer, 128);
        if (result == 0) {
            return null;
        }
        return new String(buffer, 0, result);
    }

    @Override
    protected String processUri(String inputUri) {
        return inputUri;
    }

    @Override
    protected void processVideo(String videoUri) {
        viewVideoBetter(mMainActivity, videoUri);
    }
}