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

public class DialogMapMenu {
    public interface Callback {
        void onOpenIn();
        void onShareLocation();
        void onSendPin();
        void onCreateEnchantment();
        void onCreateMarker();
        void onForgeLocation();
    }
    public DialogMapMenu(Context context, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_map_menu,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        Button open_in = (Button) dialog_view.findViewById(R.id.open_in);
        open_in.setText(TextUtils.concat(open_in.getText(), " ", new EmojiText(context, "⤴")));
        open_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onOpenIn();
            }
        });
        Button share_location = (Button) dialog_view.findViewById(R.id.share_location);
        share_location.setText(TextUtils.concat(share_location.getText(), " ", new EmojiText(context, "✉")));
        share_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onShareLocation();
            }
        });
        Button send_pin = (Button) dialog_view.findViewById(R.id.send_pin);
        send_pin.setText(TextUtils.concat(send_pin.getText(), " ", new EmojiText(context, "\uD83D\uDCAC")));
        send_pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onSendPin();
            }
        });
        Button create_enchantment = (Button) dialog_view.findViewById(R.id.create_enchantment);
        create_enchantment.setText(TextUtils.concat(create_enchantment.getText(), " ", new EmojiText(context, "⭕")));
        create_enchantment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onCreateEnchantment();
            }
        });
        Button create_marker = (Button) dialog_view.findViewById(R.id.create_marker);
        create_marker.setText(TextUtils.concat(create_marker.getText(), " ", new EmojiText(context, "\uD83D\uDCCD")));
        create_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onCreateMarker();
            }
        });
        Button forge_location = (Button) dialog_view.findViewById(R.id.forge_location);
        forge_location.setText(TextUtils.concat(forge_location.getText(), " ", new EmojiText(context, "\uD83D\uDE08")));
        forge_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onForgeLocation();
            }
        });
        dialog.show();
    }
}
