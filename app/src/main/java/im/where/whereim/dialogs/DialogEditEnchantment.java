package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import im.where.whereim.R;


public class DialogEditEnchantment {
    public interface Callback {
        void onEditEnchantment(String name, boolean isPublic);
    }
    public DialogEditEnchantment(final Context context, String defaultTitle, final boolean isShared, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_enchantment,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
        et_name.setText(defaultTitle);
        if(isShared){
            isPublic.setVisibility(View.GONE);
        }else{
            isPublic.setChecked(false);
        }
        new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callback.onEditEnchantment(et_name.getText().toString(), isShared || isPublic.isChecked());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
