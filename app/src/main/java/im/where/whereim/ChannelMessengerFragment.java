package im.where.whereim;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.ArrayList;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.Message;
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
    private class MessageCursorAdapter extends CursorAdapter {
        private Message.BundledCursor mBundledCursor;
        public MessageCursorAdapter(Context context, Message.BundledCursor bc) {
            super(context, bc.cursor, true);
            mBundledCursor = bc;
        }

        public void changeCursor(Message.BundledCursor bc) {
            mBundledCursor = bc;
            super.changeCursor(bc.cursor);
        }

        private int getItemViewType(Cursor cursor) {
            Message m = Message.parse(cursor);
            if("image".equals(m.type)){
                if(mChannel.mate_id.equals(m.mate_id)){
                    return 3; // out
                }else{
                    return 2; // in
                }
            }else{
                if(mChannel.mate_id.equals(m.mate_id)){
                    return 1; // out
                }else{
                    return 0; // in
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            Cursor cursor = (Cursor) getItem(position);
            return getItemViewType(cursor);
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        class InMessageViewHolder {
            TextView date;
            TextView sender;
            TextView time;
            TextView message;
        }

        class OutMesageViewHolder {
            TextView date;
            TextView time;
            TextView message;
        }

        class InImageViewHolder {
            TextView date;
            TextView sender;
            TextView time;
            ImageView image;
        }

        class OutImageViewHolder {
            TextView date;
            TextView time;
            ImageView image;
        }

        @Override
        public int getCount() {
            return mBundledCursor.count;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = null;
            switch(getItemViewType(cursor)){
                case 0: {
                    view = LayoutInflater.from(context).inflate(R.layout.in_message_item, parent, false);
                    InMessageViewHolder ivh = new InMessageViewHolder();
                    ivh.date = (TextView) view.findViewById(R.id.date);
                    ivh.sender = (TextView) view.findViewById(R.id.sender);
                    ivh.time = (TextView) view.findViewById(R.id.time);
                    ivh.message = (TextView) view.findViewById(R.id.message);
                    view.setTag(ivh);
                    return view;
                }
                case 1: {
                    view = LayoutInflater.from(context).inflate(R.layout.out_message_item, parent, false);
                    OutMesageViewHolder ovh = new OutMesageViewHolder();
                    ovh.date = (TextView) view.findViewById(R.id.date);
                    ovh.time = (TextView) view.findViewById(R.id.time);
                    ovh.message = (TextView) view.findViewById(R.id.message);
                    view.setTag(ovh);

                    return view;
                }
                case 2: {
                    view = LayoutInflater.from(context).inflate(R.layout.in_image_item, parent, false);
                    InImageViewHolder ivh = new InImageViewHolder();
                    ivh.date = (TextView) view.findViewById(R.id.date);
                    ivh.sender = (TextView) view.findViewById(R.id.sender);
                    ivh.time = (TextView) view.findViewById(R.id.time);
                    ivh.image = (ImageView) view.findViewById(R.id.image);
                    view.setTag(ivh);
                    return view;
                }
                case 3: {
                    view = LayoutInflater.from(context).inflate(R.layout.out_image_item, parent, false);
                    OutImageViewHolder ovh = new OutImageViewHolder();
                    ovh.date = (TextView) view.findViewById(R.id.date);
                    ovh.time = (TextView) view.findViewById(R.id.time);
                    ovh.image = (ImageView) view.findViewById(R.id.image);
                    view.setTag(ovh);

                    return view;
                }
            }
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CoreService.CoreBinder binder = getBinder();
            final int position = cursor.getPosition();
            boolean showDate = false;
            Message m = Message.parse(cursor);
            Timestamp time = new Timestamp(m.time*1000);
            String lymd = DateFormat.getDateInstance().format(time);
            String eee = new SimpleDateFormat("EEE").format(time);
            String hm = new SimpleDateFormat("HH:mm").format(time);
            if(position==0){
                showDate = true;
            }else{
                cursor.moveToPosition(position-1);
                Message prev = Message.parse(cursor);
                if(!new SimpleDateFormat("yyyy-MM-dd").format(time).equals(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(prev.time*1000)))){
                    showDate = true;
                }
                cursor.moveToPosition(position);
            }
            switch(getItemViewType(cursor)){
                case 0: {
                    InMessageViewHolder ivh = (InMessageViewHolder) view.getTag();

                    if (binder == null) {
                        ivh.date.setVisibility(View.GONE);
                        ivh.sender.setText(null);
                        ivh.message.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, m.mate_id);
                    ivh.sender.setText(mate == null ? "" : mate.getDisplayName());
                    ivh.message.setText(m.getText(getActivity(), binder, clickedListener));
                    ivh.time.setText(hm);
                    ivh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ivh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case 1: {
                    OutMesageViewHolder ovh = (OutMesageViewHolder) view.getTag();
                    if (binder == null) {
                        ovh.date.setVisibility(View.GONE);
                        ovh.message.setText(null);
                        ovh.time.setText(null);
                        return;
                    }
                    ovh.message.setText(m.getText(getActivity(), binder, clickedListener));
                    ovh.time.setText(hm);
                    ovh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ovh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case 2: {
                    InImageViewHolder ivh = (InImageViewHolder) view.getTag();
                    Glide.with(ChannelMessengerFragment.this).clear(ivh.image);
                    if (binder == null) {
                        ivh.date.setVisibility(View.GONE);
                        ivh.sender.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, m.mate_id);
                    ivh.sender.setText(mate == null ? "" : mate.getDisplayName());
                    Glide.with(ChannelMessengerFragment.this).load(Config.getThumbnail(m.getPayload())).into(ivh.image);
                    ivh.time.setText(hm);
                    ivh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if (showDate) {
                        ivh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                }
                case 3: {
                    OutImageViewHolder ovh = (OutImageViewHolder) view.getTag();
                    Glide.with(ChannelMessengerFragment.this).clear(ovh.image);
                    if (binder == null) {
                        ovh.date.setVisibility(View.GONE);
                        ovh.time.setText(null);
                        return;
                    }
                    Glide.with(ChannelMessengerFragment.this).load(Config.getThumbnail(m.getPayload())).into(ovh.image);
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
    private ListView mListView;
    private View mUnread;
    private View mPin;
    private View mPicker;
    private View mCamera;
    private EditText mInput;
    public QuadTree.LatLng pinLocation;
    private String mCurrentPhotoPath;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_messenger, container, false);

        mPin = view.findViewById(R.id.pin);

        mUnread = view.findViewById(R.id.unread);
        mUnread.setVisibility(View.GONE);
        mUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListView.setSelection(mAdapter.getCount() - 1);
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

        mListView = (ListView) view.findViewById(R.id.message);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mCurrentCursor==null){
                    return;
                }
                messageViewEnd = view.getChildCount() == 0 || view.getLastVisiblePosition() == view.getAdapter().getCount() - 1 &&
                        view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight();
                if(messageViewEnd){
                    mUnread.setVisibility(View.GONE);
                }
                if(firstVisibleItem==0){
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
                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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

    private boolean inited = false;
    private Runnable mMessageListener = new Runnable() {
        @Override
        public void run() {
            if(!inited){
                inited = true;
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
                        int originPosition = mListView.getFirstVisiblePosition();
                        long originId = mAdapter.getItemId(originPosition);
                        Integer newPosition = mCurrentCursor.positionMap.get(originId);
                        if(newPosition == null){
                            newPosition = originPosition;
                        }
                        View v = mListView.getChildAt(0);
                        int originTop = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
                        mAdapter.changeCursor(mCurrentCursor);
                        mListView.setSelectionFromTop(newPosition, originTop);
                        if(origMessageViewEnd){
                            mListView.smoothScrollToPosition(mAdapter.getCount() - 1);
                            messageViewEnd = origMessageViewEnd;
                        }else{
                            if(mCurrentCursor.lastId > maxMessageId) {
                                mUnread.setVisibility(View.VISIBLE);
                            }
                            maxMessageId = mCurrentCursor.lastId;
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
                                    String filename = Util.getFileName(getContext(), uri);

                                    try {
                                        File temp = new File(getContext().getCacheDir(), filename);
                                        InputStream is = getContext().getContentResolver().openInputStream(uri);
                                        OutputStream os = new FileOutputStream(temp);
                                        IOUtils.copy(is, os);
                                        is.close();
                                        os.close();
                                        temp.deleteOnExit();
                                        binder.sendImage(getContext(), channel, filename, temp, true);
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
                                binder.sendImage(getContext(), channel, file.getName(), file, false);
                            }
                        });
                    }
                });
                break;
            }
        }
    }
}
