package im.where.whereim;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FCMReceiverService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();

        Intent intent = new Intent(FCMReceiverService.this, CoreService.class);
        intent.putExtra(Key.KEY, data.get(Key.KEY));
        intent.putExtra(Key.CHANNEL, data.get(Key.CHANNEL));
        intent.putExtra(Key.TITLE, data.get(Key.TITLE));
        intent.putExtra(Key.ARGS, data.get(Key.ARGS));
        startService(intent);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        Intent intent = new Intent(FCMReceiverService.this, CoreService.class);
        intent.putExtra(Key.TOKEN, token);
        startService(intent);
    }
}
