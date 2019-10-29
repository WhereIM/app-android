package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogPublic {
    public interface Callback {
        void onSelected(boolean isPublic);
        void onCanceled();
    }
    public DialogPublic(Context context, final String title, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_public,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onCanceled();
                    }
                }).create();
        dialog_view.findViewById(R.id.is_public).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelected(true);
            }
        });
        dialog_view.findViewById(R.id.only_me).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelected(false);
            }
        });
        dialog.show();
    }
}
