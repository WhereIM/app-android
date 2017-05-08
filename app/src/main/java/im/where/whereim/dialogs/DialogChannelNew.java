package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogChannelNew {
    public interface Callback {
        void onSelectJoinByQrCode();
        void onSelectCreateChannel();
    }
    public DialogChannelNew(Context context, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_new,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        Button scan_qr_code = (Button) dialog_view.findViewById(R.id.scan_qr_code);
        scan_qr_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelectJoinByQrCode();
            }
        });
        Button create_channel = (Button) dialog_view.findViewById(R.id.create_channel);
        create_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelectCreateChannel();
            }
        });
        dialog.show();
    }
}
