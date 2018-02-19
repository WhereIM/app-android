package im.where.whereim;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import im.where.whereim.models.Channel;
import im.where.whereim.models.ChannelView;

public class GuestViewMgmtActivity extends BaseChannelActivity {
    private TextView mChannelTitle;
    private TextView mChannelSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest_view_mgmt);
        mChannelTitle = (TextView) findViewById(R.id.channel_title);
        mChannelSubtitle = (TextView) findViewById(R.id.channel_subtitle);

        ListView mViewsListView = (ListView) findViewById(R.id.views_list);
        mViewsListView.setAdapter(mAdapter);
        mViewsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                getChannel(new GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        ChannelView channelView = (ChannelView) mAdapter.getItem(position);
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.action_guest_view));
                        i.putExtra(Intent.EXTRA_TEXT, getString(R.string.guest_view_link_for, channel.channel_name)+"\n"+channelView.getLink());
                        startActivity(Intent.createChooser(i, getString(R.string.action_guest_view)));
                    }
                });
            }
        });

        getChannel(new GetChannelCallback() {
            @Override
            public void onGetChannel(Channel channel) {
                if(mChannel.user_channel_name==null || mChannel.user_channel_name.isEmpty()){
                    mChannelTitle.setText(mChannel.channel_name);
                    mChannelSubtitle.setVisibility(View.GONE);
                }else{
                    mChannelSubtitle.setVisibility(View.VISIBLE);
                    mChannelTitle.setText(mChannel.user_channel_name);
                    mChannelSubtitle.setText(mChannel.channel_name);
                }
            }
        });
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return viewsList.size();
        }

        @Override
        public Object getItem(int position) {
            return viewsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        class ViewHolder{
            ChannelView mView;
            TextView mName;
            Button mEdit;

            public ViewHolder(View view) {
                mName = (TextView) view.findViewById(R.id.name);
                mEdit = (Button) view.findViewById(R.id.edit);
                mEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }

            public void setItem(ChannelView view){
                mView = view;
                mName.setText(view.name);
                mEdit.setVisibility(view.admin ? View.GONE : View.VISIBLE);
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(GuestViewMgmtActivity.this).inflate(R.layout.channelview_item, null);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            ChannelView channelView = (ChannelView) getItem(position);
            vh.setItem(channelView);
            return view;
        }
    };

    final private HashMap<String, ChannelView> mViewMap = new HashMap<>();
    ArrayList<ChannelView> viewsList = new ArrayList<>();

    private CoreService.ChannelViewDelegate delegate = new CoreService.ChannelViewDelegate() {
        @Override
        public void onChannelView(ChannelView view) {
            synchronized (mViewMap) {
                mViewMap.put(view.id, view);
            }
            final ArrayList<ChannelView> list = new ArrayList<>();
            synchronized (mViewMap) {
                list.addAll(mViewMap.values());
            }
            Collections.sort(list, new Comparator<ChannelView>() {
                @Override
                public int compare(ChannelView o1, ChannelView o2) {
                    return o1.name.compareToIgnoreCase(o2.name);
                }
            });
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    viewsList = list;
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        binder.openChannelView(channel, delegate);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.closeChannelView(mChannel, delegate);
        }
        super.onPause();
    }
}
