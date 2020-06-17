package com.example.locationtest2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.compat.Place;
import com.google.android.libraries.places.compat.ui.PlaceAutocompleteFragment;
import com.google.android.libraries.places.compat.ui.PlaceSelectionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private GoogleMap mMap;
    private LatLng latLng = new LatLng(-8.579892, 116.095239);
    private MapFragment mapFragment;
    PlaceAutocompleteFragment autocompleteFragment;
    private Button btnCurrLoc,submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //finds google map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);


        setupAutoCompleteFragment();


        //Sets up the get current position button
        btnCurrLoc = (Button) findViewById(R.id.btn);
        btnCurrLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE_LOCATION_PERMISSION);

                } else {
                    getCurrentLocation();
                }
            }
        });

        //Pick position button
        submit = (Button) findViewById(R.id.btnDone);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                try {
                    //Gets the closest adress to the given position. If you pick for example a country it will get the closest street to the given coordinates
                    //Havent found a way to make it better than that
                    String address = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1).get(0).getAddressLine(0).toString();

                    //REPLACE THIS WITH THE SETRESULT AND FINISH IF YOU WANNA USE THIS TO GET MAPS
                    //REMEMBER THAT LONGITUDE AND LATITUDE IS A GOOD IDEA TO BRING WITH YOU FOR PRECISION
                    Toast.makeText(MapsActivity.this,address,Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    //Finds current location
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {

        //Location request
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback(){
                    @Override
                    //When a result for the location request gets here
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MapsActivity.this)
                                .removeLocationUpdates(this);
                        if(locationResult!=null && locationResult.getLocations().size()>0){

                            //Extract info
                            int lastLocationIndex = locationResult.getLocations().size()-1;
                            double latitude = locationResult.getLocations().get(lastLocationIndex).getLatitude();
                            double longitude = locationResult.getLocations().get(lastLocationIndex).getLongitude();

                            //Find address and put it into the search part
                            try {
                                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                                List<Address> adresses = geocoder.getFromLocation(latitude,longitude,1);
                                String address = adresses.get(0).getAddressLine(0);
                                autocompleteFragment.setText(address);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //Sets the maps position to the address'
                            latLng = new LatLng(latitude, longitude);
                            mapFragment.getMapAsync(MapsActivity.this);



                        }
                    }
                }, Looper.getMainLooper());
    }


    //Sets up the autocomplete
    private void setupAutoCompleteFragment() {
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            //When a place is selected the coordinates are set and the map sets its marker
            public void onPlaceSelected(Place place) {
                latLng = place.getLatLng();
                mapFragment.getMapAsync(MapsActivity.this);
            }

            @Override
            public void onError(Status status) {
                Log.e("Error", status.getStatusMessage());
            }
        });
    }

    //Whenever the map has loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Moves the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8.5f));
        //Clears any existing markers
        mMap.clear();
        //Adds a new marker
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            mMap.clear();
        }
    }

    //When permission has been requested for the location this instantly calls get current location if it was granted
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_LOCATION_PERMISSION:
                if (grantResults.length > 0) {
                    if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        getCurrentLocation();
                    }
                }
                break;
        }
    }
}