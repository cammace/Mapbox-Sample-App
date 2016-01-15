package com.mycompany.myfirstglapp;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.Toast;

import com.mapbox.geocoder.MapboxGeocoder;
import com.mapbox.geocoder.service.models.GeocoderFeature;
import com.mapbox.geocoder.service.models.GeocoderResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class SearchActivity extends AppCompatActivity {

    private static final String LOG_TAG = "SearchActivity";

    private ArrayList<String> autoCompleteList = new ArrayList<>();
    private RecyclerView.Adapter adapter;
    private View coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        coordinatorLayout = findViewById(R.id.coordinator_layout);

        // Display the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Adds back button to toolbar and handle its onClick
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        });

        autoCompleteRecycler();

    }// End onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Setup the searchView in toolbar
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) findViewById(R.id.search_view);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.onActionViewExpanded();
        searchView.setQueryRefinementEnabled(true);
        searchView.setMaxWidth(2000);
        searchView.setQueryHint(getString(R.string.search_hint));

        // Handle the text submit and change here
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                executeSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                // Clear the recyclerView list
                autoCompleteList.clear();

                if (newText.isEmpty()) adapter.notifyDataSetChanged();
                else {

                    MapboxGeocoder client = new MapboxGeocoder.Builder()
                            .setAccessToken(getString(R.string.accessToken))
                            .setLocation(newText)
                            .build();

                    client.enqueue(new Callback<GeocoderResponse>() {
                                       @Override
                                       public void onResponse(Response<GeocoderResponse> response, Retrofit retrofit) {
                                           List<GeocoderFeature> results = response.body().getFeatures();
                                           for (int i = 0; i < results.size(); i++) {
                                               autoCompleteList.add(results.get(0).getPlaceName());
                                           }
                                           adapter.notifyDataSetChanged();
                                       }

                                       @Override
                                       public void onFailure(Throwable t) {
                                           Log.e(LOG_TAG, "Unable to fill auto complete list", t);
                                       }
                                   }
                    );
                }
                return false;
            }// End onQueryTextChange
        });
        return super.onCreateOptionsMenu(menu);
    }// End onCreateOptionsMenu

    private void autoCompleteRecycler(){
        // Create the recyclerView and add the list.
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.mapbox_autocomplete);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter(SearchActivity.this, autoCompleteList);
        recyclerView.setAdapter(adapter);

        // Create a gesture detector inorder to understand what the users action is on the recyclerView
        final GestureDetector mGestureDetector = new GestureDetector(SearchActivity.this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        // User click on recyclerView item is handled here
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null && mGestureDetector.onTouchEvent(e)) {

                    // Play the default item click sound.
                    coordinatorLayout.playSoundEffect(SoundEffectConstants.CLICK);

                    executeSearch(autoCompleteList.get(rv.getChildAdapterPosition(child)));

                    return true;
                }
                return false;
            }// End onInterceptTouchEvent

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {/* We don't use this*/}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {/* We don't use this*/}
        });
    }// End autoCompleteRecycler

    private void executeSearch(String query){

        MapboxGeocoder client = new MapboxGeocoder.Builder()
                .setAccessToken(getString(R.string.accessToken))
                .setLocation(query)
                .build();

        client.enqueue(new Callback<GeocoderResponse>() {
            @Override
            public void onResponse(Response<GeocoderResponse> response, Retrofit retrofit) {
                List<GeocoderFeature> results = response.body().getFeatures();

                if (results.size() > 0) {
                    String placeName = results.get(0).getPlaceName();
                    Double placeLat = results.get(0).getLatitude();
                    Double placeLon = results.get(0).getLongitude();

                    Intent returnIntent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putDouble("placeLat", placeLat);
                    bundle.putDouble("placeLon", placeLon);
                    bundle.putString("placeName", placeName);
                    returnIntent.putExtra("result", bundle);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();

                } else {
                    Toast.makeText(SearchActivity.this, "No Results", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(LOG_TAG, "Error: ", t);
            }
        });

    }// End executeSearch
}// End SearchActivity
