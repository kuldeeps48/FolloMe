package com.theeralabs.follome.view.location;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.theeralabs.follome.model.directionMatrix.user.User;

import static android.content.ContentValues.TAG;

public class PeriodicLocationUpdateService extends Service {
    private User user;
    LocationManager locationManager;
    Context mContext;


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                10, locationListenerGPS);
    }

    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListenerGPS);
        super.onDestroy();
    }

    private void addUserLocationToFirebase(Double lat, Double lng) {
        //Add location to user object
        user.setLat(lat);
        user.setLng(lng);
        //Add user object to database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("users");
        myRef.child(user.getId()).setValue(user);
        Toast.makeText(this, "Your location updated", Toast.LENGTH_SHORT).show();
    }

    LocationListener locationListenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            addUserLocationToFirebase(location.getLatitude(), location.getLongitude());
            Log.d(TAG, "onLocationChanged: Added To firebase");
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


    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        assert intent != null;
        user = intent.getExtras().getParcelable("userObject");

        int ID = 7;
        startForeground(ID, new Notification());

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
