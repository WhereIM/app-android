package im.where.whereim.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import im.where.whereim.GuestViewMgmtActivity;
import im.where.whereim.R;
import im.where.whereim.models.Channel;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogLocationServicePermissionRationale {
    public DialogLocationServicePermissionRationale(final Activity context){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_location_service_permission_rationale,  null);
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.need_location_permission)
                .setView(dialog_view)
                .setPositiveButton(R.string.action_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName()));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create();
        dialog.show();
    }
}
