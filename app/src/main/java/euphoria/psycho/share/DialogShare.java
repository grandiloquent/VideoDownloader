package euphoria.psycho.share;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

public class DialogShare {

    public static Builder createAlertDialogBuilder(Context context, String title, DialogInterface.OnClickListener p, DialogInterface.OnClickListener n) {
        return new Builder(context)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, p)
                .setNegativeButton("取消", n);
    }


    public interface Callback {
        void run(String string);
    }

    public static AlertDialog createEditDialog(Context context, String title, Callback callback) {
        EditText editText = new EditText(context);
        AlertDialog alertDialog = new Builder(context)
                .setTitle("请输入视频网页地址")
                .setView(editText)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    if (callback != null)
                        callback.run(editText.getText().toString());
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create();
        alertDialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
        return alertDialog;
    }

    public static ProgressDialog createProgressDialog(Context context) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("解析...");
        progressDialog.show();
        return progressDialog;
    }
}
