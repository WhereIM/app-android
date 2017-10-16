package im.where.whereim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.MapzenPOI;
import im.where.whereim.models.POI;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by buganini on 19/01/17.
 */

public class ChannelMapzenSearchFragment extends ChannelSearchFragment {

    private String cachedApiKey = null;

    private void getApiKey(final CoreService.ApiKeyCallback callback){
        if (cachedApiKey!=null) {
            callback.apiKey(cachedApiKey);
        } else {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    binder.getApiKey(Key.MAPZEN, new CoreService.ApiKeyCallback() {
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
                                .host("search.mapzen.com")
                                .addPathSegment("v1")
                                .addPathSegment("search")
                                .addQueryParameter("text", keyword)
                                .addQueryParameter("size", "50")
                                .addQueryParameter("lang", getString(R.string.google_lang))
                                .addQueryParameter("api_key", api_key)
                                .addQueryParameter("focus.point.lat", String.format(Locale.ENGLISH, "%f", latlng.latitude))
                                .addQueryParameter("focus.point.lon", String.format(Locale.ENGLISH, "%f", latlng.longitude))
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            JSONArray features = ret.getJSONArray("features");
                            final ArrayList<POI> res = new ArrayList<POI>();
                            for(int i=0;i<features.length();i+=1){
                                JSONObject feature = features.getJSONObject(i);
                                JSONObject geometry = feature.getJSONObject("geometry");
                                JSONArray coordinates = geometry.getJSONArray("coordinates");
                                JSONObject properties = feature.getJSONObject("properties");
                                MapzenPOI poi = new MapzenPOI();

                                poi.label = properties.getString("label");
                                poi.name = properties.getString("name");
                                poi.longitude = coordinates.getDouble(0);
                                poi.latitude = coordinates.getDouble(1);
                                res.add(poi);
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setSearchResult(res);
                                }
                            });
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
                                .host("search.mapzen.com")
                                .addPathSegment("v1")
                                .addPathSegment("autocomplete")
                                .addQueryParameter("text", keyword)
                                .addQueryParameter("size", "50")
                                .addQueryParameter("lang", getString(R.string.google_lang))
                                .addQueryParameter("api_key", api_key)
                                .addQueryParameter("focus.point.lat", String.format(Locale.ENGLISH, "%f", latlng.latitude))
                                .addQueryParameter("focus.point.lon", String.format(Locale.ENGLISH, "%f", latlng.longitude))
                                .build();

                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            JSONArray features = ret.getJSONArray("features");
                            final ArrayList<String> res = new ArrayList<String>();
                            for(int i=0;i<features.length();i+=1){
                                JSONObject feature = features.getJSONObject(i);
                                JSONObject properties = feature.getJSONObject("properties");
                                String place_name= properties.getString("label");
                                res.add(place_name);
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setAutoComplete(res);
                                }
                            });
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

            public ViewHolder(View view){
                name = (TextView) view.findViewById(R.id.name);
            }

            public void setItem(MapzenPOI result){
                name.setText(result.label);
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
                view = LayoutInflater.from(getActivity()).inflate(R.layout.mapbox_search_result_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((MapzenPOI) getItem(position));

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
