package euphoria.psycho.explorer;

import android.content.Context;

public interface ClientInterface {
    String[] getVideoList();

    void onVideoUrl(String uri);

    boolean shouldOverrideUrlLoading(String uri);

    Context getContext();
}