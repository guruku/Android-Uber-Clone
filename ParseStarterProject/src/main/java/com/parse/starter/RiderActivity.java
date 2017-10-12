package com.parse.starter;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;

public class RiderActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager manager;
    LocationListener listener;
    Button callUberButton;
    Boolean requestStatus = false;
    Handler handler = new Handler();
    TextView infoTextView;
    Boolean driverActive = false;


    public void checkForUpdate(){
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null && objects.size() > 0){
                    driverActive = true;

                    ParseQuery<ParseUser> query = ParseUser.getQuery();
                    query.whereEqualTo("username", objects.get(0).getString("driverUsername"));
                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if(e == null && objects.size() > 0){
                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("location");
                                if (Build.VERSION.SDK_INT > 23 ||  ContextCompat.checkSelfPermission(RiderActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                   Location lastKnownLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                    if(lastKnownLocation != null){
                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                        Double distance = driverLocation.distanceInMilesTo(userLocation);
                                        if(distance < 0.01){
                                            infoTextView.setText("Your Ride is here!");
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    callUberButton.setVisibility(View.VISIBLE);
                                                    callUberButton.setText("Call an Uber");
                                                    infoTextView.setText("");
                                                    driverActive = false;

                                                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
                                                    query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
                                                    query.whereExists("driverUsername");
                                                    query.findInBackground(new FindCallback<ParseObject>() {
                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            if(e == null){
                                                                for (ParseObject object: objects){
                                                                    object.deleteInBackground();
                                                                }
                                                            }
                                                        }
                                                    });
                                                }
                                            }, 5000);
                                        }else {
                                            Double distanceOneDP = (double) Math.round(distance * 10) / 10;
                                            infoTextView.setText("Your Ride is " + distanceOneDP.toString() + " miles away");

                                            LatLng driverLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
                                            LatLng requestLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                                            ArrayList<Marker> markers = new ArrayList<Marker>();
                                            mMap.clear();

                                            markers.add(mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Driver Location")));
                                            markers.add(mMap.addMarker(new MarkerOptions().position(requestLatLng).title("Your Location")
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));

                                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                            for (Marker marker : markers) {
                                                builder.include(marker.getPosition());
                                            }
                                            LatLngBounds bounds = builder.build();

                                            int padding = 60;
                                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                                            mMap.animateCamera(cu);
                                            callUberButton.setVisibility(View.INVISIBLE);
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    checkForUpdate();
                                                }
                                            }, 2000);

                                        }

                                    }
                                }
                            }
                        }
                    });
                }

            }
        });
    }

    public void logout(View view){
        ParseUser.logOut();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public void callUber(View view) {

        if (requestStatus) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null){
                        if(objects.size() > 0 ){

                            for(ParseObject object: objects){
                                object.deleteInBackground();
                            }
                            requestStatus = false;
                            callUberButton.setText("Call an Uber");


                        }
                    }
                }
            });
        } else {
//        Toast.makeText(RiderActivity.this, "Call Uber", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

                try {
                    Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    ParseObject request = new ParseObject("Request");
                    request.put("username", ParseUser.getCurrentUser().getUsername());
                    ParseGeoPoint location = new ParseGeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                    request.put("location", location);
                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                callUberButton.setText("Cancel Uber");
                                requestStatus = true;


                                        checkForUpdate();

                            }
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(RiderActivity.this, "Could not find location", Toast.LENGTH_SHORT).show();

                }
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

                Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                updateMap(lastLocation);
                }

            }
        }
    }

    public void updateMap(Location location){
        if(!driverActive) {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 6));
            mMap.clear();

            mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        infoTextView = (TextView)findViewById(R.id.infoTextView);
        callUberButton = (Button) findViewById(R.id.callUberButton);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e == null){
                    if(objects.size() > 0 ){
                        requestStatus = true;
                        callUberButton.setText("Cancel Uber");
                        checkForUpdate();
                    }
                }
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
              updateMap(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
if(Build.VERSION.SDK_INT <23) {
    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
} else if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED ) {
    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

} else {
    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
    try{
        Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if(lastLocation !=null) {
        updateMap(lastLocation);
    }
    }catch(Exception e){
        Toast.makeText(RiderActivity.this, "No Last Known Value", Toast.LENGTH_SHORT).show();
    }
}
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
}
