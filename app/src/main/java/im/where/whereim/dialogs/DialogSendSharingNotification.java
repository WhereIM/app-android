package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import im.where.whereim.R;
import im.where.whereim.views.EmojiText;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogSendSharingNotification {
    public DialogSendSharingNotification(Context context, final Runnable callback){
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage(R.string.begin_sharing)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        callback.run();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        dialog.show();
    }
}
