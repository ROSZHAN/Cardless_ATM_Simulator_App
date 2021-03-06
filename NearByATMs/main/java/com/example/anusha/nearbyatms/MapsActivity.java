package com.example.anusha.nearbyatms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import com.google.android.gms.location.LocationListener;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Objects;


import static android.support.design.widget.BottomNavigationView.*;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{
    private static final int REQUEST_LOCATION_CODE = 99;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    double latitude,longitude;
    private Location mLastLocation;
    private Marker mMarker;
    private LocationRequest mLocationRequest;

    int PROXIMITY_RADIUS = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //request runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            CheckLocationPermission();
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    //To check if permissions have been granted
    //Ctrl+o
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_LOCATION_CODE:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Permission is granted
                    if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                    {
                        if(mGoogleApiClient == null)
                            buildGoogleApiClien();
                        mMap.setMyLocationEnabled(true);
                    }
                }
                else //if permission is denied
                {
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Init Google Play Services
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClien();
                mMap.setMyLocationEnabled(true);
            }
    }

    protected synchronized void buildGoogleApiClien()
    {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        //If marker is already set to some location, remove it
        if(mMarker != null)
        {
            mMarker.remove();
        }
        latitude = location.getLatitude();
        Log.d("lat = ",""+latitude);
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        //To set properties of Marker - MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title("Your position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        //Marker now points to the current location - your location
        mMarker = mMap.addMarker(markerOptions);

        //Move Camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomBy(11));

        if(mGoogleApiClient != null)
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    public void onClick(View v) {
        Object dataTransfer[] = new Object[2];
        GetNearByPlacesData getNearByPlacesData = new GetNearByPlacesData();
        switch (v.getId()) {
            case R.id.B_search: {
                EditText tf_location = (EditText) findViewById(R.id.TF_location);
                String location = tf_location.getText().toString();
                List<Address> addressList;

                if (!location.equals("")) {
                    Geocoder geocoder = new Geocoder(this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 5);
                        if(addressList!=null)
                        {
                            for (int i = 0; i < addressList.size(); i++) {
                                LatLng latLng = new LatLng(addressList.get(i).getLatitude() , addressList.get(i).getLongitude());
                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.title(location);
                                mMap.addMarker(markerOptions);
                                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                                mMap.animateCamera(CameraUpdateFactory.zoomTo(10));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;

            case R.id.B_ATM:
                Button atm = (Button) findViewById(R.id.B_ATM);
                atm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMap.clear();
                        String ATMS = "ATM";
                        String url = getUrl(latitude, longitude, ATMS);
                        Object dataTransfer[] = new Object[2];
                        GetNearByPlacesData getNearByPlacesData = new GetNearByPlacesData();
                        dataTransfer[0] = mMap;
                        dataTransfer[1] = url;
                        getNearByPlacesData.execute(dataTransfer);
                        System.out.println(url);
                        Toast.makeText(MapsActivity.this, "Showing near by ATMs", Toast.LENGTH_SHORT).show();
                    }
                });
            break;
        }
    }

    private String getUrl(double latitude, double longitude, String nearByPlace) {
        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location="+latitude+","+longitude);
        googlePlacesUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlacesUrl.append("&type="+nearByPlace);
        googlePlacesUrl.append("&keyword="+"ATM");
        googlePlacesUrl.append("&key="+"AIzaSyBcrKwm1KdEFlAOQcPgYNmn9GQCzCvpPSw");
        Log.d("MapsActivity","url="+googlePlacesUrl.toString());
        return googlePlacesUrl.toString();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this);
        }
    }


    public boolean CheckLocationPermission() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }

            else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_CODE);
            }
            return false;
        }
        else
            return true;
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
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
