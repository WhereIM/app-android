package im.where.whereim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelSearchFragment extends BaseFragment {
    private class SearchHistoryAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView text;

            public ViewHolder(View view){
                text = (TextView) view.findViewById(R.id.text);
            }

            public void setItem(String prediction){
                text.setText(prediction);
            }
        }

        @Override
        public int getCount() {
            return mSearchHistory.size();
        }

        @Override
        public Object getItem(int i) {
            return mSearchHistory.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.search_history_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((String) getItem(position));

            return view;
        }
    }

    protected Handler mHandler = new Handler();

    abstract protected void search(String keyword);
    abstract protected void autoComplete(String keyword);
    abstract protected BaseAdapter getSearchResultAdapter();
    abstract protected BaseAdapter getAutoCompleteAdapter();

    private void do_search(final String keyword){
        final ChannelActivity activity = (ChannelActivity) getActivity();
        if(activity!=null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        if(keyword.isEmpty()){
            mAdView.setVisibility(View.VISIBLE);
            setSearchResult(new ArrayList<POI>());
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mSearchHistory) {
                    mSearchHistory.remove(keyword);
                    mSearchHistory.add(0, keyword);
                    while(mSearchHistory.size() > 15){
                        mSearchHistory.remove(mSearchHistory.size()-1);
                    }
                    mSearchHistoryAdapter.notifyDataSetChanged();
                }
            }
        });

        mAdView.setVisibility(View.GONE);

        mLoading.setVisibility(View.VISIBLE);
        mListView.setAdapter(null);

        search(keyword);
    }

    protected ArrayList<POI> mSearchResult = new ArrayList<>();
    protected void setSearchResult(final ArrayList<POI> result){
        if (result.size()>0) {
            mSearch.setVisibility(View.GONE);
            mClear.setVisibility(View.VISIBLE);
        } else {
            mSearch.setVisibility(View.VISIBLE);
            mClear.setVisibility(View.GONE);

        }

        final ChannelActivity activity = (ChannelActivity) getActivity();
        activity.setSearchResult(result);
        mLoading.setVisibility(View.GONE);
        mSearchResult = result;
        mSearchResultAdapter.notifyDataSetChanged();
        mListView.setAdapter(mSearchResultAdapter);
    }

    protected ArrayList<String> mPredictions = new ArrayList<>();
    protected void setAutoComplete(final ArrayList<String> predictions){
        mAdView.setVisibility(View.GONE);
        mLoading.setVisibility(View.GONE);
        mPredictions = predictions;
        mAutoCompleteAdapter.notifyDataSetChanged();
        mListView.setAdapter(mAutoCompleteAdapter);
    }

    protected void showHistory(){
        mAdView.setVisibility(View.GONE);
        mLoading.setVisibility(View.GONE);
        mListView.setAdapter(mSearchHistoryAdapter);
    }

    private EditText mKeyword;
    private Button mSearch;
    private Button mClear;
    private View mLoading;
    private ListView mListView;
    private View mAdView;
    private BaseAdapter mSearchResultAdapter = getSearchResultAdapter();
    private BaseAdapter mAutoCompleteAdapter = getAutoCompleteAdapter();
    private BaseAdapter mSearchHistoryAdapter = new SearchHistoryAdapter();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_search, container, false);

        mAdView = view.findViewById(R.id.adView);

        mClear = (Button) view.findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mKeyword.setText("");
                do_search("");
                mSearch.setVisibility(View.VISIBLE);
                mClear.setVisibility(View.GONE);
            }
        });

        mSearch = (Button) view.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String q = mKeyword.getText().toString();
                do_search(q);
            }
        });

        mListView = (ListView) view.findViewById(R.id.results);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mListView.getAdapter()==mSearchResultAdapter){
                    final ChannelActivity activity = (ChannelActivity) getActivity();
                    if(activity!=null){
                        activity.moveToSearchResult(position, true);
                    }
                    return;
                }
                if(mListView.getAdapter()==mAutoCompleteAdapter){
                    String k = (String)mAutoCompleteAdapter.getItem(position);
                    do_search(k);
                    mKeyword.setText(k);
                    return;
                }
                if(mListView.getAdapter()==mSearchHistoryAdapter){
                    String k = (String)mSearchHistoryAdapter.getItem(position);
                    do_search(k);
                    mKeyword.setText(k);
                    return;
                }
            }
        });
        mListView.setAdapter(mSearchResultAdapter);

        mLoading = view.findViewById(R.id.loading);

        mKeyword = (EditText) view.findViewById(R.id.keyword);
        mKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String q = mKeyword.getText().toString();
                    do_search(q);
                    handled = true;
                }
                return handled;
            }
        });
        mKeyword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mKeyword.isFocused() && mKeyword.getText().toString().isEmpty()) {
                    showHistory();
                }
            }
        });
        mKeyword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    showHistory();
                } else {
                    if(mKeyword.getText().toString().isEmpty()){
                        do_search("");
                    }
                }
            }
        });
        mKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mClear.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                if (s.length()==0) {
                    showHistory();
                } else {
                    autoComplete(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        checkout();
    }

    @Override
    public void onPause() {
        commit();

        super.onPause();
    }

    private ArrayList<String> mSearchHistory = new ArrayList<>();
    private void checkout(){
        SharedPreferences sp = getActivity().getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String history = sp.getString(Key.SEARCH_HISTORY, "[]");
        try {
            JSONArray a = new JSONArray(history);
            synchronized (mSearchHistory) {
                mSearchHistory.clear();
                for(int i=0;i<a.length();i+=1){
                    String k = a.getString(i);
                    mSearchHistory.add(k);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void commit(){
        JSONArray a = new JSONArray();
        synchronized (mSearchHistory) {
            for (String k : mSearchHistory) {
                a.put(k);
            }
        }
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(Key.SEARCH_HISTORY, a.toString());
        editor.apply();
    }
}
