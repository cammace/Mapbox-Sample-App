package com.mycompany.myfirstglapp;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

public class MainActivity extends AppCompatActivity {

    private final static String LOG_TAG = "MainActivity";

    private MapView mapView = null;
    public static View mapContainer;
    Marker resultMarker;
    boolean firstTimeAppRun;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapContainer = findViewById(R.id.map_container);

        // Display the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        firstTimeAppRun = false;

        // Create the map and set its center location wherever the users located at
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapView.onCreate(savedInstanceState);
        mapView.setMyLocationEnabled(true);
        mapView.setZoomLevel(17);
        mapView.setCenterCoordinate(new LatLng(41.885, -87.679));

        // Change the center of the map to user location once acquired (not null)
        mapView.setOnMyLocationChangeListener(new MapView.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if(!firstTimeAppRun)
                mapView.setCenterCoordinate(
                        new LatLng(location.getLatitude(), location.getLongitude()));
                firstTimeAppRun = true;
            }
        });

        infoWindowAdapter();
        userLocationFAB();

    }// End onCreate

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause()  {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }// End onCreateOptionsMenu

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.search_icon) {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivityForResult(intent, 1);
        }
        return super.onOptionsItemSelected(item);
    }// End onOptionsItemSelected

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Once user returns from SearchActivity we need to place the location on mapView
        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                Bundle bundle = data.getBundleExtra("result");
                Double placeLat = bundle.getDouble("placeLat");
                Double placeLon = bundle.getDouble("placeLon");
                String placeName = bundle.getString("placeName");

                // Remove previous resultMarker if it exist on mapView
                if(resultMarker != null) mapView.removeMarker(resultMarker);

                // Move the camera to result location
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(placeLat, placeLon))
                        .build();

                mapView.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 15000, null);

                // Place resultMarker on mapView
                resultMarker = mapView.addMarker(new MarkerOptions()
                        .position(new LatLng(placeLat, placeLon))
                        .title(placeName)
                    .snippet(""));
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.v(LOG_TAG, "User canceled search");
            }
        }
    }//onActivityResult

    private void userLocationFAB(){
        FloatingActionButton FAB = (FloatingActionButton) findViewById(R.id.myLocationButton);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mapView.getMyLocation() != null) { // Check to ensure coordinates aren't null, probably a better way of doing this...

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(mapView.getMyLocation().getLatitude(), mapView.getMyLocation().getLongitude()))
                            .zoom(17)
                            .build();

                    mapView.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 15000, null);
                } else {
                    Snackbar.make(mapContainer, "Your current location cannot be found", Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }// End userLocationFAB

    private void infoWindowAdapter(){
        // Creation of the custom infoWindow is taken place here
        mapView.setInfoWindowAdapter(new MapView.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(@NonNull final Marker marker) {

                // inflate the infoWindow XML layout and set the text
                View view = View.inflate(MainActivity.this, R.layout.info_window, null);
                TextView name = (TextView) view.findViewById(R.id.name_of_place);
                TextView address = (TextView) view.findViewById(R.id.address_of_place);
                name.setText(marker.getTitle());
                address.setText(marker.getSnippet());

                // Clip the infoWindow to create round corners.
                if (Build.VERSION.SDK_INT >= 21) {
                    view.findViewById(R.id.mainpanal).setClipToOutline(true);
                    view.setElevation(24);
                }

                // The direction button click event's handled here.
                ImageButton directionButton = (ImageButton) view.findViewById(R.id.direction_button);
                directionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, DirectionsActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putDouble("destinationLat", marker.getPosition().getLatitude());
                        bundle.putDouble("destinationLon", marker.getPosition().getLongitude());
                        bundle.putDouble("centerLat", mapView.getCenterCoordinate().getLatitude());
                        bundle.putDouble("centerLon", mapView.getCenterCoordinate().getLongitude());
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                });
                return view;
            }
        });// End setInfoWindowAdapter
    }// End infoWindowAdapter

}// End MainActivity
