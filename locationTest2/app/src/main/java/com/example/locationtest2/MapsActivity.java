package com.example.locationtest2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, GoogleMap.OnMapClickListener {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 952;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 951;
    private GoogleMap mMap;
    private Geocoder mGeo;
    private LatLng latLng = new LatLng(-8.579892, 116.095239);
    private MapFragment mapFragment;
    PlaceAutocompleteFragment autocompleteFragment;
    private Button btnCurrLoc,submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mGeo = new Geocoder(MapsActivity.this, Locale.getDefault());

        //finds google map
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        setupAutoCompleteFragment();

        getCurrentLocation();

        //Sets up the get current position button
        btnCurrLoc = (Button) findViewById(R.id.btn);
        btnCurrLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCurrentLocation();
            }
        });

        //Pick position button
        submit = (Button) findViewById(R.id.btnDone);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //Gets the closest adress to the given position. If you pick for example a country it will get the closest street to the given coordinates
                    //Havent found a way to make it better than that
                    final String address = mGeo.getFromLocation(latLng.latitude,latLng.longitude,1).get(0).getAddressLine(0).toString();

                    //
                    LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                    View popupView = inflater.inflate(R.layout.place_picker_popup, null);

                    int width = LinearLayout.LayoutParams.WRAP_CONTENT;
                    int height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    boolean focusable = true; // lets taps outside the popup also dismiss it
                    final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

                    ((TextView)popupView.findViewById(R.id.txtPopupAddress)).setText(address);

                    Button popupCancel = (Button) popupView.findViewById(R.id.btnPopupCancel);
                    popupCancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            popupWindow.dismiss();
                        }
                    });

                    Button popupChoose = (Button) popupView.findViewById(R.id.btnPopupChoose);
                    popupChoose.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("address_key",address);
                            resultIntent.putExtra("latitude_key",latLng.latitude);
                            resultIntent.putExtra("longitude_key",latLng.longitude);
                            setResult(Activity.RESULT_OK,resultIntent);
                            finish();
                        }
                    });





                    popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    //Finds current location
    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapsActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE_LOCATION_PERMISSION);

        } else {
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
                                    List<Address> adresses = mGeo.getFromLocation(latitude,longitude,1);
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

        autocompleteFragment.setText("hi");
    }

    //Whenever the map has loaded
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //Moves the camera
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        //Sets the on click listener
        mMap.setOnMapClickListener(this);
        //Clears any existing markers
        mMap.clear();
        //Adds a new marker
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        //Sets the text
        double latitude = latLng.latitude;
        double longitude = latLng.longitude;

        try {
            List<Address> adresses = null;
            adresses = mGeo.getFromLocation(latitude,longitude,1);
            String address = adresses.get(0).getAddressLine(0);
            autocompleteFragment.setText(address);
        } catch (IOException e) {
            e.printStackTrace();
        }


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

    @Override
    public void onMapClick(LatLng clickedLatLng) {
        if (Build.VERSION.SDK_INT >= 29) {
            //We need background permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                latLng = clickedLatLng;
                mapFragment.getMapAsync(MapsActivity.this);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    //We show a dialog and ask for permission
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_ACCESS_REQUEST_CODE);
                }
            }

        } else {
            latLng = clickedLatLng;
            mapFragment.getMapAsync(MapsActivity.this);
        }

    }
}