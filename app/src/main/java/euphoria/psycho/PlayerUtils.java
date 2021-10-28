package euphoria.psycho;

import android.content.Context;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource.Factory;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;

import static euphoria.psycho.videos.VideosHelper.USER_AGENT;

public class PlayerUtils {
    public static RenderersFactory buildRenderersFactory(
            Context context, boolean preferExtensionRenderer) {
        int extensionRendererMode =
                (preferExtensionRenderer
                        ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);
        return new DefaultRenderersFactory(context.getApplicationContext())
                .setExtensionRendererMode(extensionRendererMode);
    }

    public static CookieManager createCookieManager() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        return cookieManager;
    }

    public static Factory getHttpFactory(String[] headers) {
        Factory httpFactory = new Factory()
                .setUserAgent(USER_AGENT);
        if (headers != null) {
            HashMap<String, String> hashMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                if (i + 1 < headers.length)
                    hashMap.put(headers[i++], headers[i]);
            }
            httpFactory.setDefaultRequestProperties(hashMap);
        }
        return httpFactory;
    }
}
