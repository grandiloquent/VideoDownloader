package euphoria.psycho.videos;

import android.content.Intent;
import android.widget.Toast;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.explorer.WebActivity;

public class Ck52 extends BaseExtractor<String> {
    private static final Pattern MATCH_52CK = Pattern.compile("/vodplay/[\\d-]+\\.html");

    protected Ck52(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    @Override
    protected String fetchVideoUri(String uri) {
        return Native.fetch57Ck(uri);
    }

    @Override
    protected void processVideo(String videoUri) {
        if (videoUri.length() == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        Intent starter = new Intent(mMainActivity, WebActivity.class);
        starter.putExtra("extra.URI", videoUri);
        mMainActivity.startActivity(starter);
    }

    public static boolean handle(String uri, MainActivity mainActivity) {
        if (MATCH_52CK.matcher(uri).find()) {
            new Ck52(uri, mainActivity).parsingVideo();
            return true;
        }
        return false;
    }
}