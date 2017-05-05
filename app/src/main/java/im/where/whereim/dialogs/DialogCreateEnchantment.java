package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogCreateEnchantment {
    public interface Callback {
        void onPositive(String name, boolean isPublic);
    }

    public DialogCreateEnchantment(Context context, final String title, final DialogCreateEnchantment.Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_enchantment_create,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
        et_name.setText(title);
        new AlertDialog.Builder(context)
                .setTitle(R.string.create_enchantment)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onPositive(et_name.getText().toString(), isPublic.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
