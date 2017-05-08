package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import im.where.whereim.ChannelListActivity;
import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogCreateChannel {
    public interface Callback {
        void onDone(String channel_name, String mate_name);
    }

    public DialogCreateChannel(Context context, final String username, final DialogCreateChannel.Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_create,  null);
        final EditText et_channel_name = (EditText) dialog_view.findViewById(R.id.channel_name);
        final EditText et_mate_name = (EditText) dialog_view.findViewById(R.id.mate_name);
        et_mate_name.setText(username);
        new AlertDialog.Builder(context)
                .setTitle(R.string.create_channel)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String channel_name = et_channel_name.getText().toString();
                        String mate_name = et_mate_name.getText().toString();
                        callback.onDone(channel_name, mate_name);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
