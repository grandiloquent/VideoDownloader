package euphoria.psycho.share;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogShare {

    public static AlertDialog.Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p,DialogInterface.OnClickListener n) {
        return new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("取消", n);
    }
}
