package com.theeralabs.follome.view.map;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theeralabs.follome.R;
import com.theeralabs.follome.model.directionMatrix.user.User;
import com.theeralabs.follome.util.OnSwipeTouchListener;
import com.theeralabs.follome.view.location.PeriodicLocationUpdateService;
import com.theeralabs.follome.view.peopleList.PeopleListFragment;

import java.io.File;
import java.util.ArrayList;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final String KEY_DIRECTION_MATRIX = "AIzaSyCuBz60QHNpCsZNgYdjWK_bdjRz9cW0_Gk";
    private static final int LOCATION_SETTING = 7;
    private ArrayList<LatLng> points;
    private User user;
    FragmentManager manager;
    private static GoogleMap mMap;
    private static Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mContext = this;

        //Get user object from Intent
        Bundle bundle = getIntent().getExtras();
        assert bundle != null;
        user = bundle.getParcelable("userObject");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //Add Right swipe listener
        FrameLayout mainLayout = findViewById(R.id.map_main_layout);
        manager = getSupportFragmentManager();
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                if (manager.getBackStackEntryCount() == 0) {
                    manager.beginTransaction().add(R.id.map, new PeopleListFragment(), "peopleList")
                            .addToBackStack("peopleList").commit();
                }
            }
        });

        //Set User Name Text
        TextView txtHelloUser = findViewById(R.id.txt_hello_name);
        txtHelloUser.setText(new StringBuilder().append("Hello, ").append(user.getName()).toString());

        //Set Logout Button
        Button btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                Toast.makeText(MapActivity.this, "Logged Out. Goodbye", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        //Initialize points array to store LatLng
        points = new ArrayList<>();

        //Start Map
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
        startBackgroundService();
        isLocationEnabled();
    }


    public static void addMarker(final User person) {
        // add marker to Map
        Toast.makeText(mContext, "Adding On Map", Toast.LENGTH_SHORT).show();
        Glide.with(mContext)
                .asFile()
                .load(person.getPhotoUri())
                .into(new SimpleTarget<File>() {
                    @Override
                    public void onResourceReady(File resource, Transition<? super File> transition) {
                        LatLng latLng = new LatLng(person.getLat(), person.getLng());
                        Bitmap bm = BitmapFactory.decodeFile(resource.getPath());
                        //Bitmap scaled = Bitmap.createScaledBitmap(bm,
                        //        (int) (bm.getWidth() * 0.07),
                        //        (int) (bm.getHeight() * 0.07), true);
                        mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromBitmap(bm))
                                .position(latLng)
                                // Specifies the anchor to be at a particular point in the marker image.
                                .anchor(0.5f, 1));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.0f));
                    }
                });

    }

    public void init() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setRotateGesturesEnabled(true);
            mMap.getUiSettings().setTiltGesturesEnabled(true);
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
                            }
                        }
                    });

        }
    }

    private void startBackgroundService() {
        //Start background location update service
        Intent intent = new Intent(this, PeriodicLocationUpdateService.class);
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
                    startActivityForResult(intent, LOCATION_SETTING);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = alertDialog.create();
            alert.show();
        } else {
            init();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOCATION_REQUEST_CODE) {
            isLocationEnabled();
        }
    }

}
