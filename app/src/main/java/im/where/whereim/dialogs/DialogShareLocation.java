package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.google.common.net.UrlEscapers;

import im.where.whereim.Config;
import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogShareLocation {
    public DialogShareLocation(final Context context, String default_title, final double lat, final double lng){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_share_location,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        et_name.setText(default_title);
        new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String title = et_name.getText().toString();
                        String t = UrlEscapers.urlPathSegmentEscaper().escape(title!=null && !title.trim().isEmpty() ? title.trim() : "");
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.action_share));
                        i.putExtra(Intent.EXTRA_TEXT, title+"\n \n"+String.format(Config.WHERE_IM_URL, "here/"+lat+"/"+lng+(t.isEmpty()?"":"/"+t)));
                        context.startActivity(Intent.createChooser(i, context.getString(R.string.action_share)));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
