package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.CoreService;
import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.models.Marker;

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
