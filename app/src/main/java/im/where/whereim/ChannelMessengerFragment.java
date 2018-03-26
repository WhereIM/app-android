package im.where.whereim;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.amazonaws.util.IOUtils;
import com.bumptech.glide.Glide;

import org.json.JSONException;

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
import im.where.whereim.models.PendingMessage;
import im.where.whereim.views.WimImageView;
import im.where.whereim.views.WimSpan;

import static android.app.Activity.RESULT_OK;

public class ChannelMessengerFragment extends BaseFragment {
    private final static int ACTION_PICKER = 0;
    private final static int ACTION_CAMERA = 1;


    public ChannelMessengerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onShow() {
        updateUI();
        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.addMessageListener(channel, mMessageListener);
                        binder.setRead(mChannel);
                    }
                });
            }
        });

    }

    @Override
    public void onHide() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mChannel!=null){
                    binder.removeMessageListener(mChannel, mMessageListener);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Handler mHandler = new Handler();

    private Channel mChannel;
    private MessageCursorAdapter mAdapter;
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
                    Glide.with(ChannelMessengerFragment.this).clear(ivh.image);
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
                    Glide.with(ChannelMessengerFragment.this).clear(ovh.image);
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
            final ChannelActivity activity = (ChannelActivity) getActivity();
            final String[] args = url.split("/");
            switch(args[0]){
                case "marker":
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            Marker m = binder.getChannelMarker(mChannel.id, args[1]);
                            if(m != null){
                                activity.moveToMarker(m, true);
                            } else {
                                activity.moveToPin(new QuadTree.LatLng(Double.valueOf(args[2]), Double.valueOf(args[3])));
                            }
                        }
                    });
                    break;
                case "pin":
                    activity.moveToPin(new QuadTree.LatLng(Double.valueOf(args[1]), Double.valueOf(args[2])));
                    break;
            }
        }
    };

    private boolean messageViewEnd = true;
    private long maxMessageId = 0;
    private Message.BundledCursor mCurrentCursor;
    private RecyclerView mListView;
    private LinearLayoutManager layoutManager;
    private View mUnread;
    private View mPin;
    private View mPicker;
    private View mCamera;
    private EditText mInput;
    public QuadTree.LatLng pinLocation;
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

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_messenger, container, false);

        mPin = view.findViewById(R.id.pin);

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
                Activity activity = getActivity();
                InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                final String message = mInput.getEditableText().toString();
                final QuadTree.LatLng location = pinLocation;
                if(message.isEmpty()){
                    return;
                }
                mInput.setText(null);
                mInput.clearFocus();
                pinLocation = null;
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
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        mListView.setLayoutManager(layoutManager);
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView view, int scrollState) {

            }

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
                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                View focus = getActivity().getCurrentFocus();
                if(focus != null){
                    inputManager.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
                mInput.clearFocus();
                return false;
            }
        });

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;
            }
        });
        return view;
    }

    private void updateUI(){
        boolean camera = pinLocation==null;
        boolean picker = pinLocation==null;
        mPin.setVisibility(pinLocation==null ? View.GONE : View.VISIBLE);
        if(mInput.hasFocus()){
            camera = false;
            picker = false;
        }
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
                public void onBinderReady(CoreService.CoreBinder binder) {
                    final boolean origMessageViewEnd = messageViewEnd;
                    mCurrentCursor = binder.getMessageCursor(mChannel);
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
                        long originId = mAdapter.getItemId(originPosition);
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
                                    mListView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                                }
                            });
                            messageViewEnd = origMessageViewEnd;
                            maxMessageId = mCurrentCursor.lastId;
                        }else{
                            if(mCurrentCursor.lastId > maxMessageId) {
                                mUnread.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                    if(isShowed()) {
                        binder.setRead(mChannel);
                    }
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        if(mCurrentCursor!=null){
            mCurrentCursor.cursor.close();
        }
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
