package im.where.whereim;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.GooglePOI;
import im.where.whereim.models.POI;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by buganini on 19/01/17.
 */

public class PaneGoogleSearch extends PaneSearch {

    private String cachedApiKey = null;

    private void getApiKey(final CoreService.ApiKeyCallback callback){
        if (cachedApiKey!=null) {
            callback.apiKey(cachedApiKey);
        } else {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    binder.getApiKey(Key.GOOGLE_SEARCH, new CoreService.ApiKeyCallback() {
                        @Override
                        public void apiKey(String api_key) {
                            cachedApiKey = api_key;
                            callback.apiKey(api_key);
                        }
                    });
                }
            });
        }
    }

    @Override
    public Config.MapProvider getProvider() {
        return Config.MapProvider.GOOGLE;
    }

    @Override
    public void search(final String keyword) {
        final ChannelActivity activity = (ChannelActivity) getActivity();

        getApiKey(new CoreService.ApiKeyCallback() {
            @Override
            public void apiKey(final String api_key) {
                if(activity==null)
                    return;
                new Thread(){
                    @Override
                    public void run() {
                        QuadTree.LatLng latlng = activity.getMapCenter();
                        HttpUrl url = new HttpUrl.Builder()
                                .scheme("https")
                                .host("maps.googleapis.com")
                                .addPathSegment("maps")
                                .addPathSegment("api")
                                .addPathSegment("place")
                                .addPathSegment("textsearch")
                                .addPathSegment("json")
                                .addQueryParameter("key", api_key)
                                .addQueryParameter("query", keyword)
                                .addQueryParameter("language", getString(R.string.google_lang))
                                .addQueryParameter("location", String.format(Locale.ENGLISH, "%f,%f", latlng.latitude, latlng.longitude))
                                .addQueryParameter("rankby", "distance")
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            String status = ret.getString("status");
                            if("REQUEST_DENIED".equals(status)){
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                        cachedApiKey = null;
                                        binder.invalidateApiKey(Key.GOOGLE_SEARCH);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                search(keyword);
                                            }
                                        });
                                    }
                                });
                                return;
                            }
                            if("OVER_QUERY_LIMIT".equals(status)){
                                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if("ZERO_RESULTS".equals(status)){
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setGoogleAttribution();
                                        setSearchResult(new ArrayList<POI>());
                                    }
                                });
                                return;
                            }
                            if("OK".equals(status)){
                                JSONArray results = ret.getJSONArray("results");
                                final ArrayList<POI> res = new ArrayList<POI>();
                                for(int i=0;i<results.length();i+=1){
                                    JSONObject result = results.getJSONObject(i);
                                    Double lat = null;
                                    Double lng = null;
                                    String name = null;
                                    String address = null;
                                    Spanned attribution = null;
                                    try{
                                        JSONObject geometry = result.getJSONObject("geometry");
                                        JSONObject location = geometry.getJSONObject("location");
                                        lat = location.getDouble("lat");
                                        lng = location.getDouble("lng");

                                        name = result.getString("name");
                                        address = result.getString("formatted_address");

                                        String attr = result.optString("attribution");
                                        if(attr!=null){
                                            attribution = Html.fromHtml(attr);
                                        }
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                        continue;
                                    }
                                    GooglePOI r = new GooglePOI();
                                    r.name = name;
                                    r.address = address;
                                    r.attribution = attribution;
                                    r.latitude = lat;
                                    r.longitude = lng;
                                    res.add(r);
                                }
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setGoogleAttribution();
                                        setSearchResult(res);
                                    }
                                });
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    @Override
    protected void autoComplete(final String keyword) {
        final ChannelActivity activity = (ChannelActivity) getActivity();

        getApiKey(new CoreService.ApiKeyCallback() {
            @Override
            public void apiKey(final String api_key) {
                if(activity==null)
                    return;
                new Thread(){
                    @Override
                    public void run() {
                        QuadTree.LatLng latlng = activity.getMapCenter();
                        HttpUrl url = new HttpUrl.Builder()
                                .scheme("https")
                                .host("maps.googleapis.com")
                                .addPathSegment("maps")
                                .addPathSegment("api")
                                .addPathSegment("place")
                                .addPathSegment("queryautocomplete")
                                .addPathSegment("json")
                                .addQueryParameter("key", api_key)
                                .addQueryParameter("input", keyword)
                                .addQueryParameter("language", getString(R.string.google_lang))
                                .addQueryParameter("radius", "50000")
                                .addQueryParameter("location", String.format(Locale.ENGLISH, "%f,%f", latlng.latitude, latlng.longitude))
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            String status = ret.getString("status");
                            if("REQUEST_DENIED".equals(status)){
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                        cachedApiKey = null;
                                        binder.invalidateApiKey(Key.GOOGLE_SEARCH);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                autoComplete(keyword);
                                            }
                                        });
                                    }
                                });
                                return;
                            }
                            if("OVER_QUERY_LIMIT".equals(status)){
                                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if("ZERO_RESULTS".equals(status)){
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setGoogleAttribution();
                                        setAutoComplete(new ArrayList<String>());
                                    }
                                });
                                return;
                            }
                            if("OK".equals(status)){
                                JSONArray predictions = ret.getJSONArray("predictions");
                                final ArrayList<String> res = new ArrayList<String>();

                                for(int i=0;i<predictions.length();i+=1){
                                    JSONObject prediction = predictions.getJSONObject(i);
                                    String description = prediction.getString("description");
                                    res.add(description);
                                }
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        setGoogleAttribution();
                                        setAutoComplete(res);
                                    }
                                });
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    private class SearchResultAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView name;
            TextView address;
            TextView attribution;

            public ViewHolder(View view){
                name = (TextView) view.findViewById(R.id.name);
                address = (TextView) view.findViewById(R.id.address);
                attribution = (TextView) view.findViewById(R.id.attribution);
            }

            public void setItem(GooglePOI result){
                name.setText(result.name);
                if(result.address==null){
                    address.setVisibility(View.GONE);
                }else{
                    address.setText(result.address);
                    address.setVisibility(View.VISIBLE);
                }
                if(result.attribution==null){
                    attribution.setVisibility(View.GONE);
                }else{
                    attribution.setText(result.attribution);
                    attribution.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getCount() {
            return mSearchResult.size();
        }

        @Override
        public Object getItem(int position) {
            return mSearchResult.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.google_search_result_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((GooglePOI) getItem(position));

            return view;
        }
    }

    @Override
    protected BaseAdapter getSearchResultAdapter() {
        return new SearchResultAdapter();
    }

    class AutoCompleteAdapter extends BaseAdapter {
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
            return mPredictions.size();
        }

        @Override
        public Object getItem(int i) {
            return mPredictions.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            AutoCompleteAdapter.ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.autocomplete_item, parent, false);
                vh = new AutoCompleteAdapter.ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (AutoCompleteAdapter.ViewHolder) view.getTag();
            }

            vh.setItem((String) getItem(position));

            return view;
        }
    }

    @Override
    protected BaseAdapter getAutoCompleteAdapter() {
        return new AutoCompleteAdapter();
    }
}
