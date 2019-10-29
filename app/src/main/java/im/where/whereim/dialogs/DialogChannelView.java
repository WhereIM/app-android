package im.where.whereim.dialogs;

/**
 * Created by buganini on 10/06/17.
 */

import android.app.Activity;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import im.where.whereim.CoreService;
import im.where.whereim.R;
import im.where.whereim.Util;
import im.where.whereim.models.Channel;
import im.where.whereim.models.ChannelView;


public class DialogChannelView {
    public DialogChannelView(final Activity context, final CoreService.CoreBinder binder, final Channel channel, final ChannelView channelView){
        final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channelview,  null);
        final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
        final CheckBox chk_access_messages = (CheckBox) dialog_view.findViewById(R.id.access_messages);
        if(channelView != null){
            et_name.setText(channelView.name);
            chk_access_messages.setChecked(channelView.enable_message);
        }
        new AlertDialog.Builder(context)
                .setTitle(R.string.guest_view)
                .setView(dialog_view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ChannelView view = new ChannelView();
                        if(channelView != null){
                            view.id = channelView.id;
                        }else{
                            view.id = null;
                        }
                        view.name = et_name.getEditableText().toString();
                        view.enable_message = chk_access_messages.isChecked();
                        binder.putChannelView(channel, view);
                        Util.closeKeyboard(context);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.closeKeyboard(context);
                    }
                }).show();
    }
}
