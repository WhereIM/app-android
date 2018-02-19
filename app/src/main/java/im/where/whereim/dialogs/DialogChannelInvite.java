package im.where.whereim.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import im.where.whereim.ChannelActivity;
import im.where.whereim.GuestViewMgmtActivity;
import im.where.whereim.R;
import im.where.whereim.models.Channel;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogChannelInvite {
    public DialogChannelInvite(final Activity context, final Channel channel){
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
                new DialogChannelInviteQrCode(context, channel);
            }
        });
        Button send_invite_link = (Button) dialog_view.findViewById(R.id.send_invite_link);
        send_invite_link.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.action_invite));
                i.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invitation, channel.channel_name)+"\n"+channel.getLink());
                context.startActivity(Intent.createChooser(i, context.getString(R.string.action_invite)));
            }
        });
        Button guest_view = (Button) dialog_view.findViewById(R.id.guest_view);
        guest_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(context, GuestViewMgmtActivity.class);
                intent.putExtra("channel", channel.id);
                context.startActivity(intent);
            }
        });
        dialog.show();
    }
}
