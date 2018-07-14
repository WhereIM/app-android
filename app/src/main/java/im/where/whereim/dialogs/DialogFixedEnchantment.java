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

public class DialogFixedEnchantment {
    public DialogFixedEnchantment(Context context){
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(TextUtils.concat(new EmojiText(context, "ℹ"), " ", context.getString(R.string.fixed_geofence)))
                .setMessage(R.string.fixed_geofence_desc)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }
}
