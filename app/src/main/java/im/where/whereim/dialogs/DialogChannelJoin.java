package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogChannelJoin {
    public interface Callback {
        void onDone(String mate_name);
    }
    public DialogChannelJoin(Context context, final String name, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_join,  null);
        final EditText et_mate_name = (EditText) dialog_view.findViewById(R.id.mate_name);
        et_mate_name.setText(name);
        new AlertDialog.Builder(context)
                .setTitle(R.string.join_channel)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String mate_name = et_mate_name.getText().toString();
                        callback.onDone(mate_name);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
