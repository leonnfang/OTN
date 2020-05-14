package com.example.otn;

import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;
import android.location.*;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;

import com.example.otn.ui.ar.LocationActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;


import java.util.*;
import java.io.IOException;
import java.lang.String;
import java.util.ArrayList;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;

    private static final String TAG = "MapActivity";
    private Boolean mLocationPermissionsGranted = false;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PLACE_PICKER_REQUEST = 1;
    private LatLng currentLocation;
    private LatLng destination;
    private String apiKey;
    private ArrayList<LatLng> start_location;
    private ArrayList<LatLng> end_location;
    protected GoogleApiClient mGoogleApiClient;
    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark,R.color.colorPrimary,R.color.common_google_signin_btn_text_light_pressed,R.color.colorAccent,R.color.primary_dark_material_light};

    // private EditText mSearchText;

    PlacesClient placesClient; // instance of a google api places client

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        apiKey = getString(R.string.google_maps_key);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        mapFragment.getMapAsync(this);
        getLocationPermission();


        // Di: auto-complete-suggestion set-up
        // Google Places API AutoCompleteFragment
        placesClient = Places.createClient(this);

        final AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.input_search);

        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME));

        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng LatLng = place.getLatLng();
                // Toast.makeText(MapsActivity.this, ""+LatLng.latitude, Toast.LENGTH_SHORT).show();
                Log.d("PlacesApi", "onPlaceSelected: "+LatLng.latitude+"\n"+LatLng.longitude);
                geoLocate(place.getName());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.d(TAG, "An error occurred: " + status);
            }
        });

        // Navigation Button
        Button btn = findViewById(R.id.nav_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLocation != null && destination != null)
                {
                    Log.d(TAG, "Start Navigation");
                    Toast.makeText(getApplicationContext(), "Starting Navigation", Toast.LENGTH_SHORT)
                            .show();
                    startNavigation();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please enter destination", Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        // AR Button
        Button arbtn = findViewById(R.id.ar_button);
        arbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MapsActivity.this, LocationActivity.class);
                intent.putParcelableArrayListExtra("key",  end_location);
                startActivity(intent);
            }
        });
    }

    private void init() {
        Log.d(TAG, "init: initializing");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
            }
        });
        hideSoftKeyboard();
        currentLocation = new LatLng(40.72944,-73.99111); //default to Cooper address
    }

    private void initMap(){
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapsActivity.this);
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
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            LatLng bikeExample = new LatLng(40.7487128, -73.9859724);
            moveCamera(bikeExample, DEFAULT_ZOOM, "Empire State Building");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

            init();
        }
    }

    /**
     * Get User permission to access their location
     */
    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;
        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    initMap();
                }
            }
        }
    }


    /**
     * Get User's initial location when the app is open.
     */
    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location location = (Location) task.getResult();
                            moveCamera(new LatLng(location.getLatitude(), location.getLongitude()),
                                    DEFAULT_ZOOM, "My Location");
                            currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
    }

    /**
     * Private function to move the camera to desired location with LatLng information
     */
    private void moveCamera(LatLng latLng, float zoom, String place) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if (place.equals("Empire State Building"))
        {
            BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.mipmap.bike_icon);
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .icon(icon);
            mMap.addMarker(options);
        }
        if(!place.equals("My Location") && !place.equals("Empire State Building")){
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(place);
            mMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    private void geoLocate(String place_name){
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = place_name;

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list = geocoder.getFromLocationName(searchString, 1);
        }catch (IOException e){
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage() );
        }

        if(list.size() > 0){
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            destination = new LatLng(address.getLatitude(), address.getLongitude());
            moveCamera(destination, DEFAULT_ZOOM, address.getAddressLine(0));
        }

    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void startNavigation() {
        getDeviceLocation();
        Routing route = new Routing();
        end_location = route.getRoute(this.currentLocation, this.destination, apiKey);
    }
}