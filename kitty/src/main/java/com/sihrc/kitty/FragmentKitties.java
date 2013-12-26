package com.sihrc.kitty;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by chris on 12/22/13.
 */
public class FragmentKitties extends Fragment {
    //List of kitties to show
    ArrayList<Kitty> kitties;

    //ImageAdapter
    AdapterImage kittyAdapter;

    //Database
    HandlerDatabase db;

    //Server is Ready for more Kittens
    Integer isReady = 0;

    //Public Constructor to decide the kitties
    public FragmentKitties(){}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_kitty_grid, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        db = ((ActivityMain) getActivity()).db;

        //ListView Grid
        ListView grid = (ListView) getView().findViewById(R.id.fragment_kitty_listView);

        //ListView Adapter
        kitties = db.getAllKitties();
        kittyAdapter = new AdapterImage(getActivity(), kitties);
        grid.setAdapter(kittyAdapter);

        //Check for kitties
        if (kittyAdapter.getCount() == 0){
            getKitties();
            Toast.makeText(getActivity(), "Loaded more images!", Toast.LENGTH_SHORT).show();
        }

        //Set OnScroll ListenerInteger previousSize = 0;
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.i("SCROLLINGSHIT", firstVisibleItem + " " + visibleItemCount + " " + kittyAdapter.getCount());
                if (firstVisibleItem > kittyAdapter.getCount() - 10 && isReady == 0){
                    getKitties();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        updateGridView();
        Log.d("FragmentKitties", "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateGridView();
        Log.d("FragmentKitties", "onResume");
    }

    //Update the Grid with new kitties
    private void updateGridView(){
        kitties.clear();
        kitties.addAll(db.getAllKitties());
        Log.d("ArrayAdapterSize", kitties.size() + "");
        kittyAdapter.notifyDataSetChanged();
        Log.d("ArrayAdapterSize", kittyAdapter.getCount() + "");
    }

    //Get New Kitty Urls - Async Task
    private void getKitties(){
        new AsyncTask<Void, Void, String>() {
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                isReady += 1;
                //Request TimeOut
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
            }

            @Override
            protected String doInBackground(Void... params) {
                //Get next url
                String url = getSearchURL("cute baby kitten");

                //HTTP GET Request
                HttpGet getImages = new HttpGet(url);
                try {
                    //Parsing HTTP Response
                    response = client.execute(getImages);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"), 8);
                    StringBuilder sb = new StringBuilder();

                    String line;
                    while ((line = reader.readLine())!=null){
                        sb.append(line);
                        sb.append(System.getProperty("line.separator"));
                    }
                    return sb.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                    return "failed"; //Default URL for failed Requests
                }
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                try {
                    JSONArray results = new JSONObject(s).getJSONObject("responseData").getJSONArray("results");
                    for (int i = 0; i < results.length(); i++){
                        getImageAndPush(results.getJSONObject(i).getString("unescapedUrl"));
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    Log.d("ReturnedResponse", s);
                }
                isReady -= 1;

            }
        }.execute();
    }

    public String getSearchURL(String search){
        Log.d("URLSIZE", db.getAllKitties().size() + "");
        return "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=" + search.replace(" ", "+") + "&start=" + db.getAllKitties().size() + "&userip=MyIP&imgsz=large";
    }
    //Get Image and Push
    private void getImageAndPush(final String url){
        new AsyncTask<Void, Void, byte[]>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                isReady += 1;
            }

            @Override
            protected byte[] doInBackground(Void... params) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(url);
                try{
                    //Request google image search results
                    HttpResponse response = client.execute(request);
                    HttpEntity entity = response.getEntity();
                    int imageLength = (int)(entity.getContentLength());
                    InputStream is = entity.getContent();
                    byte[] imageBlob = new byte[imageLength];
                    int bytesRead = 0;
                    //Pull the image's byte array
                    while (bytesRead < imageLength) {
                        int n = is.read(imageBlob, bytesRead, imageLength - bytesRead);
                        bytesRead += n;
                    }
                    return imageBlob;
                } catch (Exception e) {
                    e.printStackTrace();
                    return new byte[0];
                }
            }

            @Override
            protected void onPostExecute(byte[] bytes) {
                super.onPostExecute(bytes);
                db.addKittyToDatabase(bytes);
                updateGridView();
                isReady -= 1;
            }
        }.execute();
    }
}
