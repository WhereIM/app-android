package im.where.whereim;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.where.whereim.models.Channel;

public class ScannerActivity extends BaseActivity {
    private RelativeLayout frame;
    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private boolean matched = false;
    private View loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        frame = (RelativeLayout) findViewById(R.id.frame);

        loading = findViewById(R.id.loading);

        int e = (int) (Math.min(Resources.getSystem().getDisplayMetrics().widthPixels, Resources.getSystem().getDisplayMetrics().heightPixels) * 0.75);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(e, e);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        surfaceView = new SurfaceView(this);
        surfaceView.setLayoutParams(params);

        BarcodeDetector barcodeDetector =
                new BarcodeDetector.Builder(this)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();

        final Pattern pattern = Pattern.compile("^.*?where.im/(channel/[A-Fa-f0-9]{32})$");
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();

                if (!matched && barcodes.size() != 0) {
                    String s = barcodes.valueAt(0).displayValue;
                    final Matcher m = pattern.matcher(s);
                    if(m.matches()){
                        matched = true;

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                frame.removeView(surfaceView);
                                loading.setVisibility(View.VISIBLE);
                                processLink(m.group(1));
                            }
                        });
                    }
                }
            }
        });

        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(e, e)
                .build();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cameraSource.start(holder);
                } catch (IOException ie) {
                    Log.e("CAMERA SOURCE", ie.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        if (ActivityCompat.checkSelfPermission(ScannerActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ScannerActivity.this, new String[]{android.Manifest.permission.CAMERA}, 0);
        } else {
            ready();
        }
    }

    private void ready(){
        frame.addView(surfaceView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==0){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ready();
            }
        }
    }

    private Runnable channelListChangedListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    if(pending_joined_channel != null){
                        for(Channel c: binder.getChannelList()){
                            if(pending_joined_channel.equals(c.id)){
                                Intent intent = new Intent(ScannerActivity.this, ChannelActivity.class);
                                startActivity(intent);
                                finish();
                                return;
                            }
                        }
                    }
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
}
