package im.where.whereim;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.util.IOUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

import im.where.whereim.dialogs.DialogMessage;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.Message;
import im.where.whereim.models.POI;
import im.where.whereim.models.PendingMessage;
import im.where.whereim.views.WimImageView;
import im.where.whereim.views.WimSpan;

import static android.app.Activity.RESULT_OK;

public class PaneMessenger extends BasePane {
    private final static int ACTION_PICKER = 0;
    private final static int ACTION_CAMERA = 1;


    public PaneMessenger() {
        // Required empty public constructor
    }

    @Override
    public void onShow() {
        updateUI();
        addListener();
    }

    @Override
    public void onHide() {
        removeListener();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Handler mHandler = new Handler();

    private Channel mChannel;
    private MessageCursorAdapter mAdapter;

    @Override
    protected void initChannel() {
        addListener();
    }

    @Override
    protected void deinitChannel() {
        removeListener();
    }

    private void addListener(){
        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.addMessageListener(mChannel, mMessageListener);
                        binder.setRead(mChannel);
                    }
                });
            }
        });
    }
    private void removeListener(){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mListView!=null)
                    mListView.setAdapter(null);
                mAdapter = null;
                if(mChannel!=null){
                    binder.removeMessageListener(mChannel, mMessageListener);
                }
            }
        });
    }

    private class MessageCursorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final static int TYPE_PENDING_TEXT = 0;
        private final static int TYPE_PENDING_IMAGE = 1;
        private final static int TYPE_IN_TEXT = 2;
        private final static int TYPE_OUT_TEXT = 3;
        private final static int TYPE_IN_IMAGE = 4;
        private final static int TYPE_OUT_IMAGE = 5;


        class PendingMessageViewHolder extends RecyclerView.ViewHolder {
            TextView message;

            public PendingMessageViewHolder(View itemView) {
                super(itemView);
                message = (TextView) itemView.findViewById(R.id.message);
            }
        }

        class PendingImageViewHolder extends RecyclerView.ViewHolder {
            WimImageView image;

            public PendingImageViewHolder(View itemView) {
                super(itemView);
                image = (WimImageView) itemView.findViewById(R.id.image);
            }
        }

        class InMessageViewHolder extends RecyclerView.ViewHolder {
            TextView date;
            TextView sender;
            TextView time;
            TextView message;
            Message msg;

            public InMessageViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                sender = (TextView) itemView.findViewById(R.id.sender);
                time = (TextView) itemView.findViewById(R.id.time);
                message = (TextView) itemView.findViewById(R.id.message);
                message.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                DialogMessage.show(getContext(), true, msg, binder);
                            }
                        });
                        return true;
                    }
                });
            }
        }

        class OutMessageViewHolder extends RecyclerView.ViewHolder {
            TextView date;
            TextView time;
            TextView message;
            Message msg;

            public OutMessageViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                time = (TextView) itemView.findViewById(R.id.time);
                message = (TextView) itemView.findViewById(R.id.message);
                message.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                DialogMessage.show(getContext(), false, msg, binder);
                            }
                        });
                        return true;
                    }
                });
            }
        }

        class InImageViewHolder extends RecyclerView.ViewHolder {
            TextView date;
            TextView sender;
            TextView time;
            WimImageView image;
            Message msg;

            public InImageViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                sender = (TextView) itemView.findViewById(R.id.sender);
                time = (TextView) itemView.findViewById(R.id.time);
                image = (WimImageView) itemView.findViewById(R.id.image);
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                Mate mate = binder.getChannelMate(mChannel.id, msg.mate_id);

                                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                                intent.putExtra(Key.MATE_NAME, mate == null ? "" : mate.getDisplayName());
                                intent.putExtra(Key.TIME, msg.time);
                                intent.putExtra(Key.KEY, msg.getImage().key);
                                intent.putExtra(Key.EXTENSION, msg.getImage().ext);
                                startActivity(intent);
                            }
                        });
                    }
                });
                image.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                DialogMessage.show(getContext(), true, msg, binder);
                            }
                        });
                        return true;
                    }
                });
            }
        }

        class OutImageViewHolder extends RecyclerView.ViewHolder {
            TextView date;
            TextView time;
            WimImageView image;
            Message msg;

            public OutImageViewHolder(View itemView) {
                super(itemView);
                date = (TextView) itemView.findViewById(R.id.date);
                time = (TextView) itemView.findViewById(R.id.time);
                image = (WimImageView) itemView.findViewById(R.id.image);
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                Mate mate = binder.getChannelMate(mChannel.id, msg.mate_id);

                                Intent intent = new Intent(getContext(), ImageViewerActivity.class);
                                intent.putExtra(Key.KEY, msg.getImage().key);
                                intent.putExtra(Key.EXTENSION, msg.getImage().ext);
                                intent.putExtra(Key.MATE_NAME, mate == null ? "" : mate.getDisplayName());
                                intent.putExtra(Key.TIME, msg.time);
                                startActivity(intent);
                            }
                        });
                    }
                });
                image.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                DialogMessage.show(getContext(), false, msg, binder);
                            }
                        });
                        return true;
                    }
                });
            }
        }

        private Message.BundledCursor mBundledCursor;
        public MessageCursorAdapter(Context context, Message.BundledCursor bc) {
            mBundledCursor = bc;
        }

        public void changeCursor(Message.BundledCursor bc) {
            mBundledCursor.cursor.close();
            mBundledCursor = bc;
            notifyDataSetChanged();
        }

        public Message.BundledCursor getCursor(){
            return mBundledCursor;
        }

        @Override
        public int getItemViewType(int position) {
            if(position < mBundledCursor.count) {
                mBundledCursor.cursor.moveToPosition(position);
                Message m = Message.parse(mBundledCursor.cursor);
                if(m.deleted || m.hidden){
                    if (mChannel.mate_id.equals(m.mate_id)) {
                        return TYPE_OUT_TEXT;
                    } else {
                        return TYPE_IN_TEXT;
                    }
                }
                if ("image".equals(m.type)) {
                    if (mChannel.mate_id.equals(m.mate_id)) {
                        return TYPE_OUT_IMAGE;
                    } else {
                        return TYPE_IN_IMAGE;
                    }
                } else {
                    if (mChannel.mate_id.equals(m.mate_id)) {
                        return TYPE_OUT_TEXT;
                    } else {
                        return TYPE_IN_TEXT;
                    }
                }
            }else{
                int pp = position - mBundledCursor.count;
                PendingMessage pm = mBundledCursor.pending.get(pp);
                if(pm.type.equals("rich")){
                    return TYPE_PENDING_TEXT;
                }else if("image".equals(pm.type)){
                    return TYPE_PENDING_IMAGE;
                }
            }
            return -1;
        }

        @Override
        public int getItemCount() {
            return mBundledCursor.count + mBundledCursor.pending.size();
        }

        @Override
        public long getItemId(int position) {
            if(position < mBundledCursor.count){
                mCurrentCursor.cursor.moveToPosition(position);
                return mCurrentCursor.cursor.getLong(0);
            }else{
                return mBundledCursor.count - position - 1;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch(viewType){
                case TYPE_PENDING_TEXT:
                    return new PendingMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.pending_message_item, parent, false));
                case TYPE_PENDING_IMAGE:
                    return new PendingImageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.pending_image_item, parent, false));
                case TYPE_IN_TEXT:
                    return new InMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.in_message_item, parent, false));
                case TYPE_OUT_TEXT:
                    return new OutMessageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.out_message_item, parent, false));
                case TYPE_IN_IMAGE:
                    return new InImageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.in_image_item, parent, false));
                case TYPE_OUT_IMAGE:
                    return new OutImageViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.out_image_item, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            CoreService.CoreBinder binder = getBinder();
            Message message = null;
            String lymd = null;
            String eee = null;
            String hm = null;
            PendingMessage pendingMessage = null;
            boolean showDate = false;
            if(position < mBundledCursor.count){
                Cursor cursor = mBundledCursor.cursor;
                cursor.moveToPosition(position);
                message = Message.parse(cursor);
                Timestamp time = new Timestamp(message.time*1000);
                lymd = DateFormat.getDateInstance().format(time);
                eee = new SimpleDateFormat("EEE", Locale.getDefault()).format(time);
                hm = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(time);
                if(position==0){
                    showDate = true;
                }else{
                    cursor.moveToPosition(position-1);
                    Message prev = Message.parse(cursor);
                    if(!new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(time).equals(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Timestamp(prev.time*1000)))){
                        showDate = true;
                    }
                    cursor.moveToPosition(position);
                }
            }else{
                int pp = position - mBundledCursor.count;
                pendingMessage = mBundledCursor.pending.get(pp);
            }
            switch(getItemViewType(position)){
                case TYPE_PENDING_TEXT: {
                    PendingMessageViewHolder vh = (PendingMessageViewHolder) holder;
                    vh.message.setText(Message.getText(getContext(), "rich", pendingMessage.payload, null));
                    vh.message.requestLayout();
                    return;
                }
                case TYPE_PENDING_IMAGE: {
                    PendingImageViewHolder vh = (PendingImageViewHolder) holder;
                    vh.image.setFile(pendingMessage.getFile());
                    return;
                }
                case TYPE_IN_TEXT: {
                    InMessageViewHolder ivh = (InMessageViewHolder) holder;
                    ivh.msg = message;
                    if (binder == null) {
                        ivh.date.setVisibility(View.GONE);
                        ivh.sender.setText(null);
                        ivh.message.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, message.mate_id);
                    ivh.sender.setText(mate == null ? "" : mate.getDisplayName());
                    ivh.message.setText(message.getText(getActivity(), clickedListener));
                    ivh.message.requestLayout();
                    ivh.time.setText(hm);
                    ivh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ivh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case TYPE_OUT_TEXT: {
                    OutMessageViewHolder ovh = (OutMessageViewHolder) holder;
                    ovh.msg = message;
                    if (binder == null) {
                        ovh.date.setVisibility(View.GONE);
                        ovh.message.setText(null);
                        ovh.time.setText(null);
                        return;
                    }
                    ovh.message.setText(message.getText(getActivity(), clickedListener));
                    ovh.message.requestLayout();
                    ovh.time.setText(hm);
                    ovh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ovh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case TYPE_IN_IMAGE: {
                    InImageViewHolder ivh = (InImageViewHolder) holder;
                    ivh.msg = message;
                    Glide.with(PaneMessenger.this).clear(ivh.image);
                    if (binder == null) {
                        ivh.date.setVisibility(View.GONE);
                        ivh.sender.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, message.mate_id);
                    ivh.sender.setText(mate == null ? "" : mate.getDisplayName());
                    ivh.image.setImage(message.getImage());
                    ivh.time.setText(hm);
                    ivh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ivh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case TYPE_OUT_IMAGE: {
                    OutImageViewHolder ovh = (OutImageViewHolder) holder;
                    ovh.msg = message;
                    Glide.with(PaneMessenger.this).clear(ovh.image);
                    if (binder == null) {
                        ovh.date.setVisibility(View.GONE);
                        ovh.time.setText(null);
                        return;
                    }
                    ovh.image.setImage(message.getImage());
                    ovh.time.setText(hm);
                    ovh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ovh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
            }
        }
    };

    private WimSpan.OnClickedListener clickedListener = new WimSpan.OnClickedListener(){

        @Override
        public void onClick(String url) {
            final String[] args = url.split("/");
            switch(args[0]){
                case "marker":
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            Marker m = binder.getChannelMarker(mChannel.id, args[1]);
                            if(m != null){
                                channelActivity.moveToMarker(m, true);
                            } else {
                                POI poi = new POI();
                                poi.latitude = Double.valueOf(args[2]);
                                poi.longitude = Double.valueOf(args[3]);
                                channelActivity.setPOI(poi);
                            }
                        }
                    });
                    break;
                case "pin":
                    POI poi = new POI();
                    poi.latitude = Double.valueOf(args[1]);
                    poi.longitude = Double.valueOf(args[2]);
                    channelActivity.setPOI(poi);
                    break;
            }
            setSizePolicy(ChannelActivity.PaneSizePolicy.FREE);
        }
    };

    private static class WimLinearLayoutManager extends  LinearLayoutManager {
        public WimLinearLayoutManager(Context context) {
            super(context);
        }

        private static final float MILLISECONDS_PER_INCH = 150f;

        public void slowlySmoothScrollToPosition(RecyclerView recyclerView, int position) {
            final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {

                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return super.computeScrollVectorForPosition(targetPosition);
                }

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                }
            };

            linearSmoothScroller.setTargetPosition(position);
            startSmoothScroll(linearSmoothScroller);
        }

    }

    private boolean messageViewEnd = true;
    private long maxMessageId = 0;
    private Message.BundledCursor mCurrentCursor;
    private RecyclerView mListView;
    private WimLinearLayoutManager layoutManager;
    private View mUnread;
    private View mBottom;
    private View mPin;
    private View mPicker;
    private View mCamera;
    private EditText mInput;
    private String mCurrentPhotoPath;

    private void takePhoto(){
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    "tmp",  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = image.getAbsolutePath();
            Uri photoURI = FileProvider.getUriForFile(getContext(),
                    "im.where.whereim.fileprovider",
                    image);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(intent, ACTION_CAMERA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean mShowCrosshair = false;
    @Override
    public boolean showCrosshair() {
        return mShowCrosshair;
    }

    private void setCrosshair(boolean enable){
        mShowCrosshair = enable;
        channelActivity.setCrosshair(enable);
    }

    private Integer origSize = null;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pane_messenger, container, false);

        final View input_bar = view.findViewById(R.id.input_bar);
        mPin = view.findViewById(R.id.pin);
        mPin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                withPin = !withPin;
                updateUI();
                setCrosshair(withPin);
                if(withPin){
                    origSize = channelActivity.getPaneSize();
                    channelActivity.setPaneSize(input_bar.getHeight());
                }else{
                    channelActivity.setPaneSize(origSize);
                }
            }
        });

        mBottom = view.findViewById(R.id.bottom);
        mBottom.setVisibility(View.GONE);
        mBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListView.scrollToPosition(mAdapter.getItemCount() - 1);
                mBottom.setVisibility(View.GONE);
                messageViewEnd = true;
            }
        });

        mUnread = view.findViewById(R.id.unread);
        mUnread.setVisibility(View.GONE);
        mUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListView.scrollToPosition(mAdapter.getItemCount() - 1);
                messageViewEnd = true;
            }
        });

        mPicker = view.findViewById(R.id.picker);
        mPicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, null), ACTION_PICKER);
            }
        });

        mCamera = view.findViewById(R.id.camera);
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 0);
                } else {
                    takePhoto();
                }
            }
        });

        mInput = (EditText) view.findViewById(R.id.input);
        mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                updateUI();
            }
        });

        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                channelActivity.closeKeyboard();
                final String message = mInput.getEditableText().toString();
                final QuadTree.LatLng location = withPin ? channelActivity.getMapCenter() : null;
                if(message.isEmpty()){
                    return;
                }
                mInput.setText(null);
                mInput.clearFocus();
                if(withPin && origSize!=null){
                    channelActivity.setPaneSize(origSize);
                }
                withPin = false;
                updateUI();
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                binder.sendMessage(channel, message, location);
                            }
                        });
                    }
                });
            }
        });

        mListView = (RecyclerView) view.findViewById(R.id.message);
        layoutManager = new WimLinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        mListView.setLayoutManager(layoutManager);
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {

            }

            private Runnable mShowBottom = new Runnable() {
                @Override
                public void run() {
                    mBottom.setVisibility(View.VISIBLE);
                }
            };

            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if(mCurrentCursor==null){
                    return;
                }
                messageViewEnd = view.getChildCount() == 0 || layoutManager.findLastVisibleItemPosition() == view.getAdapter().getItemCount() - 1 &&
                        view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight();
                if(messageViewEnd){
                    mUnread.setVisibility(View.GONE);
                }
                boolean showBottom = !messageViewEnd && mUnread.getVisibility()==View.GONE;
                if(showBottom){
                    mHandler.postDelayed(mShowBottom, 1000);
                }else{
                    mHandler.removeCallbacks(mShowBottom);
                    mBottom.setVisibility(View.GONE);
                }
                if(layoutManager.findFirstVisibleItemPosition()==0){
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            if(mCurrentCursor.loadMoreChannelData || mCurrentCursor.loadMoreUserData){
                                binder.requestMessage(mChannel, mCurrentCursor.loadMoreBefore, mCurrentCursor.loadMoreAfter);
                            }else{
                                binder.requestMessage(mChannel, mCurrentCursor.firstId, null);
                            }
                        }
                    });
                }
            }
        });
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                channelActivity.closeKeyboard();
                mInput.clearFocus();
                return false;
            }
        });

        return view;
    }

    private boolean withPin = false;
    private void updateUI(){
        boolean camera = !withPin;
        boolean picker = !withPin;
        boolean pin = true;
        if(mInput.hasFocus()){
            camera = false;
            picker = false;
            if(!withPin){
                pin = false;
            }
        }
        mPin.setVisibility(pin ? View.VISIBLE : View.GONE);
        mCamera.setVisibility(camera ? View.VISIBLE : View.GONE);
        mPicker.setVisibility(picker ? View.VISIBLE : View.GONE);
    }

    private long lastReload = 0;
    private Runnable mMessageListener = new Runnable() {
        @Override
        public void run() {
            if(System.currentTimeMillis() - lastReload > 1500){
                reloadData.run();
            }else{
                mHandler.removeCallbacks(reloadData);
                mHandler.postDelayed(reloadData, 1500);
            }
        }
    };

    private Runnable reloadData = new Runnable() {
        @Override
        public void run() {
            lastReload = System.currentTimeMillis();
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    final boolean origMessageViewEnd = messageViewEnd;
                    new Thread(){
                        @Override
                        public void run() {
                            mCurrentCursor = binder.getMessageCursor(mChannel);
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if(mAdapter==null){
                                        mAdapter = new MessageCursorAdapter(getActivity(), mCurrentCursor);
                                        mListView.setAdapter(mAdapter);
                                        maxMessageId = mCurrentCursor.lastId;
                                    }else{
                                        if(mAdapter.getCursor().count == 0){
                                            mAdapter.changeCursor(mCurrentCursor);
                                            return;
                                        }
                                        int originPosition = layoutManager.findFirstVisibleItemPosition();
                                        long originId;
                                        if(originPosition == -1){
                                            originId = -1;
                                        }else{
                                            originId = mAdapter.getItemId(originPosition);
                                        }
                                        Integer newPosition = null;
                                        int originTop = 0;
                                        if(originId >= 0){
                                            mCurrentCursor.cursor.moveToFirst();
                                            do{
                                                long id = mCurrentCursor.cursor.getLong(0);
                                                if(id >= originId){
                                                    newPosition = mCurrentCursor.cursor.getPosition();
                                                    break;
                                                }
                                            }while(mCurrentCursor.cursor.moveToNext());
                                            if(newPosition == null){
                                                newPosition = originPosition;
                                            }
                                            View v = mListView.getChildAt(0);
                                            originTop = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
                                        }
                                        mAdapter.changeCursor(mCurrentCursor);
                                        if(originId < 0){
                                            maxMessageId = mCurrentCursor.lastId;
                                        }else{
                                            layoutManager.scrollToPositionWithOffset(newPosition, originTop);
                                        }
                                        if(origMessageViewEnd){
                                            mHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    layoutManager.slowlySmoothScrollToPosition(mListView, mAdapter.getItemCount() - 1);
                                                }
                                            });
                                            messageViewEnd = origMessageViewEnd;
                                            maxMessageId = mCurrentCursor.lastId;
                                        }else{
                                            if(mCurrentCursor.lastId > maxMessageId) {
                                                mUnread.setVisibility(View.VISIBLE);
                                                mBottom.setVisibility(View.GONE);
                                            }
                                        }
                                    }
                                    if(isShowed()) {
                                        binder.setRead(mChannel);
                                    }
                                }
                            });
                        }
                    }.start();
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        if(mCurrentCursor!=null){
            mCurrentCursor.cursor.close();
        }
        mAdapter = null;
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(RESULT_OK != resultCode){
            return;
        }
        switch(requestCode){
            case ACTION_PICKER: {
                final ClipData clipData = data.getClipData();
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new BaseChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                for (int i = 0; i < clipData.getItemCount(); i++) {
                                    ClipData.Item item = clipData.getItemAt(i);
                                    Uri uri = item.getUri();

                                    final String filename = Util.getFileName(getContext(), uri);
                                    if (filename == null) {
                                        continue;
                                    }
                                    try {
                                        InputStream is = getContext().getContentResolver().openInputStream(uri);
                                        if (is == null) {
                                            continue;
                                        }
                                        final File temp = new File(getContext().getCacheDir(), UUID.randomUUID().toString().replace("-", "")+"-"+filename);
                                        OutputStream os = new FileOutputStream(temp);
                                        IOUtils.copy(is, os);
                                        is.close();
                                        os.close();
                                        binder.sendImage(channel, temp);
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                });
                break;
            }
            case ACTION_CAMERA: {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new BaseChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                File file = new File(mCurrentPhotoPath);
                                binder.sendImage(channel, file);
                            }
                        });
                    }
                });
                break;
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==0){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            }
        }
    }
}
