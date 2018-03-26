package im.where.whereim;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import im.where.whereim.models.Image;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class ImageViewerActivity extends AppCompatActivity {
    private Handler handler = new Handler();
    private View loading;
    private SubsamplingScaleImageView imageView;
    private File previewDir;
    private File previewFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        loading = findViewById(R.id.loading);

        final Activity activity = this;
        final Intent intent = getIntent();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        TextView sender = findViewById(R.id.sender);
        sender.setText(intent.getStringExtra(Key.MATE_NAME));

        TextView time = findViewById(R.id.time);
        Timestamp ts = new Timestamp(intent.getLongExtra(Key.TIME, 0)*1000);
        String lymd = DateFormat.getDateInstance().format(ts);
        String eee = new SimpleDateFormat("EEE").format(ts);
        String hm = new SimpleDateFormat("HH:mm").format(ts);
        time.setText(getString(R.string.date_format, eee, lymd)+" "+hm);

        final String key = intent.getStringExtra(Key.KEY);
        final String ext = intent.getStringExtra(Key.EXTENSION);

        previewDir = new File(getCacheDir(), "preview");
        if(!previewDir.exists()){
            previewDir.mkdir();
        }
        previewFile = new File(previewDir, key);

        imageView = findViewById(R.id.image);

        if(previewFile.exists()){
            loading.setVisibility(View.GONE);
            imageView.setImage(ImageSource.uri(previewFile.getAbsolutePath()));
        }else{
            new Thread(){
                @Override
                public void run() {
                    OkHttpClient client = new OkHttpClient();
                    Image img = new Image();
                    img.key = key;
                    img.ext = ext;
                    Request request = new Request.Builder().url(Config.getPreview(img)).build();
                    try {
                        Response response = client.newCall(request).execute();
                        if(response.isSuccessful()){
                            BufferedSink sink = Okio.buffer(Okio.sink(previewFile));
                            sink.writeAll(response.body().source());
                            sink.close();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(!activity.isDestroyed() && !activity.isFinishing()                                                   ){
                                        loading.setVisibility(View.GONE);
                                        imageView.setImage(ImageSource.uri(previewFile.getAbsolutePath()));
                                    }
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    }
}
