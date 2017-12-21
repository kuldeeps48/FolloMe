package com.theeralabs.follome.view.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.PolyUtil;
import com.theeralabs.follome.R;
import com.theeralabs.follome.api.ApiInterface;
import com.theeralabs.follome.api.GoogleDirectionApiClient;
import com.theeralabs.follome.model.directionMatrix.direction.DirectionMatrix;
import com.theeralabs.follome.model.directionMatrix.direction.Leg;
import com.theeralabs.follome.model.directionMatrix.direction.Route;
import com.theeralabs.follome.model.directionMatrix.direction.Step;
import com.theeralabs.follome.model.directionMatrix.user.User;
import com.theeralabs.follome.util.OnSwipeTouchListener;
import com.theeralabs.follome.view.location.PeriodicLocationUpdateService;
import com.theeralabs.follome.view.peopleList.PeopleListFragment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final String KEY_DIRECTION_MATRIX = "AIzaSyCuBz60QHNpCsZNgYdjWK_bdjRz9cW0_Gk";
    private ArrayList<LatLng> points;
    private User user;
    FragmentManager manager;
    private static GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Get user object from Intent
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        user = bundle.getParcelable("userObject");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        FrameLayout mainLayout = findViewById(R.id.map_main_layout);
        manager = getSupportFragmentManager();
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                manager.beginTransaction().add(R.id.map, new PeopleListFragment(), "peopleList")
                        .addToBackStack("peopleList").commit();
            }
        });
        points = new ArrayList<>();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {
        if (manager.getBackStackEntryCount() > 0)
            manager.popBackStack();
        else
            super.onBackPressed();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                points.add(latLng);
                //Format for url
                final LatLng o = points.get(points.size() - 1);
                final String origin = o.latitude + "," + o.longitude;
                final LatLng d = points.get(points.size() - 2);
                String dest = d.latitude + "," + d.longitude;

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng)
                        .title("Destination"));
                mMap.addMarker(new MarkerOptions().position(points.get(points.size() - 2))
                        .title("Origin").alpha(0.6f));

                ApiInterface apiInterface1 = GoogleDirectionApiClient.getClient().create(ApiInterface.class);
                Call<DirectionMatrix> call1 = apiInterface1.getDirection(origin, dest, KEY_DIRECTION_MATRIX);
                call1.enqueue(new Callback<DirectionMatrix>() {
                    @Override
                    public void onResponse(Call<DirectionMatrix> call, Response<DirectionMatrix> response) {
                        DirectionMatrix d = response.body();

                        if (d.getStatus().equalsIgnoreCase("OK")) {
                            List<Route> routes = d.getRoutes();
                            Route route = routes.get(0);
                            List<Leg> legList = route.getLegs();
                            Leg leg = legList.get(0);

                            /*txtDistance.setVisibility(View.VISIBLE);
                            txtDuration.setVisibility(View.VISIBLE);
                            txtDistance.setText("Distance: " + leg.getDistance().getValue() + " Meters");
                            txtDuration.setText("Duration: " + (leg.getDuration().getValue()) / 60 + " Minutes");
*/
                            List<Step> stepList = leg.getSteps();
                            for (Step step : stepList) {
                                mMap.addPolyline(new PolylineOptions().addAll
                                        (PolyUtil.decode(step.getPolyline().getPoints())));
                            }
                        } else
                            Toast.makeText(getApplicationContext(), d.getStatus(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<DirectionMatrix> call, Throwable t) {

                    }
                });
            }
        });

        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);

        checkPermission();
        init();
    }

    public static void addMarker(User listPerson) {
        LatLng ln = new LatLng(listPerson.getLat(), listPerson.getLng());
        mMap.addMarker(new MarkerOptions().position(ln)
                .title(listPerson.getName()));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ln, 18.0f));
        Log.d("MAP ACTIVITY", "addMarker: Marker Added at " + ln.toString());
    }

    public void init() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
            mFusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.addMarker(new MarkerOptions().position(currentLocation)
                                        .title("Your Current Location"));
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18.0f));
                                points.add(currentLocation);
                                addUserLocationToFirebase(currentLocation);

                                startBackgroundService();
                            }
                        }
                    });

        }
    }

    private void startBackgroundService() {
        //Start background location update service
        Intent intent = new Intent(MapActivity.this, PeriodicLocationUpdateService.class);
        intent.putExtra("userObject", user);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else
            startService(intent);
    }

    private void addUserLocationToFirebase(LatLng currentLocation) {
        //Add location to user object
        user.setLat(currentLocation.latitude);
        user.setLng(currentLocation.longitude);
        //Add user object to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");
        myRef.child(user.getId()).setValue(user);
        Toast.makeText(this, "Your location updated", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLocationEnabled();
        init();
    }

    LocationManager locationManager;

    private void isLocationEnabled() {
        Context mContext = this;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
            alertDialog.setTitle("Enable Location");
            alertDialog.setMessage("Your locations setting is not enabled. Please enabled it in settings menu.");
            alertDialog.setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        } else
            startBackgroundService();
    }

    //Location Permission///////////////////////////////////////////////////////////////////////////
    public void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            Toast.makeText(getApplicationContext(), "Location Permission Required", Toast.LENGTH_SHORT).show();
        } else {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            checkPermission();
        }
    }
}
