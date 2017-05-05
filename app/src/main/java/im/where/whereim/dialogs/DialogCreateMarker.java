package im.where.whereim.dialogs;

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

import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.models.Marker;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogCreateMarker {
    public interface Callback {
        void onCreateMarker(String name, boolean isPublic, JSONObject attr);
    }
    public DialogCreateMarker(final Context context, String defaultTitle, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_marker_create,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
        final Spinner icon = (Spinner) dialog_view.findViewById(R.id.icon);
        et_name.setText(defaultTitle);
        icon.setAdapter(new BaseAdapter() {
            private String[] icon = Marker.getIconList();

            class ViewHolder {
                ImageView icon;

                public ViewHolder(View view) {
                    view.setTag(this);
                    this.icon = (ImageView) view.findViewById(R.id.icon);
                }

                void setItem(String color){
                    icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), Marker.getIconResource(color), null));
                }
            }

            @Override
            public int getCount() {
                return icon.length;
            }

            @Override
            public String getItem(int position) {
                return icon[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View view, ViewGroup parent) {
                ViewHolder vh;
                if(view==null){
                    view = LayoutInflater.from(context).inflate(R.layout.icon_item, parent, false);
                    vh = new ViewHolder(view);
                }else{
                    vh = (ViewHolder) view.getTag();
                }
                vh.setItem(getItem(position));
                return view;
            }
        });
        new AlertDialog.Builder(context)
                .setTitle(R.string.create_marker)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONObject attr = new JSONObject();
                        try {
                            attr.put(Key.COLOR, icon.getSelectedItem());
                            callback.onCreateMarker(et_name.getText().toString(), isPublic.isChecked(), attr);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
