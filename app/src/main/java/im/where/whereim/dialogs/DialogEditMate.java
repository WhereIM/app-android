package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import im.where.whereim.CoreService;
import im.where.whereim.R;

public class DialogEditMate {
    public interface Callback {
        void onEditMate(String name);
    }
    public DialogEditMate(final Context context, String matename, String defaultName, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_mate_edit,  null);
        final TextView mate_name = (TextView) dialog_view.findViewById(R.id.mate_name);
        final EditText et_user_mate_name = (EditText) dialog_view.findViewById(R.id.user_mate_name);
        mate_name.setText(matename);
        et_user_mate_name.setText(defaultName);
        new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String user_mate_name = et_user_mate_name.getText().toString();
                        callback.onEditMate(user_mate_name);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
