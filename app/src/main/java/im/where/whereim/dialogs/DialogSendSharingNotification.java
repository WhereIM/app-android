package im.where.whereim.dialogs;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;

import im.where.whereim.CoreService;
import im.where.whereim.R;
import im.where.whereim.models.Channel;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogSendSharingNotification {
    public DialogSendSharingNotification(Activity activity, final Channel channel, final CoreService.CoreBinder binder){
        final Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), R.string.begin_sharing, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.send, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binder.sendNotification(channel, "begin_sharing");
            }
        });
        snackbar.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }
}
