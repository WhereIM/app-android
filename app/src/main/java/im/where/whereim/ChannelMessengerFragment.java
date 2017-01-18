package im.where.whereim;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import im.where.whereim.database.Message;

public class ChannelMessengerFragment extends BaseFragment {
    public ChannelMessengerFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Models.Channel mChannel;
    private MessageCursorAdapter mAdapter;
    private class MessageCursorAdapter extends CursorAdapter {

        public MessageCursorAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        class ViewHolder {
            TextView sender;
            TextView time;
            TextView message;
        }
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.sender = (TextView) view.findViewById(R.id.sender);
            vh.time = (TextView) view.findViewById(R.id.time);
            vh.message = (TextView) view.findViewById(R.id.message);
            view.setTag(vh);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CoreService.CoreBinder binder = getBinder();
            Message m = Message.parse(cursor);
            ViewHolder vh = (ViewHolder) view.getTag();
            if(binder==null){
                vh.sender.setText(null);
                vh.message.setText(null);
                vh.time.setText(null);
                return;
            }
            vh.sender.setText(binder.getChannelMate(mChannel.id, m.mate_id).getDisplayName());
            Models.Marker marker;
            switch(m.type){
                case "text":
                    vh.message.setText(m.message);
                    break;
                case "marker_create":
                    marker = binder.getChannelMarker(mChannel.id, m.message);
                    vh.message.setText(getResources().getString(R.string.message_marker_create, marker==null?"":marker.name));
                    break;
            }
            vh.time.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(m.time*1000)));
        }
    };

    private ListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_messenger, container, false);

        final EditText input = (EditText) view.findViewById(R.id.input);
        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = input.getEditableText().toString();
                if(message.isEmpty()){
                    return;
                }
                input.setText(null);
                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Models.Channel channel) {
                                binder.sendMessage(channel, message);
                            }
                        });
                    }
                });
            }
        });

        mListView = (ListView) view.findViewById(R.id.message);

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Models.Channel channel) {
                mChannel = channel;

                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mAdapter = new MessageCursorAdapter(getActivity(), binder.getMessageCursor(mChannel));
                        mListView.setAdapter(mAdapter);

                        binder.addMessageListener(channel, mMessageListener);
                    }
                });
            }
        });
        return view;
    }

    private Runnable mMessageListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new Models.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    mAdapter.changeCursor(binder.getMessageCursor(mChannel));
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        postBinderTask(new Models.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.removeMessageListener(mChannel, mMessageListener);
            }
        });
        super.onDestroyView();
    }
}
