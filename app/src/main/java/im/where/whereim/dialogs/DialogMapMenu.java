package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogMapMenu {
    public interface Callback {
        void onOpenIn();
        void onShareLocation();
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
        dialog_view.findViewById(R.id.open_in).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onOpenIn();
            }
        });
        dialog_view.findViewById(R.id.share_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onShareLocation();
            }
        });
        dialog_view.findViewById(R.id.create_enchantment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onCreateEnchantment();
            }
        });
        dialog_view.findViewById(R.id.create_marker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onCreateMarker();
            }
        });
        dialog_view.findViewById(R.id.forge_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                callback.onForgeLocation();
            }
        });
        dialog.show();
    }
}
