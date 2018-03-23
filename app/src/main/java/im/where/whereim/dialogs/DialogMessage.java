package im.where.whereim.dialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import im.where.whereim.CoreService;
import im.where.whereim.R;
import im.where.whereim.models.Message;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * Created by buganini on 23/03/18.
 */

public class DialogMessage {
    public static void show(final Context context, boolean income, final Message message, final CoreService.CoreBinder binder){
        if(income){
            if(message.deleted || message.hidden){
                return;
            }
            if("text".equals(message.type) || "rich".equals(message.type)){
                new DialogInMessage(context, message, binder);
            }else if("image".equals(message.type)){
                new DialogInImage(context, message, binder);
            }
        }else{
            if(message.deleted || message.hidden){
                return;
            }
            if("text".equals(message.type) || "rich".equals(message.type)){
                new DialogOutMessage(context, message, binder);
            }else if("image".equals(message.type)){
                new DialogOutImage(context, message, binder);
            }
        }
    }

    private static class DialogInMessage {
        public DialogInMessage(final Context context, final Message message, final CoreService.CoreBinder binder){
            final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_in_message,  null);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialog_view)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            Button copy = (Button) dialog_view.findViewById(R.id.copy);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    final ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                    clipBoard.setPrimaryClip(ClipData.newPlainText("message", message.getPlainText()));
                }
            });
            Button report = (Button) dialog_view.findViewById(R.id.report);
            report.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    binder.report(message);
                                }})
                            .setNegativeButton(R.string.cancel, null).show();
                }
            });
            dialog.show();
        }
    }

    private static class DialogInImage {
        public DialogInImage(final Context context, final Message message, final CoreService.CoreBinder binder){
            final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_in_image,  null);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialog_view)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            Button report = (Button) dialog_view.findViewById(R.id.report);
            report.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setPositiveButton(R.string.report, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    binder.report(message);
                                }})
                            .setNegativeButton(R.string.cancel, null).show();
                }
            });
            dialog.show();
        }
    }


    private static class DialogOutMessage {
        public DialogOutMessage(final Context context, final Message message, final CoreService.CoreBinder binder){
            final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_out_message,  null);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialog_view)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            Button copy = (Button) dialog_view.findViewById(R.id.copy);
            copy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    final ClipboardManager clipBoard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                    clipBoard.setPrimaryClip(ClipData.newPlainText("message", message.getPlainText()));
                }
            });
            Button delete = (Button) dialog_view.findViewById(R.id.delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    binder.delete(message);
                                }})
                            .setNegativeButton(R.string.cancel, null).show();
                }
            });
            dialog.show();
        }
    }

    private static class DialogOutImage {
        public DialogOutImage(final Context context, final Message message, final CoreService.CoreBinder binder){
            final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_out_image,  null);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialog_view)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
            Button delete = (Button) dialog_view.findViewById(R.id.delete);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    binder.delete(message);
                                }})
                            .setNegativeButton(R.string.cancel, null).show();
                }
            });
            dialog.show();
        }
    }
}
