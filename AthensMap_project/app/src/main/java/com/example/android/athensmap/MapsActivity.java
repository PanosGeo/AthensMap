package com.example.android.athensmap;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.View;

import com.example.android.athensmap.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.src;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap athensMap; //map instance of Athens
    @Nullable
    private Location myLocation; //current location of user
    private GoogleApiClient googleApiClient; //entry point to Google Play Services used for location provider
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private double destLatitude = 0.0;
    private double destLongitude = 0.0;
    private static final int PERMISSIONS_REQUEST_ACCESS_GPS = 1;
    private boolean accessLocationGranted;
    View label;
    Marker destMarker;
    ArrayList<Polyline> polylines = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        label = findViewById(R.id.lbl);
        //builder for connecting to the google API client in order to connect explicitly with google maps API
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            googleApiClient.connect();
        }
    }

    //METHODS OF MapsActivity.java

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        athensMap = map;
        athensMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        updateCurrentLocation();
        getCurrentLocation();
    }

    //UI change accordingly to user's current known position
    private void updateCurrentLocation() {
        if (athensMap == null) {
            return;
        }
        //checks if user has given permission for GPS else asks for it
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            accessLocationGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_GPS);
        }

        if (accessLocationGranted) {
            athensMap.setMyLocationEnabled(true); //enables GPS button top right of map
            athensMap.getUiSettings().setMyLocationButtonEnabled(true); //Enables GPS feature
        } else {
            athensMap.setMyLocationEnabled(false);
            athensMap.getUiSettings().setMyLocationButtonEnabled(false); //GPS not enabled
            myLocation = null; //if permission denied app shows nothing and crashes (for now)
        }
    }


    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            accessLocationGranted = true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_GPS);
        }

        //check if location access is granted
        if (accessLocationGranted) {
            myLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient); //gets user's last known location usually the current one
            if (myLocation != null) {
                currentLatitude = myLocation.getLatitude();
                currentLongitude = myLocation.getLongitude(); //sets the map's camera to user's current position
                //Log.d(TAG, "Value: " + Double.toString(currentLatitude));
                //Log.d(TAG, "Value: " + Double.toString(currentLongitude));
                LatLng orig = new LatLng(currentLatitude, currentLongitude);
                athensMap.addMarker(new MarkerOptions().position(orig)
                        .title("Origin")
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                athensMap.animateCamera(CameraUpdateFactory.newLatLngZoom(orig, 15.0f), 1500, null);
                label.bringToFront();
                label.animate().translationY(230);
            } else {
                Log.d(TAG, "Current location is null. Using defaults.");
                currentLatitude = 37.9838;
                currentLongitude =  23.7275;
                athensMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLatitude, currentLongitude), 15.0f), 1500, null);//omonoia, default location if gps is off
                athensMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            athensMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                @Override
                public void onMapClick(@NonNull LatLng point) {
                    if (destMarker != null) {
                        destMarker.remove();
                    }
                    destMarker = athensMap.addMarker(new MarkerOptions().position(point).title("Destination").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    destLongitude = point.longitude;
                    destLatitude = point.latitude;
                    //animation code
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    if (label.getX() > 0) {
                        label.animate().translationX(-width);
                    }
                    //end of animation code
                    clearMap(false);
                    DummyClient client = new DummyClient();
                    client.execute(currentLatitude + "," + currentLongitude, destLatitude + "," + destLongitude);
                }
            });
            athensMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker arg0) {
                    // TODO Auto-generated method stub
                    Log.d("System out", "onMarkerDragStart..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);
                }

                @SuppressWarnings("unchecked")
                @Override
                public void onMarkerDragEnd(Marker arg0) {
                    // TODO Auto-generated method stub
                    Log.d("System out", "onMarkerDragEnd..." + arg0.getPosition().latitude + "..." + arg0.getPosition().longitude);
                    clearMap(true);
                    currentLatitude = arg0.getPosition().latitude;
                    currentLongitude = arg0.getPosition().longitude;
                    athensMap.animateCamera(CameraUpdateFactory.newLatLng(arg0.getPosition()));
                }

                @Override
                public void onMarkerDrag(Marker arg0) {
                    // TODO Auto-generated method stub
                    Log.i("System out", "onMarkerDrag...");

                }
            });
        }
    }

    public void clearMap(Boolean marker){
        if(!polylines.isEmpty()){
            for(Polyline pol:polylines){
                pol.remove();
            }
            polylines.clear();
        }
        if(marker){
            if(destMarker!=null){
                destMarker.remove();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // Builds the map that is showed on screen
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        accessLocationGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_GPS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    accessLocationGranted = true;
                }
            }
        }
        updateCurrentLocation();
    }

    public void drawDirections(List<LatLng> list){
//        for (int z = 0; z < list.size() - 1; z++) {
//            LatLng src = list.get(z);
//            LatLng dest = list.get(z + 1);
            polylines.add(
                    athensMap.addPolyline(new PolylineOptions()
                            .addAll(list)
                                    //new LatLng(src.latitude, src.longitude),
                                  //  new LatLng(dest.latitude, dest.longitude))
                            .width(5).color(Color.BLUE).geodesic(true)));
       // }
    }

    private class DummyClient extends AsyncTask<String, Void, String> {

        String query;

        @Override
        protected String doInBackground(String... pair) {
            createQuery(pair);
            Socket requestSocket = null;
            ObjectOutputStream out = null;
            ObjectInputStream input = null;
            ergasia1.Pair result = null;
            try {

                //requestSocket = new Socket(InetAddress.getByName("10.0.2.2"), 1100);
                requestSocket = new Socket(InetAddress.getByName("192.168.1.3"), 1100);
                out = new ObjectOutputStream(requestSocket.getOutputStream());
                input = new ObjectInputStream(requestSocket.getInputStream());


                out.writeObject(query);
                out.flush();

                result = (ergasia1.Pair) input.readObject();

            } catch (UnknownHostException unknownHost) {
                System.err.println("You are trying to connect to an unknown host!");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    input.close();
                    out.close();
                    requestSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            if (result != null) {
                return result.getRight();
            } else {
                return "There was an error with the result";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
            drawDirections(PolyUtil.decode(result));
        }


        public void createQuery(String... pair) {
            if (pair[0].length() != 5) {
                this.query = "(" + "(" + pair[0] + ")" + "," + "(" + pair[1] + ")" + ")";

            } else {
                this.query = "(" + pair[0] + "," + pair[1] + ")";

            }
        }


    }

}
