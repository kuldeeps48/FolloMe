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
import android.location.Address;
import android.location.Geocoder;
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
import android.util.Log;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_REQUEST_CODE = 1;
    private static final String KEY_DIRECTION_MATRIX = "AIzaSyCuBz60QHNpCsZNgYdjWK_bdjRz9cW0_Gk";
    private static final int LOCATION_SETTING = 7;
    private ArrayList<LatLng> points;
    private User user;
    FragmentManager manager;
    private static GoogleMap mMap;
    private static Context mContext;
    private static ArrayList<User> markedUsers;
    private static TextView txtDistance;

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

        //Set Distance text;
        txtDistance = findViewById(R.id.txt_distance);
        txtDistance.setVisibility(View.INVISIBLE);
        //Initialize points array to store LatLng
        points = new ArrayList<>();
        markedUsers = new ArrayList<>();
        markedUsers.add(user);

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
        markedUsers.add(person);
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
                                .title(getCompleteAddressString(person.getLat(), person.getLng()))
                                // Specifies the anchor to be at a particular point in the marker image.
                                .anchor(0.5f, 1));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18.0f));
                        drawPath();
                    }
                });

    }

    public static void drawPath() {
        String origin = markedUsers.get(0).getLat() + "," + markedUsers.get(0).getLng();
        String dest = markedUsers.get(markedUsers.size() - 1).getLat() + "," +
                markedUsers.get(markedUsers.size() - 1).getLng();


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

                    txtDistance.setVisibility(View.VISIBLE);
                    txtDistance.setText(new StringBuilder().append("Distance: ").append(leg.getDistance().getValue()).append(" Meters").toString());

                    List<Step> stepList = leg.getSteps();
                    for (Step step : stepList) {
                        mMap.addPolyline(new PolylineOptions().addAll
                                (PolyUtil.decode(step.getPolyline().getPoints())));
                    }
                } else
                    Toast.makeText(mContext, d.getStatus(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<DirectionMatrix> call, Throwable t) {

            }
        });
    }

    private static String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.w("Current loction ", strReturnedAddress.toString());
            } else {
                Log.w("Current loction ", "No Address returned!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("Current loction ", "Canont get Address!");
        }
        return strAdd;
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
