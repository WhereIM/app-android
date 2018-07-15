package im.where.whereim;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import im.where.whereim.models.Channel;

public class NewChannelActivity extends BaseActivity {
    private View loading;
    private EditText et_channel_name;
    private EditText et_mate_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_channel);

        loading = findViewById(R.id.loading);
        et_channel_name = findViewById(R.id.channel_name);
        et_mate_name = findViewById(R.id.mate_name);


        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                et_mate_name.setText(binder.getUserName());
            }
        });

        final Button create_channel = findViewById(R.id.create_channel);
        create_channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String channel_name = et_channel_name.getText().toString();
                final String mate_name = et_mate_name.getText().toString();
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        create_channel.setEnabled(false);
                        loading.setVisibility(View.VISIBLE);
                        binder.createChannel(channel_name, mate_name);
                    }
                });
            }
        });
        final TextView scan_qr_code = findViewById(R.id.scan_qr_code);
        scan_qr_code.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NewChannelActivity.this, ScannerActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private int origChannelCount = -1;
    private Runnable channelListChangedListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    List<Channel> channels = binder.getChannelList();
                    int n = channels.size();
                    if(origChannelCount == -1){ // init channel count
                        processDeepLink();
                    } else {
                        if(n != origChannelCount){ // channel count changed
                            if(origChannelCount==0){ // switch to the new & only channel
                                Intent intent = new Intent(NewChannelActivity.this, ChannelActivity.class);
                                intent.putExtra(Key.CHANNEL, channels.get(0).id);
                                startActivity(intent);
                            }
                            finish();
                        }
                    }
                    origChannelCount = n;
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.addChannelListChangedListener(channelListChangedListener);
            }
        });
    }

    @Override
    protected void onPause() {
        if(mBinder != null){
            mBinder.removeChannelListChangedListener(channelListChangedListener);
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if(origChannelCount > 0){
            super.onBackPressed();
        }
    }
}

