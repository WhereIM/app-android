package im.where.whereim.dialogs;

import android.app.Activity;
import android.support.design.widget.Snackbar;
import android.view.View;

import im.where.whereim.R;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogSendSharingNotification {
    public DialogSendSharingNotification(Activity activity, final Runnable callback){
        final Snackbar snackbar = Snackbar.make(activity.findViewById(android.R.id.content), R.string.begin_sharing, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.send, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.run();
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
