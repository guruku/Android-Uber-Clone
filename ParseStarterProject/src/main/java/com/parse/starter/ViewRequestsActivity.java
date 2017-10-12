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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ViewRequestsActivity extends AppCompatActivity {
    ListView requestsListView;
    ArrayList<String> requests = new ArrayList<String>();
    ArrayList<Double> Longitudes = new ArrayList<Double>();
    ArrayList<Double> Latitudes = new ArrayList<Double>();
    ArrayList<String> usernames = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    LocationManager manager;
    LocationListener listener;

    public void updateListView(Location location){
        if(location!=null) {
            requests.clear();

            ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");

            final ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            query.whereNear("location",parseGeoPoint);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e == null){
                        requests.clear();
                        Longitudes.clear();
                        Latitudes.clear();
                        if(objects.size() > 0){

                            for(ParseObject object: objects){
                                ParseGeoPoint requestLocation = (ParseGeoPoint) object.get("location");
                                if(requestLocation != null) {
                                    Double distance = parseGeoPoint.distanceInMilesTo(requestLocation);
                                    Double distanceOneDP = (double) Math.round(distance * 10) / 10;
                                    Log.i(distance.toString() + "...", distanceOneDP.toString());
                                    requests.add(distanceOneDP.toString() + " Miles");
                                    Latitudes.add(requestLocation.getLatitude());
                                    Longitudes.add(requestLocation.getLongitude());
                                    usernames.add(object.getString("username"));
                                }
                            }
                        } else {
                            requests.add("No active Requests nearby..");
                        }
                        arrayAdapter.notifyDataSetChanged();

                    }
                }
            });



        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == 1){
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

                    Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastLocation);
                }

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_requests);

        setTitle("nearby rides...");
        requestsListView = (ListView) findViewById(R.id.requestsListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requestsListView.setAdapter(arrayAdapter);
        requests.clear();
        requests.add("Getting nearby requests...");

        requestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {



            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(Build.VERSION.SDK_INT < 23  ||ContextCompat.checkSelfPermission(ViewRequestsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ){

                    Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(Longitudes.size() > position && Latitudes.size() > position && usernames.size() > position){
                    Intent intent = new Intent(getApplicationContext(), DriverLocationActivity.class);
                    intent.putExtra("requestLatitude",Latitudes.get(position));
                    intent.putExtra("requestLongitude",Longitudes.get(position));
                    intent.putExtra("driverLatitude", lastLocation.getLatitude());
                    intent.putExtra("driverLongitude", lastLocation.getLongitude());
                    intent.putExtra("username", usernames.get(position));

                    startActivity(intent);


                }
                }
            }
        });


        manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);
                ParseUser.getCurrentUser().put("location", new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();

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
        } else if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        } else {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
            try{
                Location lastLocation = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(lastLocation !=null) {
                    updateListView(lastLocation);
                }
            }catch(Exception e){
                Toast.makeText(this, "No Last Known Value", Toast.LENGTH_SHORT).show();
            }
        }
        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }
    }

