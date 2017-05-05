package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogOpenIn {
    public DialogOpenIn(final Context context, final String title, final double lat, final double lng){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final LinearLayout dialog_view = new LinearLayout(context);
        dialog_view.setOrientation(LinearLayout.VERTICAL);
        Resources r = context.getResources();
        dialog_view.setPadding((int)r.getDimension(R.dimen.activity_vertical_margin), (int)r.getDimension(R.dimen.activity_horizontal_margin), (int)r.getDimension(R.dimen.activity_vertical_margin), (int)r.getDimension(R.dimen.activity_horizontal_margin));

        Button google_maps = new Button(context);
        google_maps.setAllCaps(false);
        google_maps.setText(R.string.google_maps);
        dialog_view.addView(google_maps);

        Button google_maps_navigation = new Button(context);
        google_maps_navigation.setAllCaps(false);
        google_maps_navigation.setText(R.string.google_maps_navigation);
        dialog_view.addView(google_maps_navigation);

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialog_view)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();

        google_maps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String label = "";
                if(title!=null && !title.trim().isEmpty()){
                    label = title.trim()+"@";
                }
                try {
                    String url = String.format(Locale.ENGLISH, "https://www.google.com/maps?q=%s%f,%f", URLEncoder.encode(label, "utf-8"), lat, lng);
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    context.startActivity(i);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        google_maps_navigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                String url = String.format(Locale.ENGLISH, "https://www.google.com/maps/dir/Current+Location/%f,%f", lat, lng);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                context.startActivity(i);
            }
        });

        dialog.show();
    }
}
