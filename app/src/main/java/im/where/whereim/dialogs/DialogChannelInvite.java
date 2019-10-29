package im.where.whereim.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import im.where.whereim.R;
import im.where.whereim.models.Channel;

/**
 * Created by buganini on 04/05/17.
 */

public class DialogChannelInvite {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    public DialogChannelInvite(final Context context, final Channel channel){
        try {
            int e = (int) (Math.min(Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels) * 0.75);

            BitMatrix matrix = new MultiFormatWriter().encode(channel.getLink(), BarcodeFormat.QR_CODE, e, e, null);

            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = matrix.get(x, y) ? BLACK : WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            final View dialog_view = LayoutInflater.from(context).inflate(R.layout.dialog_channel_invite,  null);
            final AlertDialog dialog = new AlertDialog.Builder(context)
                    .setView(dialog_view)
                    .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create();
            TextView label = dialog_view.findViewById(R.id.title);
            label.setText(channel.channel_name);

            Button send_invite_link = dialog_view.findViewById(R.id.send_invite_link);
            send_invite_link.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.action_invite));
                    i.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.invitation, channel.channel_name)+"\n"+channel.getLink());
                    context.startActivity(Intent.createChooser(i, context.getString(R.string.action_invite)));
                }
            });

            ImageView qr_code = dialog_view.findViewById(R.id.qr_code);
            qr_code.setImageBitmap(bitmap);
            dialog.show();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }
}
