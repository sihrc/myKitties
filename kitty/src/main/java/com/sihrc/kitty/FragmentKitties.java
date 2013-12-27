package com.sihrc.kitty;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
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
    /**
     * Handles the List of Kitties
     */
    AdapterImage kittyAdapter;
    ListView kittyList;

    /**
     * Database
     */
    HandlerDatabase db;     //Database Handler
    Integer isReady = 0;    //isReady to download new kitties


    //Public Constructor to decide the kitties
    public FragmentKitties(){}

    //When the View is first created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true); //Options Menu
        //Return the appropriate Fragment View
        return inflater.inflate(R.layout.fragment_kitty_grid, null);
    }

    //When the Activity is done being created
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //Grab the Database Handler from the Activity
        db = ((ActivityMain) getActivity()).db;

        //Setup the ListView
        kittyList = (ListView) getView().findViewById(R.id.fragment_kitty_listView);

        //ListView Adapter
        kittyAdapter = new AdapterImage(getActivity(), db.getAllKitties());
        kittyList.setAdapter(kittyAdapter);

        //Check for kitties on first run
        if (kittyAdapter.getCount() == 0){
            Log.d("DEBUGGER", "Getting Kitties");
            Toast.makeText(getActivity(), "Loading more images!", Toast.LENGTH_LONG).show();
            getKitties();
        }

        //Set OnScroll Listener
        kittyList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.d("DEBUGGER", "SCROLLING DATA " + firstVisibleItem + " " + visibleItemCount + " " + totalItemCount);
                if (firstVisibleItem > kittyAdapter.getCount() - 5 && isReady == 0) {
                    Log.d("DEBUGGER", "Getting Kitties");
                    Toast.makeText(getActivity(), "Loading more images!", Toast.LENGTH_SHORT).show();
                    getKitties();
                }
            }
        });
    }

    //When the Fragment is started
    @Override
    public void onStart() {
        super.onStart();
        updateGridView();
    }

    //When the Fragment is resumed
    @Override
    public void onResume() {
        super.onResume();
        updateGridView();
    }

    //Update the Grid with new kitties
    private void updateGridView(){
        kittyAdapter.clear();
        kittyAdapter.addAll(db.getKittiesByCategory(getSearchTerm()));
        kittyAdapter.notifyDataSetChanged();
        kittyList.invalidate();
        Log.d("DEBUGGER", kittyAdapter.toString());

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
                String url = getSearchURL();
                Log.d("DEBUGGER", "Getting Kitties URL " + url);

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
                        Log.d("DEBUGGER", "Getting Kitties GETTING IMAGE");
                    }
                } catch (JSONException e){
                    e.printStackTrace();
                    Log.d("DEBUGGER", "Getting Kitties - caught JSON Exception");
                    Log.d("DEBUGGER", "Getting Kitties - " + e.getMessage());
                    Log.d("DEBUGGER", "Getting Kitties - " + s);
                }
                isReady -= 1;
            }
        }.execute();
    }

    //Get Search URL
    public String getSearchURL(){
        String search = getActivity().getSharedPreferences("KittyApp", Context.MODE_PRIVATE).getString("search", "cute baby kitten");
        return "https://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=" + search.replace(" ", "+") + "&start=" + db.getKittiesByCategory(search).size() + "&userip=MyIP&imgsz=large";
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
                db.addKittyToDatabase(url, bytes, getSearchTerm());
                updateGridView();
                isReady -= 1;
            }
        }.execute();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        getActivity().getMenuInflater().inflate(R.menu.kitties, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_hide:
                hideAllKitties();
                break;
            case R.id.action_search:
                changeSearchTerm();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideAllKitties(){
        //TODO
        new AlertDialog.Builder(getActivity())
                .setTitle("Hide Images?")
                .setMessage("Are you sure you want to hide all the kitties? You'll have to reload them all to get to them again.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteKittiesByCategory(getSearchTerm());
                        updateGridView();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    /**
     * Handles Search Terms
     */
    private void changeSearchTerm(){
        final EditText searchInput = new EditText(getActivity());
        searchInput.setHint("Search Terms");
        searchInput.setGravity(View.TEXT_ALIGNMENT_CENTER);
        new AlertDialog.Builder(getActivity())
                .setTitle("Change Image Subject")
                .setMessage("What would you like to search for?")
                .setView(searchInput)
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String previousSearch = getSearchTerm();
/*                        while (isReady != 0) {
                            db.deleteKittiesByCategory(previousSearch);
                        }*/
                        db.deleteKittiesByCategory(previousSearch);
                        getActivity().getSharedPreferences("KittyApp", Context.MODE_PRIVATE).edit().putString("search", String.valueOf(searchInput.getText())).commit();
                        updateGridView();
                        getKitties();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }
    private String getSearchTerm(){
        return getActivity().getSharedPreferences("KittyApp", Context.MODE_PRIVATE).getString("search", "cute baby kitten");
    }
}
