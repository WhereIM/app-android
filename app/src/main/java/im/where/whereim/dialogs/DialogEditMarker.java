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

import java.util.Arrays;

import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.models.Marker;

public class DialogEditMarker {
    public interface Callback {
        void onEditMarker(String name, boolean isPublic, JSONObject attr);
    }
    public DialogEditMarker(final Context context, String defaultTitle, final String defaultColor, final boolean isShared, final Callback callback){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_marker,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        final CheckBox isPublic = (CheckBox) dialog_view.findViewById(R.id.ispublic);
        final Spinner icon = (Spinner) dialog_view.findViewById(R.id.icon);
        et_name.setText(defaultTitle);
        final String[] iconList = Marker.getIconList();
        icon.setAdapter(new BaseAdapter() {
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
                return iconList.length;
            }

            @Override
            public String getItem(int position) {
                return iconList[position];
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
        icon.setSelection(Arrays.asList(iconList).indexOf(defaultColor));
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
                        try {
                            JSONObject attr = new JSONObject();
                            attr.put(Key.COLOR, icon.getSelectedItem());
                            callback.onEditMarker(et_name.getText().toString(), isShared || isPublic.isChecked(), attr);
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
