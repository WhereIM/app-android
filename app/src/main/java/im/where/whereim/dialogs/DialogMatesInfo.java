package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import im.where.whereim.R;
import im.where.whereim.views.EmojiText;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogMatesInfo {
    public DialogMatesInfo(Context context){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_mates_info,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(TextUtils.concat(new EmojiText(context, "ℹ"), " ", context.getString(R.string.location_status)))
                .setView(dialog_view)
                .setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }
}
