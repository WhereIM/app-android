package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.content.Context;
import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import im.where.whereim.R;

public class DialogEditSelf {
    public interface Callback {
        void onEditSelf(String name);
    }
    public DialogEditSelf(final Context context, String defaultName, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_self_edit,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        et_name.setText(defaultName);
        new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String name = et_name.getText().toString();
                        if(name.isEmpty())
                            return;
                        callback.onEditSelf(name);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
