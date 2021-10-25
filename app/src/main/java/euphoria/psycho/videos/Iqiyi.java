package euphoria.psycho.videos;

import android.content.Intent;
import android.os.Process;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import euphoria.psycho.explorer.HttpUtils;
import euphoria.psycho.explorer.MainActivity;
import euphoria.psycho.explorer.Native;
import euphoria.psycho.player.IqiyiActivity;

public class Iqiyi extends BaseExtractor<String[]> {
    public static Pattern MATCH_IQIYI = Pattern.compile("\\.iqiyi\\.com/v_");

    public Iqiyi(String inputUri, MainActivity mainActivity) {
        super(inputUri, mainActivity);
    }

    public static String getVideoAddress(String uri, Callback callback) {
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            String results = HttpUtils.getString(uri, null);
            if (results == null) {
                if (callback != null) {
                    callback.onVideoUri(null);
                }
                return;
            }
            try {
                JSONObject object = new JSONObject(results);
                if (object.has("l") && callback != null) {
                    callback.onVideoUri(object.getString("l"));
                    return;
                }

            } catch (JSONException ignored) {
            }
            if (callback != null) {
                callback.onVideoUri(null);
            }
        }).start();
        return null;
    }

    @Override
    protected String[] fetchVideoUri(String uri) {
        return Native.fetchIqiyi(uri);
    }

    @Override
    protected void processVideo(String[] videoUris) {
        if (videoUris == null || videoUris.length == 0) {
            Toast.makeText(mMainActivity, "无法解析视频", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(mMainActivity, IqiyiActivity.class);
        intent.putExtra(IqiyiActivity.KEY_PLAYLIST, videoUris);
        mMainActivity.startActivity(intent);

        // https://video-hw.xvideos-cdn.com/videos_new/3gp/8/9/d/xvideos.com_89d502a98c25c4c5a4c971eeb7e6fae2.mp4?e=1635107846&ri=1024&rs=85&h=b847150be273133ebb701d5ebbe72782

    }

    public interface Callback {
        void onVideoUri(String uri);
    }
}


