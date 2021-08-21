package euphoria.psycho.share;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class IntentShare {
    public static Intent createShareVideoIntent(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        return intent;
    }

    public static <T extends Activity> void launchActivity(Context context, Class<T> klass, Uri dataUri) {
        Intent intent = new Intent(context, klass);
        intent.setData(dataUri);
        context.startActivity(intent);
    }

    public static void launchChrome(Context context, String uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage("com.android.chrome");
        intent.setData(Uri.parse(uri));
        context.startActivity(intent);
    }
}
