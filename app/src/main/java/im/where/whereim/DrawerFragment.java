package im.where.whereim;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import im.where.whereim.models.Channel;


public class DrawerFragment extends BaseFragment {
    protected Handler mHandler = new Handler();
    private List<Channel> mChannelList;
    private ListView mListView;
    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            refresh();
        }
    };

    public void refresh(){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                mHandler.post(new Runnable(){

                    @Override
                    public void run() {
                        mChannelList = binder.getChannelList();
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public DrawerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChannelActivity activity = (ChannelActivity) getActivity();
                activity.closeDrawer();
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
            }
        });
        view.findViewById(R.id.new_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), NewChannelActivity.class);
                startActivity(intent);
            }
        });

        mListView = (ListView) view.findViewById(R.id.channel_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel channel = (Channel) mAdapter.getItem(position);
                ChannelActivity activity = (ChannelActivity) getActivity();
                activity.setChannel(channel.id);
                activity.closeDrawer();
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                getActivity().startActionMode(new ActionMode.Callback() {
                    private final static int ACTION_EDIT = 0;
                    private final static int ACTION_TOGGLE_ENABLED = 1;
                    private final static int ACTION_DELETE = 2;
                    private Channel channel;

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        channel = (Channel) mAdapter.getItem(position);
                        menu.add(0, ACTION_EDIT, 0, "✏️");
//                        if(channel.enabled !=null && !channel.enabled)
//                            menu.add(0, ACTION_TOGGLE_ENABLED, 0, "\uD83D\uDD13");
//                        if(channel.enabled !=null && channel.enabled)
//                            menu.add(0, ACTION_TOGGLE_ENABLED, 0, "\uD83D\uDD12");
                        menu.add(0, ACTION_DELETE, 0, "❌️");
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        mode.finish();
                        switch(item.getItemId()){
                            case ACTION_EDIT:
                                final View dialog_view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_channel_edit,  null);
                                final EditText et_channel_name = (EditText) dialog_view.findViewById(R.id.channel_name);
                                final EditText et_user_channel_name = (EditText) dialog_view.findViewById(R.id.user_channel_name);
                                et_channel_name.setText(channel.channel_name);
                                et_user_channel_name.setText(channel.user_channel_name);
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.edit_channel)
                                        .setView(dialog_view)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final String channel_name = et_channel_name.getText().toString();
                                                final String user_channel_name = et_user_channel_name.getText().toString();
                                                postBinderTask(new CoreService.BinderTask() {
                                                    @Override
                                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                                        binder.editChannel(channel, channel_name, user_channel_name);
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                                return true;
                            case ACTION_TOGGLE_ENABLED:
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                        binder.toggleChannelEnabled(channel);
                                    }
                                });
                                return true;
                            case ACTION_DELETE:
                                new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.quit_channel)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                postBinderTask(new CoreService.BinderTask() {
                                                    @Override
                                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                                        binder.deleteChannel(channel);
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                    }
                });
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.addChannelListChangedListener(mChannelListChangedListener);
            }
        });
    }

    @Override
    public void onPause() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.removeChannelListChangedListener(mChannelListChangedListener);
            }
        });
        super.onPause();
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            if(mChannelList == null)
                return 0;
            return mChannelList.size();
        }

        @Override
        public Object getItem(int position) {
            return mChannelList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        class ViewHolder{
            Channel mChannel;
            View mUnread;
            TextView mTitle;
            TextView mSubtitle;
            Switch mEnable;
            View mLoading;

            public ViewHolder(View view) {
                mUnread = view.findViewById(R.id.unread);
                mTitle = (TextView) view.findViewById(R.id.title);
                mSubtitle = (TextView) view.findViewById(R.id.subtitle);
                mEnable = (Switch) view.findViewById(R.id.enable);
                mLoading = view.findViewById(R.id.loading);
                mEnable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                binder.toggleChannelActive(getActivity(), mChannel);
                            }
                        });
                    }
                });
                View.OnLongClickListener deactivate = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                binder.deactivateChannel(mChannel);
                            }
                        });
                        return true;
                    }
                };
                mLoading.setOnLongClickListener(deactivate);
                mEnable.setOnLongClickListener(deactivate);
            }

            public void setItem(Channel channel){
                mChannel = channel;
                mUnread.setVisibility(mChannel.enabled!=null && mChannel.enabled && mChannel.unread ? View.VISIBLE : View.INVISIBLE);
                if(channel.user_channel_name !=null && !channel.user_channel_name.isEmpty()){
                    mSubtitle.setVisibility(View.VISIBLE);
                    mTitle.setText(channel.user_channel_name);
                    mSubtitle.setText(channel.channel_name);
                } else {
                    mTitle.setText(channel.channel_name);
                    mSubtitle.setVisibility(View.GONE);
                }
                if(channel.enabled!=null && channel.enabled) {
                    if(channel.active == null) {
                        mLoading.setVisibility(View.VISIBLE);
                        mEnable.setVisibility(View.GONE);
                    } else {
                        mLoading.setVisibility(View.GONE);
                        mEnable.setVisibility(View.VISIBLE);
                        mEnable.setChecked(channel.active);
                    }
                } else {
                    mLoading.setVisibility(View.GONE);
                    mEnable.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.channel_item, null);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            Channel channel = (Channel) getItem(position);
            vh.setItem(channel);

            return view;
        }
    };
}
