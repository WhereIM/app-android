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

public class DialogChannelInvite {
    public interface Callback {
        void onSelectGenerateQRCode();
        void onSelectSendInviteLink();
    }
    public DialogChannelInvite(Context context, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_invite,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        Button generate_qr_code = (Button) dialog_view.findViewById(R.id.generate_qr_code);
        generate_qr_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelectGenerateQRCode();
            }
        });
        Button send_invite_link = (Button) dialog_view.findViewById(R.id.send_invite_link);
        send_invite_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSelectSendInviteLink();
            }
        });
        dialog.show();
    }
}
