package com.mycompany.myfirstglapp;

import android.animation.Animator;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.directions.DirectionsCriteria;
import com.mapbox.directions.MapboxDirections;
import com.mapbox.directions.service.models.DirectionsResponse;
import com.mapbox.directions.service.models.DirectionsRoute;
import com.mapbox.directions.service.models.Waypoint;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.MyBearingTracking;
import com.mapbox.mapboxsdk.constants.MyLocationTracking;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.BoundingBox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.views.MapView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public class DirectionsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "DirectionsActivity";

    private MapView mapView = null;
    int duration;
    private Double desLon;
    private Double desLat;
    Polyline routeLine;
    private List<DirectionsRoute> routes;
    private ProgressDialog dialog;
    private FloatingActionButton FAB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directions);

        // Display the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.directions_toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Adds back button to toolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavUtils.navigateUpFromSameTask(DirectionsActivity.this);
            }
        });

        // "Unpack" the bundle and assign all the information to their corresponding variables.
        Bundle bundle = getIntent().getExtras();
        desLat = bundle.getDouble("destinationLat");
        desLon = bundle.getDouble("destinationLon");
        Double centerlat = bundle.getDouble("centerLat");
        Double centerlon = bundle.getDouble("centerLon");

        // Setup mapView
        mapView = (MapView) findViewById(R.id.mapview);
        mapView.setStyleUrl(Style.MAPBOX_STREETS);
        mapView.setCenterCoordinate(new LatLng(centerlat, centerlon));
        mapView.setZoomLevel(17);
        mapView.onCreate(savedInstanceState);
        mapView.setRotateEnabled(false);
        mapView.setMyLocationEnabled(true);

        // Show a loading dialog so user can't click on null objects. This is a terrible way of
        // doing this as it doesn't abide by Material Design.
        dialog = ProgressDialog.show(DirectionsActivity.this, "", getString(R.string.loading_route), true);
        dialog.show();

        // Assign the FAB and handle when clicked, enter into navigation mode
        FAB = (FloatingActionButton) findViewById(R.id.begin_direction_fab);
        FAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterNavigationMode();
            }
        });

        executeRouting();

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

    private void executeRouting(){
        // This method's used to actually contact the Mapbox directions API and get the routes.
        if(mapView.getMyLocation() != null) {
            Waypoint origin = new Waypoint(mapView.getMyLocation().getLongitude(), mapView.getMyLocation().getLatitude());
            Waypoint destination = new Waypoint(desLon, desLat);

            MapboxDirections client = new MapboxDirections.Builder()
                    .setAccessToken(getString(R.string.accessToken))
                    .setOrigin(origin)
                    .setDestination(destination)
                    .setProfile(DirectionsCriteria.PROFILE_DRIVING)
                    .setAlternatives(true)
                    .setSteps(true)
                    .build();

            client.enqueue(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Response<DirectionsResponse> response, Retrofit retrofit) {
                    // Hide the loading dialog
                    dialog.hide();

                    // Check to make sure we received at least 1 route
                    if (response.body().getRoutes().size() > 0) {

                        routes = response.body().getRoutes();

                        // Drawing the routes occurs here. The first route will always be default
                        for (int i = 0; i < response.body().getRoutes().size(); i++) {
                            boolean defaultRoute;
                            defaultRoute = i == 0;
                            drawRoutes(response.body().getRoutes().get(i), defaultRoute);
                        }

                        // This is for calculation of the Bounding box. Every point within route is
                        // added to list and then sent to findBoundingBoxForGivenLocation method.
                        ArrayList<LatLng> allPoints = new ArrayList<>();
                        for (int j = 0; j < routes.get(0).getGeometry().getCoordinates().size(); j++) {
                            List<Double> routePoint = routes.get(0).getGeometry().getCoordinates().get(j);
                            allPoints.add(new LatLng(routePoint.get(1), routePoint.get(0)));
                        }
                        BoundingBox routeBB = findBoundingBoxForGivenLocations(allPoints);

                        // Pass the boundingBox to zoomFromBoundingBox method to calculate correct
                        // zoom level that will fit all route in mapView
                        int zoomLevel = zoomFromBoundingBox(routeBB);

                        // Move map to correct position now that everything is calculated.
                        mapView.setCenterCoordinate(routeBB.getCenter());
                        mapView.setZoomLevel(zoomLevel);

                        // Lastly, add the markers for origin and destination
                        List<Double> origin = response.body().getOrigin().getGeometry().getCoordinates();
                        mapView.addMarker(new MarkerOptions()
                                .position(new LatLng(origin.get(1), origin.get(0)))
                                .title(getString(R.string.origin))
                                .snippet(response.body().getOrigin().getProperties().getName()));

                        List<Double> destination = response.body().getDestination().getGeometry().getCoordinates();
                        mapView.addMarker(new MarkerOptions()
                                .position(new LatLng(destination.get(1), destination.get(0)))
                                .title(getString(R.string.destination))
                                .snippet(response.body().getDestination().getProperties().getName()));

                    }// End if statement
                }// End onResponse
                @Override
                public void onFailure(Throwable t) {
                    dialog.hide();
                    Log.e(LOG_TAG, "Throwable: ",t);
                    Toast.makeText(DirectionsActivity.this, getString(R.string.error_getting_route), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        }// End if statement
        else{
            dialog.hide();
            Toast.makeText(DirectionsActivity.this, getString(R.string.error_finding_location), Toast.LENGTH_LONG).show();
            finish();
        }
    }// End executeRoutes

    private void drawRoutes(DirectionsRoute route, Boolean defaultRoute){
        // Besides drawing the routes, duration and distance are also set in textViews here
        duration = route.getDuration();
        String durationString = formatRouteTime(duration * 1000);
        int distance = route.getDistance();
        String distanceString = formatRouteDistance(distance);

        TextView durationText = (TextView) findViewById(R.id.route_time);
        TextView distanceText = (TextView) findViewById(R.id.route_distance);
        durationText.setText(durationString);
        distanceText.setText(distanceString);

        // Convert List<Waypoint> into LatLng[]
        List<Waypoint> waypoints = route.getGeometry().getWaypoints();
        LatLng[] point = new LatLng[waypoints.size()];
        for (int i = 0; i < waypoints.size(); i++) {
            point[i] = new LatLng(
                    waypoints.get(i).getLatitude(),
                    waypoints.get(i).getLongitude());
        }

        // The default routes the only one to be 100% opacity
        float alpha;
        if(defaultRoute) alpha = 1f;
        else alpha = 0.4f;

        // Draw Points on MapView
        routeLine = mapView.addPolyline(new PolylineOptions()
                .add(point)
                .color(ContextCompat.getColor(this, R.color.colorRoute))
                .alpha(alpha)
                .width(5));
    }// End drawRoutes

    private void enterNavigationMode(){

        // Previously invisible view
        View turnByTurnTB = findViewById(R.id.turn_by_turn_toolbar);

        // Calculate the starting point of the circle reveal
        int cx = Math.round(FAB.getX()) + 56;
        int cy = Math.round(FAB.getY()) + 56;

        // Get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        // Create the animator for this view (the start radius is zero)
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            Animator anim = ViewAnimationUtils.createCircularReveal(turnByTurnTB, cx, cy, 0, finalRadius);
            // Start Reveal animation
            anim.start();

            // Change status bar color. Definitely a better way of accomplishing this but don't have time
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(DirectionsActivity.this, R.color.colorAccentDark));
        }// End if statement

            // Make the view visible and start the animation
            turnByTurnTB.setVisibility(View.VISIBLE);

            // Move FAB up and make it disappear
            FAB.setTranslationY(-80);
            FAB.setVisibility(View.INVISIBLE);

            // Don't allow user to control map in navigation mode
            mapView.setCompassEnabled(false);
            mapView.setZoomEnabled(false);
            mapView.setScrollEnabled(false);
            mapView.setTiltEnabled(false);

            // Move camera to starting point
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(routes.get(0).getGeometry().getCoordinates().get(0).get(1), routes.get(0).getGeometry().getCoordinates().get(0).get(0)))
                    .zoom(18)
                    .bearing(mapView.getMyBearingTrackingMode())
                    .tilt(55)
                    .build();

            mapView.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 15000, null);

            // Wait till camera's in position before actually tracking user movement. If handler
            // isn't created, it will override the move camera to starting point code just above.
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mapView.setMyBearingTrackingMode(MyBearingTracking.COMPASS);
                    mapView.setMyLocationTrackingMode(MyLocationTracking.TRACKING_FOLLOW);
                }
            }, 15000);
    }// End enterNavigationMode

    /**
     * Calculations and Conversions
     */

    private int zoomFromBoundingBox(BoundingBox boundingBox) {
        int zoomLevel;
        double latDiff = boundingBox.getLatNorth() - boundingBox.getLatSouth();
        double lngDiff = boundingBox.getLonEast() - boundingBox.getLonWest();

        double maxDiff = (lngDiff > latDiff) ? lngDiff : latDiff;
        if (maxDiff < 360 / Math.pow(2, 20)) {
            zoomLevel = 21;
        } else {
            zoomLevel = (int) (-1 * ((Math.log(maxDiff) / Math.log(2)) - (Math.log(360) / Math.log(2))));
            if (zoomLevel < 1)
                zoomLevel = 1;
        }
        return zoomLevel;
    }// End zoomFromBoundingBox

    private BoundingBox findBoundingBoxForGivenLocations(ArrayList<LatLng> coordinates) {
        double west = 0.0;
        double east = 0.0;
        double north = 0.0;
        double south = 0.0;

        for (int lc = 0; lc < coordinates.size(); lc++) {
            LatLng loc = coordinates.get(lc);

            if (lc == 0) {
                north = loc.getLatitude();
                south = loc.getLatitude();
                west = loc.getLongitude();
                east = loc.getLongitude();
            } else {
                if (loc.getLatitude() > north)
                {
                    north = loc.getLatitude();
                }
                else if (loc.getLatitude() < south)
                {
                    south = loc.getLatitude();
                }
                if (loc.getLongitude() < west)
                {
                    west = loc.getLongitude();
                }
                else if (loc.getLongitude() > east)
                {
                    east = loc.getLongitude();
                }
            }
        }

        // OPTIONAL - Add some extra "padding" for better map display
        double padding = 0.005;
        north = north + padding;
        south = south - padding;
        west = west - padding;
        east = east + padding;

        return new BoundingBox(north, east, south, west);
    }// End findBoundingBoxForGivenLocations

    private String formatRouteTime(long millis) {
        if(millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        if(seconds >= 30) minutes = minutes + 1;

        StringBuilder sb = new StringBuilder(64);
        if(days != 0){
            sb.append(days);
            sb.append(" Days ");
        }
        if(hours != 0){
            sb.append(hours);
            sb.append(" Hours ");
        }
        if(minutes != 0){
            sb.append(minutes);
            sb.append(" Minutes ");
        }
        if(days == 0 && hours == 0 && minutes == 0) {
            sb.append(seconds);
            sb.append(" Seconds ");
        }
        return(sb.toString());
    }// End formatRouteTime

    private String formatRouteDistance(double meters){
        double miles = meters * .00062137;
        DecimalFormat df = new DecimalFormat("#.##");
        miles = Double.valueOf(df.format(miles));

        return(miles + " Miles");
    }// End formatRouteDistance

}// End DirectionsActivity